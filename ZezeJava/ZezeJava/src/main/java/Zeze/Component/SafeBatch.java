package Zeze.Component;

import Zeze.Application;
import Zeze.Builtin.SafeBatch.BAppInstanceId;
import Zeze.Builtin.SafeBatch.BBatchTable;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class SafeBatch extends AbstractSafeBatch {
	private final @NotNull Application zeze;
	private final ConcurrentHashMap<String, Future<?>> running = new ConcurrentHashMap<>();
	private final HotHandle<ITableJob> hotHandle = new HotHandle<>();

	/**
	 * 开始表格遍历批处理。
	 *
	 * @param table table
	 * @param jobHandle jobHandleClass
	 * @param oneByOneKey oneByOneKey，null表示其他执行模式
	 * @return jobId
	 */
	public String startTableBatch(TableX<?, ?> table, ITableJob jobHandle, Object oneByOneKey) {
		return startTableBatch(table, jobHandle, oneByOneKey, 0, 60_000, 100);
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
	public String startTableBatch(TableX<?, ?> table, ITableJob jobHandle, Object oneByOneKey, long delay, long checkPeriod, int limit) {
		var timerId = zeze.getTimer().schedule(delay, checkPeriod,
			TableBatchTimerHandle.class, new BAppInstanceId(zeze.getInstanceId()));
		var batch = _tSafeBatchTable.getOrAdd(timerId);
		batch.setTableName(table.getName());
		batch.setProposeLimit(limit);
		batch.setJobClass(jobHandle.getClass().getName());
		if (null != oneByOneKey) {
			var buffer = ByteBuffer.Allocate();
			jobHandle.encodeOneByOneKey(buffer, oneByOneKey);
			batch.setOneByOneKey(new Binary(buffer));
		}
		Transaction.whileCommit(() -> running.computeIfAbsent(timerId, (key) -> {
			var worker = new TableBatchWorker(timerId, jobHandle, oneByOneKey, batch.toData());
			var future = Task.runUnsafe(worker, "TableBatchWorker_" + timerId);
			worker.setFuture(future);
			return future;
			}));
		return timerId;
	}

	public void start() throws Exception {
		startTableBatch(_tSafeBatchTable, new TableBatchLoadHandle(), null, 0, 100, 100);
	}

	public void stop() throws Exception {

	}

	public static class TableBatchTimerHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			var custom = (BAppInstanceId)context.customData;
			if (null != custom) {
				var zeze = Application.getAppInstance(custom.getAppInstanceId());
				if (null != zeze) {
					zeze.getSafeBatch().checkTableBatch(context.timerId, zeze.getSafeBatch()._tSafeBatchTable.get(context.timerId));
				}
			}
		}
	}

	public static class TableBatchLoadHandle implements ITableJob {
		@Override
		public long runJob(SafeBatch safeBatch, Object _key, Object _value) throws Exception {
			var key = (String)_key;
			var value = (BBatchTable)_value;
			safeBatch.checkTableBatch(key, value);
			return 0;
		}
	}

	void checkTableBatch(String timerId, BBatchTable batch) throws Exception {
		if (null == batch) {
			_stopTableBatch(timerId);
			return;
		}

		if (running.containsKey(timerId)) {
			return; // already running
		}

		var jobHandle = hotHandle.findHandle(zeze, batch.getJobClass());
		var oneByOneKey = jobHandle.decodeOneByOneKey(ByteBuffer.Wrap(batch.getOneByOneKey()));
		Transaction.whileCommit(() -> running.computeIfAbsent(timerId, (key) -> {
			var worker = new TableBatchWorker(timerId, jobHandle, oneByOneKey, batch.toData());
			var future = Task.runUnsafe(worker, "TableBatchWorker_" + timerId);
			worker.setFuture(future);
			return future;
		}));
	}

	private long _stopTableBatch(String jobId) {
		zeze.getTimer().cancel(jobId);
		_tSafeBatchTable.remove(jobId);
		Transaction.whileCommit(() -> {
			var job = running.remove(jobId);
			if (null != job)
				job.cancel(true);
		});
		return 0;
	}

	public void stopTableBatch(String jobId) {
		if (Transaction.getCurrent() != null) {
			_stopTableBatch(jobId);
			return;
		}
		zeze.newProcedure(() -> _stopTableBatch(jobId), "stopTableBatch_" + jobId).call();
	}

	private class TableBatchWorker implements Action0, TableWalkHandle<Object, Object> {
		private final String timerId;
		private final BBatchTable.Data context;
		private final TableX<?, ?> table;
		private final ITableJob jobHandle;
		private final Object oneByOneKey;
		private Comparable<?> lastKey;
		private final ArrayList<Future<?>> futures = new ArrayList<>();
		private Future<?> futureSelf;

		public TableBatchWorker(String timerId, ITableJob jobHandle, Object oneByOneKey, BBatchTable.Data context) {
			this.timerId = timerId;
			this.jobHandle = jobHandle;
			this.oneByOneKey = oneByOneKey;
			this.context = context;
			this.table = (TableX<?, ?>)zeze.getTable(context.getTableName());
		}

		public void setFuture(Future<?> future) {
			this.futureSelf = future;
		}

		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		public void run() throws Exception {
			// isDone 不必要。
			while (!futureSelf.isCancelled() && !futureSelf.isDone()) {
				lastKey = ((TableX)table).walkDatabase(lastKey, Math.abs(context.getProposeLimit()), this);
				if (null == lastKey) {
					stopTableBatch(timerId);
					return;
				}
			}
		}

		@Override
		public long endWalk(long count) throws Exception {
			if (null != oneByOneKey) {
				// one by one 模式，加入一个任务并等待完成。
				var future = new TaskCompletionSource<Boolean>();
				zeze.getTaskOneByOneByKey().Execute(oneByOneKey, () -> future.setResult(true));
				future.await();
			} else if (context.getProposeLimit() < 0){
				// 并发执行模式，全部等待。
				for (var future : futures)
					future.get();
				futures.clear();
			} // else 立即call模式，不需要额外等待。
			return count;
		}

		@Override
		public boolean handle(@NotNull Object key, @NotNull Object value) throws Exception {
			var proc = zeze.newProcedure(() -> {
				var result = jobHandle.runJob(SafeBatch.this, key, value);
				if (0 == result) {
					var batch = _tSafeBatchTable.get(timerId);
					if (null != batch)
						batch.setLastTableKey(new Binary(table.encodeKey(key)));
				}
				return result;
			},"TableBatchJob_" + timerId);

			if (null != oneByOneKey) {
				// one by one 执行。
				zeze.getTaskOneByOneByKey().Execute(oneByOneKey, proc);
				return true;
			}

			if (context.getProposeLimit() > 0) {
				// 立即当前线程执行。
				proc.call();
				return true;
			}
			// 并发执行。
			futures.add(Task.runUnsafe(proc));

			return true;
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
	public String startBatchSortedMap(TableX<?, ?> table, ByteBuffer key, Class<ISortedMapJob> jobHandle, Object oneByOneKey) {
		return startBatchSortedMap(table, key, jobHandle, oneByOneKey, 60_000, 100);
	}

	/**
	 * 开始SortedMap遍历批处理。
	 *
	 * @param table table
	 * @param key 记录的key
	 * @param jobHandle jobHandleClass
	 * @param oneByOneKey oneByOneKey
	 * @param period 任务运行监控Timer间隔
	 * @param limit 每次创建的Job
	 * @return jobId
	 */
	public String startBatchSortedMap(TableX<?, ?> table, ByteBuffer key, Class<ISortedMapJob> jobHandle, Object oneByOneKey, long period, int limit) {
		return null;
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

	public interface ITableJob {
		long runJob(SafeBatch safeBatch, Object key, Object value) throws Exception;
		default Object decodeOneByOneKey(ByteBuffer buffer) {
			return null;
		}
		default void encodeOneByOneKey(ByteBuffer buffer, Object key) {
		}
	}
	public interface ISortedMapJob {
		Iterator<Map.Entry<?, ?>> lowerBound(TableX<?, ?> table, ByteBuffer tableKey, ByteBuffer mapKey) throws Exception;
		long runJob(SafeBatch safeBatch, Map.Entry<?, ?> entry);
		default Object decodeOneByOneKey(ByteBuffer buffer) {
			return null;
		}
		default void encodeOneByOneKey(ByteBuffer buffer, Object key) {
		}
	}
}
