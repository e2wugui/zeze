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
		default NavigableMap<?, ?> tailMap(@Nullable TableX<?, ?> table,
										   @Nullable ByteBuffer tableKey,
										   @Nullable Comparable<?> mapKey) throws Exception {
			return null;
		}
	}
	/**
	 * 开始表格遍历批处理。
	 *
	 * @param table table
	 * @param jobHandle jobHandleClass
	 * @param oneByOneKey oneByOneKey，null表示其他执行模式
	 * @return jobId
	 */
	public String startWalkTable(TableX<?, ?> table, WalkJobHandle jobHandle, Object oneByOneKey) {
		return startWalkTable(table, jobHandle, oneByOneKey, 60_000, 100);
	}

	/**
	 * 开始表格遍历批处理。
	 *
	 * @param table table
	 * @param jobHandle jobHandleClass
	 * @param oneByOneKey oneByOneKey，null表示其他执行模式，见下面的limit。
	 * @param checkPeriod 任务运行监控Timer的间隔
	 * @param limit 每批遍历的Job数量，负数表示并发执行（同时oneByOneKey要为null）。
	 * @return jobId
	 */
	public String startWalkTable(TableX<?, ?> table, WalkJobHandle jobHandle,
	                             Object oneByOneKey, long checkPeriod, int limit) {
		return _saveAndStart(table, null, jobHandle, oneByOneKey, checkPeriod, limit);
	}

	public void start() throws Exception {
		startWalkTable(_tSafeBatch, new TableLoadHandle(), null, 100, 100);
	}

	public void stop() throws Exception {
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
		running.computeIfAbsent(timerId, (key) -> {
			var worker = (batch.getRecordKey().size() == 0)
				? new TableBatchWorker(timerId, jobHandle, oneByOneKey, batch.toData())
				: new SortedMapWorker(timerId, jobHandle, oneByOneKey, batch.toData());
			var future = Task.runUnsafe(worker, "BatchWorker_" + timerId);
			worker.setFuture(future);
			return future;
		});
	}

	void checkTableBatch(String timerId, BBatch batch) throws Exception {
		if (null == batch) {
			_stopTableBatch(timerId);
			return;
		}

		if (running.containsKey(timerId)) {
			return; // already running
		}

		var jobHandle = hotWalkJobHandles.findHandle(zeze, batch.getJobClass());
		var oneByOneKey = jobHandle.decodeOneByOneKey(ByteBuffer.Wrap(batch.getOneByOneKey()));
		Transaction.whileCommit(() -> _startWorker(timerId, jobHandle, oneByOneKey, batch));
	}

	private long _stopTableBatch(String jobId) {
		zeze.getTimer().cancel(jobId);
		_tSafeBatch.remove(jobId);
		Transaction.whileCommit(() -> {
			var job = running.remove(jobId);
			if (null != job) {
				job.cancel(false);
				try {
					job.get();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
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

		protected Future<?> futureSelf;
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
			} else if (limit < 0){
				// 并发执行模式，全部等待。
				for (var future : futures)
					future.get();
				futures.clear();
			} // else 立即call模式，不需要额外等待。
			return count;
		}

		@Override
		public boolean handle(@NotNull Object key, @NotNull Object value) throws Exception {
			var proc = zeze.newProcedure(() -> jobHandle.runJob(SafeBatch.this, key, value),
				"BatchJob_" + timerId);

			if (null != oneByOneKey) {
				// one by one 执行。
				zeze.getTaskOneByOneByKey().Execute(oneByOneKey, proc);
				return true;
			}

			if (limit > 0) {
				// 立即当前线程执行。
				futures.add(Task.runUnsafe(proc));
				return true;
			}

			// 并发执行。
			futures.add(Task.runUnsafe(proc));
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
			}, "").call();
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
	 * @param jobHandle jobHandleClass
	 * @param oneByOneKey oneByOneKey
	 * @return jobId
	 */
	public String startWalkSortedMap(@Nullable TableX<?, ?> table, @Nullable Comparable<?> key,
	                                 @NotNull WalkJobHandle jobHandle, @Nullable Object oneByOneKey) throws Exception {
		return startWalkSortedMap(table, key, jobHandle, oneByOneKey, 60_000, 100);
	}

	/**
	 * 开始SortedMap遍历批处理。
	 *
	 * @param table table
	 * @param key 记录的key
	 * @param jobHandle jobHandleClass
	 * @param oneByOneKey oneByOneKey
	 * @param checkPeriod 任务运行监控Timer间隔
	 * @param limit 每次创建的Job
	 * @return jobId
	 */
	public String startWalkSortedMap(@Nullable TableX<?, ?> table, @Nullable Comparable<?> key,
									 @NotNull WalkJobHandle jobHandle, @Nullable Object oneByOneKey,
									 long checkPeriod, int limit) throws Exception {
		if (null == table || null == key) {
			var tail = jobHandle.tailMap(null, null, null);
			new SortedMapWorker(jobHandle, oneByOneKey, tail.size()).runJobs(tail, false);
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
			while (!futureSelf.isCancelled() && !futureSelf.isDone()) {
				var tail = jobHandle.tailMap(table, ByteBuffer.Wrap(batch.getRecordKey()), lastKey);
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
				endWalk(limit);
			return (Comparable<?>)key;
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
