package Zeze.Component;

import Zeze.Application;
import Zeze.Builtin.SafeBatch.BAppInstanceId;
import Zeze.Builtin.SafeBatch.BBatch;
import Zeze.Builtin.SafeBatch.BBatchReadOnly;
import Zeze.Hot.HotHandle;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableReadOnly;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.TableX;
import Zeze.Transaction.Transaction;
import Zeze.Util.Action0;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class SafeBatch extends AbstractSafeBatch {
	private final @NotNull Application zeze;
	private final ConcurrentHashMap<String, Future<?>> running = new ConcurrentHashMap<>();
	private final HotHandle<WalkJobHandle> hotWalkJobHandles = new HotHandle<>();
	private volatile boolean stopped = false;

	public interface WalkJobHandle {
		// 每个记录回调一次。
		// 在存储过程中回调。
		// 返回0表示成功，如果返回非0，中断批处理。
		long runJob(SafeBatch safeBatch, Object key, Object value) throws Exception;
		// 遍历内存中的SortedMap时需要实现。
		// 返回的NavigableMap的值必须>mapKey。也就是使用tailMap(mapKey, false)得到它。
		default NavigableMap<?, ?> tailMapExclusiveOutofTransaction(@Nullable TableX<?, ?> table,
		                                                            @Nullable ByteBuffer tableKey,
		                                                            @Nullable Comparable<?> mapKey) throws Exception {
			return null;
		}
	}
	/**
	 * 开始表格遍历批处理。
	 *
	 * @param table table
	 * @param jobHandle jobHandle
	 * @return jobId
	 */
	public String startWalkTable(TableX<?, ?> table, WalkJobHandle jobHandle) {
		return startWalkTable(table, jobHandle, 60_000, 100);
	}

	/**
	 * 开始表格遍历批处理。对每一个记录通过jobHandle回调进行处理。每个记录的处理在独立的存储过程中。遍历时分批处理：
	 * @param table table
	 * @param jobHandle jobHandle
	 * @param checkPeriod 任务运行监控Timer的间隔
	 * @param limit 每批遍历的Job数量，必须大于0.
	 * @return jobId
	 */
	public String startWalkTable(TableX<?, ?> table, WalkJobHandle jobHandle, long checkPeriod, int limit) {
		if (stopped)
			throw new IllegalStateException("stopped");
		if (limit <= 0)
			throw new IllegalStateException("limit is 0");
		return _saveAndStart(table, null, jobHandle, checkPeriod, limit);
	}

	// 返回内部管理表格，用于查询当前的walk任务。
	public TableReadOnly<String, BBatch, BBatchReadOnly> getTable() {
		return _tSafeBatch;
	}

	public void start() throws Exception {
		zeze.newProcedure(() -> {
			startWalkTable(_tSafeBatch, new TableLoadHandle(), 100, 100);
			return 0;
		}, "startSafeBatch").call();
	}

	public void stop() throws Exception {
		stopped = true;
		for (var run : running.values()) {
			run.cancel(false);
		}
		for (var run : running.values()) {
			run.get();
		}
		running.clear();
	}

	public static class TableBatchTimerHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			var custom = (BAppInstanceId)context.customData;
			if (null != custom) {
				var zeze = Application.getAppInstance(custom.getAppInstanceId());
				if (null != zeze) {
					zeze.getSafeBatch().checkBatch(context.timerId,
						zeze.getSafeBatch()._tSafeBatch.get(context.timerId));
				}
			}
		}
	}

	public static class TableLoadHandle implements WalkJobHandle {
		@Override
		public long runJob(SafeBatch safeBatch, Object _key, Object _value) throws Exception {
			var key = (String)_key;
			var value = (BBatch)_value;
			safeBatch.checkBatch(key, value);
			return 0;
		}
	}

	private void _startWorker(@NotNull String timerId, @NotNull WalkJobHandle jobHandle,
							  @NotNull BBatch batch) {
		if (!stopped) {
			running.computeIfAbsent(timerId, (key) -> {
				var worker = (batch.getRecordKey().size() == 0)
					? new TableBatchWorker(timerId, jobHandle, batch.toData())
					: new SortedMapWorker(timerId, jobHandle, batch.toData());
				var future = Task.runUnsafe(worker, "BatchWorker_" + timerId);
				worker.setFuture(future);
				return future;
			});
		}
	}

	void checkBatch(String timerId, BBatch batch) throws Exception {
		if (stopped)
			return; // stopping skip.

		if (null == batch) {
			_stopTableBatch(timerId);
			return;
		}

		var existFuture = running.get(timerId);
		if (null != existFuture && !existFuture.isCancelled() && !existFuture.isDone()) {
			return; // already running
		}
		// reset
		if (null != existFuture) {
			running.remove(timerId, existFuture);
		}
		var jobHandle = hotWalkJobHandles.findHandle(zeze, batch.getJobClass());
		Transaction.whileCommit(() -> _startWorker(timerId, jobHandle, batch));
	}

	private long _stopTableBatch(String jobId) {
		zeze.getTimer().cancel(jobId);
		_tSafeBatch.remove(jobId);
		Transaction.whileCommit(() -> {
			// 这里不用 CAS 删除：本 jobId 的 future 此时一定是当前提交 stop 时那个，
			// 不会被其他路径覆盖；重启总是分配新的 timerId，不会复用本 jobId。
			var job = running.remove(jobId);
			if (null != job) {
				job.cancel(false);
				// 为了不阻塞事务，这里不等待完成。有点风险：会丢失执行结果。
				// 因为worker.run 可能自己stop自己，等待有死锁风险。
//				try {
//					job.get();
//				} catch (Exception e) {
//					throw new RuntimeException(e);
//				}
			}
		});
		return 0;
	}

	public void stopTableBatch(String jobId) {
		if (Transaction.getCurrent() != null) {
			_stopTableBatch(jobId);
			return;
		}
		zeze.newProcedure(() -> _stopTableBatch(jobId), "stopBatch_" + jobId).call();
	}

	private abstract class Worker implements Action0, TableWalkHandle<Object, Object> {
		protected final String timerId;
		protected final WalkJobHandle jobHandle;
		protected final TableX<?, ?> table;
		protected final int limit;

		protected volatile Future<?> futureSelf;

		public void setFuture(Future<?> future) {
			this.futureSelf = future;
		}

		@Override
		public boolean handle(@NotNull Object key, @NotNull Object value) throws Exception {
			// 当前线程直接调用。call内部基础处理了所有异常。如果还有异常抛出，中断这次批处理，等待timer重启。
			// 当前记录处理失败，中断批执行，下一次再次尝试。
			return 0 == zeze.newProcedure(() -> {
				var result = jobHandle.runJob(SafeBatch.this, key, value);
				if (0 == result && !timerId.isEmpty()) {
					// 每次记录处理都保存一次lastKey。
					var batch = _tSafeBatch.get(timerId);
					if (null != batch)
						batch.setLastKey(new Binary(table.encodeKey(key)));
				}
				return result;
			}, "SafeBatchJob_" + timerId).call();
		}

		public Worker(TableX<?, ?> table, int limit, String timerId, WalkJobHandle jobHandle) {
			this.timerId = timerId;
			this.jobHandle = jobHandle;
			this.table = table;
			this.limit = limit;
		}
	}

	private class TableBatchWorker extends Worker {
		private final BBatch.Data batch;
		private Comparable<?> lastKey;

		public TableBatchWorker(String timerId, WalkJobHandle jobHandle, BBatch.Data batch) {
			super((TableX<?, ?>)zeze.getTable(batch.getTableName()), batch.getProposeLimit(), timerId, jobHandle);
			this.batch = batch;
			var lastKeyBin = batch.getLastKey();
			lastKey = lastKeyBin.size() != 0 ? table.decodeKey(ByteBuffer.Wrap(lastKeyBin)) : null;
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		public void run() throws Exception {
			while (null == futureSelf) {
				// busy wait, see _startWorker...setFuture
				Thread.onSpinWait();
			}
			// isDone 不必要。
			while (!futureSelf.isCancelled() && !futureSelf.isDone()) {
				assert table != null;
				lastKey = ((TableX)table).walkDatabase(lastKey, batch.getProposeLimit(), this);
				if (null == lastKey) {
					stopTableBatch(timerId);
					return;
				}
			}
		}
	}
	/**
	 * 开始SortedMap遍历批处理。
	 *
	 * @param table table
	 * @param key 记录的key
	 * @param jobHandle jobHandle
	 * @return jobId
	 */
	public String startWalkSortedMap(@Nullable TableX<?, ?> table, @Nullable Comparable<?> key,
	                                 @NotNull WalkJobHandle jobHandle) throws Exception {
		return startWalkSortedMap(table, key, jobHandle, 60_000, 100);
	}

	/**
	 * 开始SortedMap遍历批处理。对每一个记录通过jobHandle回调进行处理。每个记录的处理在独立的存储过程中。遍历时分批处理：
	 * walk的sortedmap来源分为两种：
	 * 1. table.record 里面的某个字段，此时参数table,key指向这个记录。
	 * 2. 内存中的任意NavigableMap。此时table,key为null。
	 * 实现须知：WalkJobHandle.tailMapExclusiveOutofTransaction 是在事务外调用的。所以返回的NavigableMap需要支持并发读取。
	 * 如果NavigableMap是记录中的字段，此时使用selectDirty得到记录并返回map。如果NavigableMap是自己的内存数据，
	 * 需要支持多线程读取安全。建议使用org.pcollections.PSortedMap。
	 * @param table table
	 * @param key 记录的key
	 * @param jobHandle jobHandle
	 * @param checkPeriod 任务运行监控Timer间隔
	 * @param limit 每次创建的Job
	 * @return jobId
	 */
	public String startWalkSortedMap(@Nullable TableX<?, ?> table, @Nullable Comparable<?> key,
									 @NotNull WalkJobHandle jobHandle, long checkPeriod, int limit) throws Exception {
		if (stopped)
			throw new IllegalStateException("stopped");
		if (limit <= 0)
			throw new IllegalStateException("limit is 0");
		if (null == table || null == key) {
			// 遍历内存中的map，另起一个线程管理jobs任务。
			Task.run(() -> {
				var tail = jobHandle.tailMapExclusiveOutofTransaction(null, null, null);
				new SortedMapWorker(jobHandle, tail.size()).runJobs(tail);
			}, "SortedMapWorker");
			// 遍历内存中的sortedmap，无法持久化，因为重启内存中的数据可能不再与原来的含义相同。
			// 所以这种情况下，并不会创建持久化timer，和保存lastKey。
			// 这里随便返回一个空串，避免外面null失败。
			return "";
		}
		return _saveAndStart(table, key, jobHandle, checkPeriod, limit);
	}

	private String _saveAndStart(@NotNull TableX<?, ?> table, @Nullable Comparable<?> key,
								 @NotNull WalkJobHandle jobHandle, long checkPeriod, int limit) {
		var timerId = zeze.getTimer().schedule(checkPeriod, checkPeriod,
			TableBatchTimerHandle.class, new BAppInstanceId(zeze.getProjectName()));
		var batch = _tSafeBatch.getOrAdd(timerId);
		batch.setTableName(table.getName());
		if (null != key)
			batch.setRecordKey(new Binary(table.encodeKey(key)));
		batch.setProposeLimit(limit);
		batch.setJobClass(jobHandle.getClass().getName());
		Transaction.whileCommit(() -> _startWorker(timerId, jobHandle, batch));
		return timerId;
	}

	public class SortedMapWorker extends Worker {
		private final BBatch.Data batch;
		private Comparable<?> lastKey;

		public SortedMapWorker(String timerId, WalkJobHandle jobHandle, BBatch.Data batch) {
			super((TableX<?, ?>)zeze.getTable(batch.getTableName()), batch.getProposeLimit(), timerId, jobHandle);
			var lastKeyBin = batch.getLastKey();
			lastKey = lastKeyBin.size() != 0 ? table.decodeKey(ByteBuffer.Wrap(lastKeyBin)) : null;
			this.batch = batch;
		}

		public SortedMapWorker(WalkJobHandle jobHandle, int limit) {
			super(null, limit, "", jobHandle);
			this.batch = null;
		}

		@Override
		public void run() throws Exception {
			while (null == futureSelf) {
				// busy wait, see _startWorker...setFuture
				Thread.onSpinWait();
			}
			while (!futureSelf.isCancelled() && !futureSelf.isDone()) {
				var tail = jobHandle.tailMapExclusiveOutofTransaction(table, ByteBuffer.Wrap(batch.getRecordKey()), lastKey);
				lastKey = runJobs(tail);
				if (null == lastKey) {
					stopTableBatch(timerId);
					return;
				}
			}
		}

		public Comparable<?> runJobs(@NotNull NavigableMap<?, ?> tail) throws Exception {
			var it = tail.entrySet().iterator();
			var _limit = limit;
			Object key = null;
			for (; _limit > 0 && it.hasNext(); --_limit) {
				var e = it.next();
				handle(e.getKey(), e.getValue());
				key = e.getKey();
			}
			return it.hasNext() ? (Comparable<?>)key : null;
		}
	}

	public @NotNull Application getZeze() {
		return zeze;
	}

	public SafeBatch(@NotNull Application zeze) {
		this.zeze = zeze;
		RegisterZezeTables(zeze);
	}

	@Override
	public void UnRegister() {
		UnRegisterZezeTables(zeze);
	}
}
