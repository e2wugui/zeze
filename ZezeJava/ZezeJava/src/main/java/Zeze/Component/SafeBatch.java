package Zeze.Component;

import Zeze.Application;
import Zeze.Builtin.SafeBatch.BAppInstanceId;
import Zeze.Builtin.SafeBatch.BBatch;
import Zeze.Hot.HotHandle;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.TableX;
import Zeze.Transaction.Transaction;
import Zeze.Util.Action0;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class SafeBatch extends AbstractSafeBatch {
	private final @NotNull Application zeze;
	private final ConcurrentHashMap<String, Future<?>> running = new ConcurrentHashMap<>();
	private final HotHandle<WalkJobHandle> hotWalkJobHandles = new HotHandle<>();
	private volatile boolean stopped = false;

	public interface WalkJobHandle {
		// 每个记录或Entry回调一次。
		long runJob(SafeBatch safeBatch, Object key, Object value) throws Exception;

		// 使用OneByOneKey时需要实现。
		default Object decodeOneByOneKey(ByteBuffer buffer) {
			return null;
		}
		default void encodeOneByOneKey(ByteBuffer buffer, Object key) {
		}
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
	 * @param oneByOneKey oneByOneKey，null表示其他执行模式
	 * @return jobId
	 */
	public String startWalkTable(TableX<?, ?> table, WalkJobHandle jobHandle, Object oneByOneKey) {
		return startWalkTable(table, jobHandle, oneByOneKey, 60_000, 100);
	}

	/**
	 * 开始表格遍历批处理。对每一个记录通过jobHandle回调进行处理。每个记录的处理在独立的存储过程中。遍历时分批处理。支持多种执行模式：
	 * 1. one by one 需要提供oneByOneKey，一般是roleId或者account。
	 * 2. 顺序在walk线程中执行事务。
	 * 3. 并发执行jobHandle。
	 * @param table table
	 * @param jobHandle jobHandle
	 * @param oneByOneKey oneByOneKey，null表示其他执行模式，见下面的limit。
	 * @param checkPeriod 任务运行监控Timer的间隔
	 * @param limit 每批遍历的Job数量，负数表示并发执行（同时oneByOneKey要为null）。
	 * @return jobId
	 */
	public String startWalkTable(TableX<?, ?> table, WalkJobHandle jobHandle,
	                             Object oneByOneKey, long checkPeriod, int limit) {
		if (stopped)
			throw new IllegalStateException("stopped");
		if (limit == 0)
			throw new IllegalStateException("limit is 0");
		return _saveAndStart(table, null, jobHandle, oneByOneKey, checkPeriod, limit);
	}

	public void start() throws Exception {
		startWalkTable(_tSafeBatch, new TableLoadHandle(), null, 100, 100);
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
					zeze.getSafeBatch().checkTableBatch(context.timerId,
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
			safeBatch.checkTableBatch(key, value);
			return 0;
		}
	}

	private void _startWorker(@NotNull String timerId, @NotNull WalkJobHandle jobHandle,
							  @Nullable Object oneByOneKey, @NotNull BBatch batch) {
		if (!stopped) {
			running.computeIfAbsent(timerId, (key) -> {
				var worker = (batch.getRecordKey().size() == 0)
					? new TableBatchWorker(timerId, jobHandle, oneByOneKey, batch.toData())
					: new SortedMapWorker(timerId, jobHandle, oneByOneKey, batch.toData());
				var future = Task.runUnsafe(worker, "BatchWorker_" + timerId);
				worker.setFuture(future);
				return future;
			});
		}
	}

	void checkTableBatch(String timerId, BBatch batch) throws Exception {
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
		var oneByOneKey = jobHandle.decodeOneByOneKey(ByteBuffer.Wrap(batch.getOneByOneKey()));
		Transaction.whileCommit(() -> _startWorker(timerId, jobHandle, oneByOneKey, batch));
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
				// 另外 worker.run 可能自己stop自己，等待有死锁风险。
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
		protected final Object oneByOneKey;
		protected final TableX<?, ?> table;
		protected final int limit;

		protected volatile Future<?> futureSelf;
		protected final ArrayList<Future<?>> futures = new ArrayList<>();

		public void setFuture(Future<?> future) {
			this.futureSelf = future;
		}

		@Override
		public long endWalk(long count) throws Exception {
			if (null != oneByOneKey) {
				// one by one 模式，加入一个任务并等待完成。
				var future = new TaskCompletionSource<Boolean>();
				zeze.getTaskOneByOneByKey().Execute(oneByOneKey, () -> future.setResult(true));
				future.await();
				return count;
			}

			if (limit < 0){
				// 并发执行模式，全部等待。
				// 这里如果某个 future.get() 抛异常（job 失败），故意不吞掉：
				// 让异常冒泡到 run()，worker 异常退出，等下次 checkTableBatch
				// 检测到 dead future 后重启整批。
				for (var future : futures)
					future.get();
				futures.clear();
				return count;
			}

			// else 立即call模式，不需要额外等待。
			return count;
		}

		@Override
		public boolean handle(@NotNull Object key, @NotNull Object value) throws Exception {
			var proc = zeze.newProcedure(() -> jobHandle.runJob(SafeBatch.this, key, value),
				"SafeBatchJob_" + timerId);

			if (null != oneByOneKey) {
				// one by one 执行。
				zeze.getTaskOneByOneByKey().Execute(oneByOneKey, proc);
				return true;
			}

			if (limit < 0) {
				// 并发执行。
				futures.add(Task.runUnsafe(proc));
				return true;
			}

			// 当前线程直接调用。
			try {
				proc.call();
			} catch (Exception ignored) {
				// call 里面已有足够日志，这里不需要记了。
			}
			return true;
		}

		public void saveLastKey(Comparable<?> lastKey) {
			// 一批执行完保存一次lastKey。
			// 这种方式没有每事务保存严谨。
			// 够用了。
			zeze.newProcedure(() -> {
				var batch = _tSafeBatch.get(timerId);
				if (null != batch)
					batch.setLastKey(new Binary(table.encodeKey(lastKey)));
				return 0;
			}, "SafeBatch_SaveLastKey").call();
		}

		public Worker(TableX<?, ?> table, int limit, String timerId, WalkJobHandle jobHandle, Object oneByOneKey) {
			this.timerId = timerId;
			this.jobHandle = jobHandle;
			this.oneByOneKey = oneByOneKey;
			this.table = table;
			this.limit = limit;
		}
	}

	private class TableBatchWorker extends Worker {
		private final BBatch.Data batch;
		private Comparable<?> lastKey;

		public TableBatchWorker(String timerId, WalkJobHandle jobHandle, Object oneByOneKey, BBatch.Data batch) {
			super((TableX<?, ?>)zeze.getTable(batch.getTableName()), batch.getProposeLimit(), timerId, jobHandle, oneByOneKey);
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
				lastKey = ((TableX)table).walkDatabase(lastKey, Math.abs(batch.getProposeLimit()), this);
				if (null == lastKey) {
					stopTableBatch(timerId);
					return;
				}
				saveLastKey(lastKey);
			}
		}
	}
	/**
	 * 开始SortedMap遍历批处理。
	 *
	 * @param table table
	 * @param key 记录的key
	 * @param jobHandle jobHandle
	 * @param oneByOneKey oneByOneKey
	 * @return jobId
	 */
	public String startWalkSortedMap(@Nullable TableX<?, ?> table, @Nullable Comparable<?> key,
	                                 @NotNull WalkJobHandle jobHandle, @Nullable Object oneByOneKey) throws Exception {
		return startWalkSortedMap(table, key, jobHandle, oneByOneKey, 60_000, 100);
	}

	/**
	 * 开始SortedMap遍历批处理。对每一个记录通过jobHandle回调进行处理。每个记录的处理在独立的存储过程中。遍历时分批处理。支持多种执行模式：
	 * 1. one by one 需要提供oneByOneKey，一般是roleId或者account。
	 * 2. 顺序在walk线程中执行事务。
	 * 3. 并发执行jobHandle。
	 * walk的sortedmap来源分为两种：
	 * 1. table.record 里面的某个字段，此时参数table,key指向这个记录。
	 * 2. 内存中的任意NavigableMap。此时table,key为null。
	 * 实现须知：WalkJobHandle.tailMapExclusiveOutofTransaction 是在事务外调用的。所以返回的NavigableMap需要支持并发读取。
	 * 如果NavigableMap是记录中的字段，此时使用selectDirty得到记录并返回map。如果NavigableMap是自己的内存数据，
	 * 需要支持多线程读取安全。建议使用org.pcollections.PSortedMap。
	 * @param table table
	 * @param key 记录的key
	 * @param jobHandle jobHandle
	 * @param oneByOneKey oneByOneKey
	 * @param checkPeriod 任务运行监控Timer间隔
	 * @param limit 每次创建的Job
	 * @return jobId
	 */
	public String startWalkSortedMap(@Nullable TableX<?, ?> table, @Nullable Comparable<?> key,
									 @NotNull WalkJobHandle jobHandle, @Nullable Object oneByOneKey,
									 long checkPeriod, int limit) throws Exception {
		if (stopped)
			throw new IllegalStateException("stopped");
		if (limit == 0)
			throw new IllegalStateException("limit is 0");
		if (null == table || null == key) {
			// 遍历内存中的map
			if (Transaction.getCurrent() != null) {
				// 事务内，另起一个线程管理jobs任务。这里本来想用onebyone避免太多并发任务，但是这样与内部的runJobs.proc会死锁。
				Task.run(() -> {
					var tail = jobHandle.tailMapExclusiveOutofTransaction(null, null, null);
					new SortedMapWorker(jobHandle, oneByOneKey, tail.size()).runJobs(tail, true);
				}, "SortedMapWorker");
			} else {
				var tail = jobHandle.tailMapExclusiveOutofTransaction(null, null, null);
				new SortedMapWorker(jobHandle, oneByOneKey, tail.size()).runJobs(tail, false);
			}
			return "";
		}
		return _saveAndStart(table, key, jobHandle, oneByOneKey, checkPeriod, limit);
	}

	private String _saveAndStart(@NotNull TableX<?, ?> table, @Nullable Comparable<?> key,
								 @NotNull WalkJobHandle jobHandle, @Nullable Object oneByOneKey,
								 long checkPeriod, int limit) {
		var timerId = zeze.getTimer().schedule(checkPeriod, checkPeriod,
			TableBatchTimerHandle.class, new BAppInstanceId(zeze.getProjectName()));
		var batch = _tSafeBatch.getOrAdd(timerId);
		batch.setTableName(table.getName());
		if (null != key)
			batch.setRecordKey(new Binary(table.encodeKey(key)));
		batch.setProposeLimit(limit);
		batch.setJobClass(jobHandle.getClass().getName());
		if (null != oneByOneKey) {
			var buffer = ByteBuffer.Allocate();
			jobHandle.encodeOneByOneKey(buffer, oneByOneKey);
			batch.setOneByOneKey(new Binary(buffer));
		}
		Transaction.whileCommit(() -> _startWorker(timerId, jobHandle, oneByOneKey, batch));
		return timerId;
	}

	public class SortedMapWorker extends Worker {
		private final BBatch.Data batch;
		private Comparable<?> lastKey;

		public SortedMapWorker(String timerId, WalkJobHandle jobHandle, Object oneByOneKey, BBatch.Data batch) {
			super((TableX<?, ?>)zeze.getTable(batch.getTableName()), batch.getProposeLimit(), timerId, jobHandle, oneByOneKey);
			var lastKeyBin = batch.getLastKey();
			lastKey = lastKeyBin.size() != 0 ? table.decodeKey(ByteBuffer.Wrap(lastKeyBin)) : null;
			this.batch = batch;
		}

		public SortedMapWorker(WalkJobHandle jobHandle, Object oneByOneKey, int limit) {
			super(null, limit, "", jobHandle, oneByOneKey);
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
				lastKey = runJobs(tail, true);
				if (null == lastKey) {
					stopTableBatch(timerId);
					return;
				}
				saveLastKey(lastKey);
			}
		}

		public Comparable<?> runJobs(@NotNull NavigableMap<?, ?> tail, boolean wait) throws Exception {
			var it = tail.entrySet().iterator();
			var _limit = Math.abs(limit);
			Object key = null;
			for (; _limit > 0 && it.hasNext(); --_limit) {
				var e = it.next();
				handle(e.getKey(), e.getValue());
				key = e.getKey();
			}
			if (wait)
				endWalk(_limit);
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
