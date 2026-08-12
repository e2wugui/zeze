package Zeze.Component;

import Zeze.Application;
import Zeze.Builtin.SafeBatch.BBatch;
import Zeze.Builtin.SafeBatch.BBatchReadOnly;
import Zeze.Hot.HotHandle;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.TableWalkHandle;
import Zeze.Transaction.TableX;
import Zeze.Transaction.Transaction;
import Zeze.Util.Action0;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NavigableMap;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SafeBatch extends AbstractSafeBatch {
	private final @NotNull Application zeze;
	private final ConcurrentHashMap<String, Future<?>> running = new ConcurrentHashMap<>();
	private final HotHandle<WalkJobHandle> hotWalkJobHandles = new HotHandle<>();
	private volatile boolean stopped = false;

	// WalkJobHandle 保留为标记接口：HotHandle 反射加载、BBatch.jobClass 持久化使用此公共基类型。
	// 真正的回调签名由下面三个子接口给出，分别对应三种 worker 语义。
	public interface WalkJobHandle {
	}

	// 表遍历。key/value 直接对应 TableX<K, V> 的类型。
	@FunctionalInterface
	public interface WalkTableJobHandle<K, V> extends WalkJobHandle {
		// 每个记录回调一次。在存储过程中回调。
		// 返回0表示成功，如果返回非0，中断批处理。
		long runJob(SafeBatch safeBatch, K key, V value) throws Exception;
	}

	// SortedMap遍历。MK 是 map key（map 自身要求 Comparable 排序），MV 是 map value。
	// 注意：MK 与 TableX<K, V> 的 K 是不同的概念。
	public interface WalkSortedMapJobHandle<MK, MV> extends WalkJobHandle {
		long runJob(SafeBatch safeBatch, MK key, MV value) throws Exception;

		// 遍历内存中的SortedMap时需要实现。
		// 返回的NavigableMap的值必须>mapKey。也就是使用tailMap(mapKey, false)得到它。
		@Nullable NavigableMap<MK, MV> getSortedMapOutTransaction(@NotNull TableX<?, ?> table,
																  @NotNull ByteBuffer tableKey) throws Exception;

		@Nullable ByteBuffer encodeMapKey(@NotNull TableX<?, ?> table, @NotNull ByteBuffer tableKey, @NotNull MK mapKey);
		@NotNull MK decodeMapKey(@NotNull TableX<?, ?> table, @NotNull ByteBuffer tableKey, @NotNull ByteBuffer bb);
	}

	// List遍历。框架按 index 推进进度，因此 key 类型固定为 int，只参数化元素类型 E。
	public interface WalkListJobHandle<E> extends WalkJobHandle {
		// index 是当前元素在 List 中的位置+1（参见 ListWorker.runJobs）。
		long runJob(SafeBatch safeBatch, int index, E value) throws Exception;

		// 根据table,tableKey定位到记录中的某个List。
		// 注意：在批处理过程中如果List的内容发生变化，那么处理的item可能丢失或重复。
		@Nullable List<E> getListOutTransaction(@NotNull TableX<?, ?> table, @NotNull ByteBuffer tableKey) throws Exception;
	}

	// 查询批处理状态。
	public BBatchReadOnly getBatch(@NotNull String jobId) {
		return (BBatchReadOnly)zeze.getTimer().getTimerCustomBean(jobId);
	}
	/**
	 * 开始表格遍历批处理。
	 *
	 * @param table table
	 * @param jobHandle jobHandle
	 * @return jobId
	 */
	public <TK extends Comparable<TK>, TV extends Bean> String startWalkTable(TableX<TK, TV> table, WalkTableJobHandle<TK, TV> jobHandle) {
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
	public <TK extends Comparable<TK>, TV extends Bean> String startWalkTable(TableX<TK, TV> table, WalkTableJobHandle<TK, TV> jobHandle,
	                                      long checkPeriod, int limit) {
		if (stopped)
			throw new IllegalStateException("stopped");
		if (limit <= 0)
			throw new IllegalStateException("limit is 0");
		return _saveAndStart(table, null, jobHandle, checkPeriod, limit, eWorkerTable);
	}

	public void start() throws Exception {
		stopped = false;
	}

	public void stop() throws Exception {
		stopped = true;
		for (var run : running.values()) {
			run.cancel(false);
		}
		for (var run : running.values()) {
			try {
				run.get(1, TimeUnit.SECONDS);
			} catch (Exception ignored) {
				// ignored
			}
		}
		running.clear();
	}

	public static class BatchTimerHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) throws Exception {
			var custom = (BBatch)context.customData;
			if (null != custom) {
				var zeze = Application.getAppInstance(custom.getAppInstanceId());
				if (null != zeze) {
					zeze.getSafeBatch().checkBatch(context.timerId, custom);
				}
			}
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void _startWorker(@NotNull String timerId, @NotNull WalkJobHandle jobHandle,
							  @NotNull BBatch batch) {
		if (!stopped) {
			running.computeIfAbsent(timerId, (key) -> {
				Worker worker = switch (batch.getWorker()) {
					// HotHandle 反射加载路径：泛型已被擦除，只能用 raw type 构造。
					case eWorkerTable -> new TableBatchWorker(timerId,
							(WalkTableJobHandle)jobHandle, batch.toData());
					case eWorkerSortedMap -> new SortedMapWorker(timerId,
							(WalkSortedMapJobHandle)jobHandle, batch.toData());
					case eWorkerList -> new ListWorker(timerId,
							(WalkListJobHandle)jobHandle, batch.toData());
					default -> throw new IllegalStateException("invalid worker type.");
				};
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
			_stopBatch(timerId);
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

	private long _stopBatch(String jobId) {
		zeze.getTimer().cancel(jobId);
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

	public void stopBatch(String jobId) {
		if (Transaction.getCurrent() != null) {
			_stopBatch(jobId);
			return;
		}
		zeze.newProcedure(() -> _stopBatch(jobId), "stopBatch_" + jobId).call();
	}

	private abstract static class Worker implements Action0 {
		protected final String timerId;
		protected final TableX<?, ?> table;
		protected final int limit;

		protected volatile Future<?> futureSelf;

		public void setFuture(Future<?> future) {
			this.futureSelf = future;
		}

		public Worker(TableX<?, ?> table, int limit, String timerId) {
			this.timerId = timerId;
			this.table = table;
			this.limit = limit;
		}
	}

	private class TableBatchWorker<TK extends Comparable<TK>, TV extends Bean> extends Worker implements TableWalkHandle<TK, TV> {
		private final WalkTableJobHandle<TK, TV> jobHandle;
		private final TableX<TK, TV> typedTable;
		private final BBatch.Data batch;
		private TK lastKey;

		@SuppressWarnings("unchecked")
		public TableBatchWorker(String timerId, WalkTableJobHandle<TK, TV> jobHandle, BBatch.Data batch) {
			super((TableX<?, ?>)zeze.getTable(batch.getTableName()), batch.getProposeLimit(), timerId);
			this.typedTable = (TableX<TK, TV>)table;
			this.batch = batch;
			this.jobHandle = jobHandle;
			var lastKeyBin = batch.getLastKey();
			lastKey = lastKeyBin.size() != 0 ? typedTable.decodeKey(ByteBuffer.Wrap(lastKeyBin)) : null;
		}

		@Override
		public boolean handle(@NotNull TK key, @NotNull TV value) throws Exception {
			// 当前线程直接调用。call内部基础处理了所有异常。如果还有异常抛出，中断这次批处理，等待timer重启。
			// 当前记录处理失败，中断批执行，下一次重启批处理，但跳过当前数据。
			return 0 == zeze.newProcedure(() -> {
				var result = jobHandle.runJob(SafeBatch.this, key, value);
				if (0 == result && !timerId.isEmpty()) {
					// 每次记录处理都保存一次lastKey。
					var batch = (BBatch)zeze.getTimer().getTimerCustomBean(timerId);
					if (null != batch) {
						batch.setLastKey(new Binary(typedTable.encodeKey(key)));
					}
				}
				return result;
			}, "SafeBatchTable_" + timerId).call();
		}

		@Override
		public void run() throws Exception {
			while (null == futureSelf) {
				// busy wait, see _startWorker...setFuture
				Thread.onSpinWait();
			}
			// isDone 不必要。
			while (!futureSelf.isCancelled() && !futureSelf.isDone()) {
				lastKey = typedTable.walkDatabase(lastKey, batch.getProposeLimit(), this);
				if (null == lastKey) {
					stopBatch(timerId);
					break;
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
	public <MK, MV> String startWalkSortedMap(@NotNull TableX<?, ?> table, @NotNull Comparable<?> key,
	                                          @NotNull WalkSortedMapJobHandle<MK, MV> jobHandle) throws Exception {
		return startWalkSortedMap(table, key, jobHandle, 60_000, 100);
	}

	/**
	 * 开始SortedMap遍历批处理。对每一个记录通过jobHandle回调进行处理。每个记录的处理在独立的存储过程中。遍历时分批处理：
	 * 内部回调WalkJobHandle.getSortedMapOutTransaction得到table.record里面的某个sortedmap。
	 * 使用selectDirty得到记录并返回map
	 * @param table table
	 * @param key 记录的key
	 * @param jobHandle jobHandle
	 * @param checkPeriod 任务运行监控Timer间隔
	 * @param limit 每次创建的Job
	 * @return jobId
	 */
	public <MK, MV> String startWalkSortedMap(@NotNull TableX<?, ?> table, @NotNull Comparable<?> key,
	                                          @NotNull WalkSortedMapJobHandle<MK, MV> jobHandle,
	                                          long checkPeriod, int limit) throws Exception {
		if (stopped)
			throw new IllegalStateException("stopped");
		if (limit <= 0)
			throw new IllegalStateException("limit is 0");
		return _saveAndStart(table, key, jobHandle, checkPeriod, limit, eWorkerSortedMap);
	}

	/**
	 * 开始List遍历批处理。
	 *
	 * @param table table
	 * @param key 记录的key
	 * @param jobHandle jobHandle
	 * @return jobId
	 */
	public <E> String startWalkList(@NotNull TableX<?, ?> table, @NotNull Comparable<?> key,
	                                @NotNull WalkListJobHandle<E> jobHandle) throws Exception {
		return startWalkList(table, key, jobHandle, 60_000, 100);
	}

	/**
	 * 开始List遍历批处理。对每一个记录通过jobHandle回调进行处理。每个记录的处理在独立的存储过程中。遍历时分批处理：
	 * 内部回调WalkJobHandle.getList得到table.record里面的某个List字段的。
	 * 使用selectDirty得到记录并返回list.
	 * @param table table
	 * @param key 记录的key
	 * @param jobHandle jobHandle
	 * @param checkPeriod 任务运行监控Timer间隔
	 * @param limit 每次创建的Job
	 * @return jobId
	 */
	public <E> String startWalkList(@NotNull TableX<?, ?> table, @NotNull Comparable<?> key,
	                                @NotNull WalkListJobHandle<E> jobHandle, long checkPeriod, int limit) {
		if (stopped)
			throw new IllegalStateException("stopped");
		if (limit <= 0)
			throw new IllegalStateException("limit is 0");
		return _saveAndStart(table, key, jobHandle, checkPeriod, limit, eWorkerList);
	}

	private String _saveAndStart(@NotNull TableX<?, ?> table, @Nullable Comparable<?> key,
								 @NotNull WalkJobHandle jobHandle, long checkPeriod, int limit,
								 int workerType) {
		if (Transaction.getCurrent() == null) {
			var jobId = new OutObject<String>();
			if (0 != zeze.newProcedure(() -> {
				jobId.value = _saveAndStart(table, key, jobHandle, checkPeriod, limit, workerType);
				return 0;
			}, "SafeBatchStart").call())
				throw new RuntimeException("SafeBatchStart _saveAndStart return non zero.");
			return jobId.value;
		}

		var batch = new BBatch();
		batch.setAppInstanceId(zeze.getProjectName());
		batch.setTableName(table.getName());
		if (null != key)
			batch.setRecordKey(new Binary(table.encodeKey(key)));
		batch.setProposeLimit(limit);
		batch.setJobClass(jobHandle.getClass().getName());
		batch.setWorker(workerType);

		var timerId = zeze.getTimer().schedule(checkPeriod, checkPeriod, BatchTimerHandle.class, batch);
		Transaction.whileCommit(() -> _startWorker(timerId, jobHandle, batch));
		return timerId;
	}

	public class SortedMapWorker<MK, MV> extends Worker {
		private final WalkSortedMapJobHandle<MK, MV> jobHandle;
		private final BBatch.Data batch;
		private MK lastKey;

		public SortedMapWorker(String timerId, WalkSortedMapJobHandle<MK, MV> jobHandle, BBatch.Data batch) {
			super((TableX<?, ?>)zeze.getTable(batch.getTableName()), batch.getProposeLimit(), timerId);
			this.jobHandle = jobHandle;
			this.batch = batch;
			var lastKeyBin = batch.getLastKey();
			lastKey = lastKeyBin.size() != 0
				? jobHandle.decodeMapKey(table, ByteBuffer.Wrap(batch.getRecordKey()), ByteBuffer.Wrap(lastKeyBin))
				: null;
		}

		public boolean handle(@NotNull MK key, @NotNull MV value) throws Exception {
			// 当前线程直接调用。call内部基础处理了所有异常。如果还有异常抛出，中断这次批处理，等待timer重启。
				// 当前记录处理失败，中断批执行，下一次重启批处理，但跳过当前数据。
			return 0 == zeze.newProcedure(() -> {
				var result = jobHandle.runJob(SafeBatch.this, key, value);
				if (0 == result && !timerId.isEmpty()) {
					// 每次记录处理都保存一次lastKey。
					var batch = (BBatch)zeze.getTimer().getTimerCustomBean(timerId);
					if (null != batch) {
						var bb = jobHandle.encodeMapKey(table, ByteBuffer.Wrap(batch.getRecordKey()), key);
						if (null != bb)
							batch.setLastKey(new Binary(bb));
					}
				}
				return result;
			}, "SafeBatchSortedMap_" + timerId).call();
		}

		@Override
		public void run() throws Exception {
			while (null == futureSelf) {
				// busy wait, see _startWorker...setFuture
				Thread.onSpinWait();
			}
			while (!futureSelf.isCancelled() && !futureSelf.isDone()) {
				var tail = jobHandle.getSortedMapOutTransaction(table, ByteBuffer.Wrap(batch.getRecordKey()));
				if (null == tail) {
					break;
				}
				if (null != lastKey) {
					tail = tail.tailMap(lastKey, false);
				}
				lastKey = runJobs(tail);
				if (null == lastKey) {
					stopBatch(timerId);
					break;
				}
			}
		}

		private MK runJobs(@NotNull NavigableMap<MK, MV> tail) throws Exception {
			var it = tail.entrySet().iterator();
			var _limit = limit;
			MK key = null;
			for (; _limit > 0 && it.hasNext(); --_limit) {
				var e = it.next();
				key = e.getKey();
				if (!handle(e.getKey(), e.getValue()))
					break; // handle 失败，跳过这一个。
			}
			return it.hasNext() ? key : null;
		}
	}

	public class ListWorker<E> extends Worker {
		private final WalkListJobHandle<E> jobHandle;
		private final BBatch.Data batch;
		private int next;

		public ListWorker(String timerId, WalkListJobHandle<E> jobHandle, BBatch.Data batch) {
			super((TableX<?, ?>)zeze.getTable(batch.getTableName()), batch.getProposeLimit(), timerId);
			this.jobHandle = jobHandle;
			var lastKeyBin = batch.getLastKey();
			next = lastKeyBin.size() != 0 ? ByteBuffer.Wrap(lastKeyBin).ReadInt() : 0;
			this.batch = batch;
		}

		public boolean handle(int index, E value) throws Exception {
			// 当前线程直接调用。call内部基础处理了所有异常。如果还有异常抛出，中断这次批处理，等待timer重启。
			// 当前记录处理失败，中断批执行，下一次重启批处理，但跳过当前数据。
			return 0 == zeze.newProcedure(() -> {
				var result = jobHandle.runJob(SafeBatch.this, index, value);
				if (0 == result && !timerId.isEmpty()) {
					// 每次记录处理都保存一次lastKey。
					var batch = (BBatch)zeze.getTimer().getTimerCustomBean(timerId);
					if (null != batch) {
						var bb = ByteBuffer.Allocate();
						bb.WriteInt(index);
						batch.setLastKey(new Binary(bb));
					}
				}
				return result;
			}, "SafeBatchList_" + timerId).call();
		}

		@Override
		public void run() throws Exception {
			while (null == futureSelf) {
				// busy wait, see _startWorker...setFuture
				Thread.onSpinWait();
			}
			while (!futureSelf.isCancelled() && !futureSelf.isDone()) {
				var list = jobHandle.getListOutTransaction(table, ByteBuffer.Wrap(batch.getRecordKey()));
				if (null == list) {
					break;
				}
				next = runJobs(list);
				if (next < 0) {
					stopBatch(timerId);
					break;
				}
			}
		}

		private int runJobs(@NotNull List<E> list) throws Exception {
			var i = next;
			for (var _limit = limit; i < list.size() && _limit > 0; --_limit) {
				var e = list.get(i);
				++i; // 马上推进一个，当前handle的e必须处理，处理失败则跳过。
				if (!handle(i, e))
					break;
			}
			return i < list.size() ? i : -1;
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
