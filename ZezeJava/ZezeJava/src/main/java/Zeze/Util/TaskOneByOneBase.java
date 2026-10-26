package Zeze.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntFunction;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

// one-by-one 队列引擎抽象基类：按 key 取桶队列（getAndLockQueue）、屏障与批量聚合提交，旧 Execute 委托 TaskSpec
public abstract class TaskOneByOneBase extends ReentrantLock {
	private static final Logger logger = LogManager.getLogger(TaskOneByOneBase.class);

	/** 【副作用】keys 将被就地排序（Comparable升序）：分组前先定序，调用方持有的列表顺序会改变。 */
	public <T extends Comparable<T>> void executeCyclicBarrier(List<T> keys, Procedure procedure,
															   @Nullable Action0 cancel, @Nullable DispatchMode mode) {
		lock();
		try {
			if (keys.isEmpty())
				throw new IllegalArgumentException("CyclicBarrier keys is empty.");

			keys.sort(Comparable::compareTo);
			try (var queues = new LockedQueues(keys)) {
				var barrier = new TaskOneByOneQueue.BarrierProcedure(procedure, queues.count, cancel);
				submitBarrierAndUnlock(queues, barrier, (sum) -> new TaskOneByOneQueue.TaskBarrierProcedure(barrier, sum, mode));
			}
		} finally {
			unlock();
		}
	}

	/** 【副作用】keys 将被就地排序（Comparable升序）：分组前先定序，调用方持有的列表顺序会改变。 */
	public <T extends Comparable<T>> void executeCyclicBarrier(List<T> keys, String actionName,
															   Action0 action, @Nullable Action0 cancel,
															   @Nullable DispatchMode mode) {
		lock();
		try {
			if (keys.isEmpty())
				throw new IllegalArgumentException("CyclicBarrier keys is empty.");

			keys.sort(Comparable::compareTo);
			try (var queues = new LockedQueues(keys)) {
				var barrier = new TaskOneByOneQueue.BarrierAction(actionName, action, queues.count, cancel);
				submitBarrierAndUnlock(queues, barrier, (sum) -> new TaskOneByOneQueue.TaskBarrierAction(barrier, sum, mode));
			}
		} finally {
			unlock();
		}
	}

	/** 从取得锁到提交移交均由同一个作用域持有；准备失败也释放此前取得的全部重入次数。 */
	private final class LockedQueues implements AutoCloseable {
		final HashMap<TaskOneByOneQueue, OutInt> groups = new HashMap<>();
		int count;

		LockedQueues(List<?> keys) {
			try {
				for (var key : keys) {
					var queue = getAndLockQueue(key);
					var registered = false;
					try {
						groups.computeIfAbsent(queue, __ -> new OutInt()).value++;
						registered = true;
						count++;
					} finally {
						if (!registered)
							queue.unlock();
					}
				}
			} catch (Throwable e) {
				close();
				throw e;
			}
		}

		@Override
		public void close() {
			for (var entry : groups.entrySet())
				for (var i = 0; i < entry.getValue().value; i++)
					entry.getKey().unlock();
			groups.clear();
		}
	}

	/** 先入队并释放全部桶锁，再派发；派发失败的同步回滚也可能执行取消回调。 */
	private static void submitBarrierAndUnlock(LockedQueues queues,
											   TaskOneByOneQueue.Barrier barrier,
											   IntFunction<TaskOneByOneQueue.Task> newTask) {
		// 入队前预留全部空间，避免取得派发认领后因列表扩容失败而丢失驱动。
		var dispatches = new ArrayList<Runnable>(queues.groups.size());
		Throwable failure = null;
		try (queues) {
			for (var it = queues.groups.entrySet().iterator(); it.hasNext(); ) {
				var entry = it.next();
				var sum = entry.getValue().value;
				var task = newTask.apply(sum);
				var queue = entry.getKey();
				it.remove(); // 此后由本次入队的 finally 释放本桶的全部锁。
				try {
					var dispatch = queue.submit(task);
					if (dispatch != null)
						dispatches.add(dispatch);
				} finally {
					for (var i = 0; i < sum; ++i)
						queue.unlock();
				}
			}
		} catch (Throwable ex) {
			failure = ex;
		}
		if (failure != null)
			cancelBarrierAfterFailure(barrier, failure);
		// 入队中途失败也必须驱动此前认领的桶，让它们消费已经取消的屏障。
		for (var dispatch : dispatches) {
			try {
				dispatch.run();
			} catch (Throwable ex) {
				if (failure == null)
					failure = ex;
				else if (failure != ex)
					failure.addSuppressed(ex);
				cancelBarrierAfterFailure(barrier, failure);
			}
		}
		if (failure != null)
			throw Task.forceThrow(failure);
	}

	private static void cancelBarrierAfterFailure(TaskOneByOneQueue.Barrier barrier, Throwable failure) {
		try {
			barrier.cancel();
		} catch (Throwable cleanup) {
			if (cleanup != failure)
				failure.addSuppressed(cleanup);
		}
	}

	public static class Batch<T> {
		private final AtomicInteger keysCount;
		private final Action1<T> action;
		private final Action0 batchEnd;

		public Batch(int keysSize, Action1<T> action, Action0 batchEnd) {
			this.keysCount = new AtomicInteger(keysSize);
			this.action = action;
			this.batchEnd = batchEnd;
		}

		/** 每key一次性核销：正常完成（run的finally）、队列侧丢弃（TaskBodyTask的cancel，覆盖
		 * submit的isShutdown静默丢、rollbackRejectedDispatch清队、shutdown(cancel)）、提交失败
		 * （executeBatch的catch）可能对同一key触发，CAS保证恰好核销一次。 */
		public final class Settle implements Action0 {
			private final AtomicBoolean settled = new AtomicBoolean();

			@Override
			public void run() {
				if (!settled.compareAndSet(false, true))
					return;
				if (keysCount.decrementAndGet() == 0)
					runBatchEndDirect(batchEnd); // batchEnd 的异常策略只有这一处实现，不能在这里裸调 batchEnd.run()。
			}
		}

		public Settle newSettle() {
			return new Settle();
		}

		public void run(T key, Settle settle) throws Exception {
			try {
				action.run(key);
			} finally {
				settle.run();
			}
		}

		/** 提交循环中断时，未尝试key的一次性核销。 */
		public void abandon(int count) {
			if (count > 0 && keysCount.addAndGet(-count) == 0)
				runBatchEndDirect(batchEnd);
		}
	}

	private static void runBatchEndDirect(Action0 batchEnd) {
		try {
			batchEnd.run();
		} catch (Throwable e) { // logger.error
			logger.error("executeBatch: batchEnd exception", e);
		}
	}

	public <T> void executeBatch(Collection<T> keys, Action1<T> action, Action0 batchEnd,
								 @Nullable DispatchMode mode) {
		if (keys.isEmpty()) {
			runBatchEndDirect(batchEnd);
			return;
		}
		var batch = new Batch<>(keys.size(), action, batchEnd);
		var remaining = keys.size();
		for (var key : keys) {
			remaining--;
			var settle = batch.newSettle();
			try {
				// cancel=settle：队列侧丢弃任务的三条路径都会执行task.cancel核销该key计数。
				execute(key, new TaskOneByOneQueue.TaskBodyTask(
						new TaskBody.OfAction(() -> batch.run(key, settle)), null, settle, mode));
			} catch (RuntimeException ex) {
				// 提交失败：本key未入队或已被回滚清队核销（幂等防双重），未尝试的key
				// 一次性核销——batchEnd不再永久悬挂。
				settle.run();
				batch.abandon(remaining);
				throw ex;
			}
		}
	}

	public void executeBatch(LongList keys, Action1<Long> action, Action0 batchEnd,
							 @Nullable DispatchMode mode) {
		if (keys.isEmpty()) {
			runBatchEndDirect(batchEnd);
			return;
		}
		var batch = new Batch<>(keys.size(), action, batchEnd);
		final var remaining = new int[]{keys.size()};
		keys.foreach((key) -> {
			remaining[0]--;
			var settle = batch.newSettle();
			try {
				execute(key, new TaskOneByOneQueue.TaskBodyTask(
						new TaskBody.OfAction(() -> batch.run(key, settle)), null, settle, mode));
			} catch (RuntimeException ex) {
				settle.run();
				batch.abandon(remaining[0]);
				throw ex;
			}
		});
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Action0 action) {
		TaskSpec.ofAction(action).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Action0 action, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Action0 action, @Nullable String name) {
		TaskSpec.ofAction(action).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Action0 action, @Nullable String name,
						@Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Action0 action, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, FuncLong func) {
		TaskSpec.ofFunc(func).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, FuncLong func, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, FuncLong func, @Nullable String name) {
		TaskSpec.ofFunc(func).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, FuncLong func, @Nullable String name,
						@Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, FuncLong func, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Procedure procedure) {
		TaskSpec.ofProcedure(procedure).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Procedure procedure, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(Object key, Procedure procedure, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Action0 action) {
		TaskSpec.ofAction(action).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Action0 action, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Action0 action, @Nullable String name) {
		TaskSpec.ofAction(action).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Action0 action, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	protected abstract TaskOneByOneQueue getAndLockQueue(Object key);

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, FuncLong func) {
		TaskSpec.ofFunc(func).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, FuncLong func, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, FuncLong func, @Nullable String name) {
		TaskSpec.ofFunc(func).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, FuncLong func, @Nullable String name, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	// 为了避免装箱,这里区分出类型,子类需要优化的时候重载.
	protected void execute(int key, TaskOneByOneQueue.Task task) {
		executeAndUnlock(getAndLockQueue(key), task);
	}

	// 为了避免装箱,这里区分出类型,子类需要优化的时候重载.
	protected void execute(long key, TaskOneByOneQueue.Task task) {
		executeAndUnlock(getAndLockQueue(key), task);
	}

	// 其他类型.
	protected void execute(Object key, TaskOneByOneQueue.Task task) {
		executeAndUnlock(getAndLockQueue(key), task);
	}

	protected static void executeAndUnlock(TaskOneByOneQueue lockedQueue,
										   TaskOneByOneQueue.Task task) {
		Runnable submit;
		try {
			submit = lockedQueue.submit(task);
		} finally {
			lockedQueue.unlock();
		}
		if (submit != null)
			submit.run();
	}

	protected static void executeAndUnlock(TaskOneByOneQueue lockedQueue,
										   TaskOneByOneQueue.Task task, int lockTimes) {
		Runnable submit;
		try {
			submit = lockedQueue.submit(task);
		} finally {
			for (var i = 0; i < lockTimes; ++i)
				lockedQueue.unlock();
		}
		if (submit != null)
			submit.run();
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, FuncLong func, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Procedure procedure) {
		TaskSpec.ofProcedure(procedure).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Procedure procedure, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, Procedure procedure, @Nullable Action0 cancel, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Action0 action) {
		TaskSpec.ofAction(action).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Action0 action, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Action0 action, @Nullable String name) {
		TaskSpec.ofAction(action).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Action0 action, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, FuncLong func) {
		TaskSpec.ofFunc(func).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, FuncLong func, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, FuncLong func, @Nullable String name) {
		TaskSpec.ofFunc(func).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, FuncLong func, @Nullable String name, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, FuncLong func, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Procedure procedure) {
		TaskSpec.ofProcedure(procedure).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Procedure procedure, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, Procedure procedure, @Nullable Action0 cancel, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

}
