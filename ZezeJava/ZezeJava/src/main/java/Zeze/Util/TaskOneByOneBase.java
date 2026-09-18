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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TaskOneByOneBase extends ReentrantLock {
	private static final @NotNull Logger logger = LogManager.getLogger(TaskOneByOneBase.class);

	public <T extends Comparable<T>> void executeCyclicBarrier(@NotNull List<T> keys, @NotNull Procedure procedure,
															   @Nullable Action0 cancel, @Nullable DispatchMode mode) {
		lock();
		try {
			if (keys.isEmpty())
				throw new IllegalArgumentException("CyclicBarrier keys is empty.");

			keys.sort(Comparable::compareTo);
			var group = new HashMap<TaskOneByOneQueue, OutInt>();
			int count = 0;
			for (var key : keys) {
				group.computeIfAbsent(getAndLockQueue(key), __ -> new OutInt()).value++;
				count++;
			}
			var barrier = new TaskOneByOneQueue.BarrierProcedure(procedure, count, cancel);
			submitBarrierAndUnlock(group, barrier, (sum) -> new TaskOneByOneQueue.TaskBarrierProcedure(barrier, sum, mode));
		} finally {
			unlock();
		}
	}

	public <T extends Comparable<T>> void executeCyclicBarrier(@NotNull List<T> keys, @NotNull String actionName,
															   @NotNull Action0 action, @Nullable Action0 cancel,
															   @Nullable DispatchMode mode) {
		lock();
		try {
			if (keys.isEmpty())
				throw new IllegalArgumentException("CyclicBarrier keys is empty.");

			keys.sort(Comparable::compareTo);
			var group = new HashMap<TaskOneByOneQueue, OutInt>();
			int count = 0;
			for (var key : keys) {
				group.computeIfAbsent(getAndLockQueue(key), __ -> new OutInt()).value++;
				count++;
			}
			var barrier = new TaskOneByOneQueue.BarrierAction(actionName, action, count, cancel);
			submitBarrierAndUnlock(group, barrier, (sum) -> new TaskOneByOneQueue.TaskBarrierAction(barrier, sum, mode));
		} finally {
			unlock();
		}
	}

	/** 逐桶提交并解锁。submit/派发抛出（池未初始化ISE、自定义executor拒绝REE）时，
	 * 当前桶的锁已由 executeAndUnlock 的 finally 解开，但剩余桶的队列锁不能跟着异常
	 * 一起泄漏——补解锁后取消屏障（幂等；已提交桶的 barrier 任务 count 永不归零，
	 * 队列非空且再无派发点，必须由 cancel 的 runNext 收尾），最后重抛首个异常。 */
	private static void submitBarrierAndUnlock(@NotNull HashMap<TaskOneByOneQueue, OutInt> group,
											   @NotNull TaskOneByOneQueue.Barrier barrier,
											   @NotNull IntFunction<TaskOneByOneQueue.Task> newTask) {
		var buckets = new ArrayList<>(group.entrySet());
		for (var i = 0; i < buckets.size(); ++i) {
			var sum = buckets.get(i).getValue().value;
			try {
				executeAndUnlock(buckets.get(i).getKey(), newTask.apply(sum), sum);
			} catch (RuntimeException ex) {
				for (var j = i + 1; j < buckets.size(); ++j) {
					var rest = buckets.get(j);
					for (var k = 0; k < rest.getValue().value; ++k)
						rest.getKey().unlock();
				}
				barrier.cancel();
				throw ex;
			}
		}
	}

	public static class Batch<T> {
		private final @NotNull AtomicInteger keysCount;
		private final @NotNull Action1<T> action;
		private final @NotNull Action0 batchEnd;

		public Batch(int keysSize, @NotNull Action1<T> action, @NotNull Action0 batchEnd) {
			this.keysCount = new AtomicInteger(keysSize);
			this.action = action;
			this.batchEnd = batchEnd;
		}

		/** 每key一次性核销：正常完成（run的finally）、队列侧丢弃（TaskBodyTask的cancel，覆盖
		 * submit的isShutdown静默丢、rollbackRejectedDispatch清队、shutdown(cancel)）、提交失败
		 * （executeBatch的catch）可能对同一key触发，CAS保证恰好核销一次。 */
		public final class Settle implements Action0 {
			private final @NotNull AtomicBoolean settled = new AtomicBoolean();

			@Override
			public void run() {
				if (!settled.compareAndSet(false, true))
					return;
				if (keysCount.decrementAndGet() == 0)
					runBatchEndDirect(batchEnd); // batchEnd 的异常策略只有这一处实现，不能在这里裸调 batchEnd.run()。
			}
		}

		public @NotNull Settle newSettle() {
			return new Settle();
		}

		public void run(@NotNull T key, @NotNull Settle settle) throws Exception {
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

	private static void runBatchEndDirect(@NotNull Action0 batchEnd) {
		try {
			batchEnd.run();
		} catch (Throwable e) { // logger.error
			logger.error("executeBatch: batchEnd exception", e);
		}
	}

	public <T> void executeBatch(@NotNull Collection<T> keys, @NotNull Action1<T> action, @NotNull Action0 batchEnd,
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

	public void executeBatch(@NotNull LongList keys, @NotNull Action1<Long> action, @NotNull Action0 batchEnd,
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
	public void Execute(@NotNull Object key, @NotNull Action0 action) {
		TaskSpec.ofAction(action).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable String name) {
		TaskSpec.ofAction(action).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable String name,
						@Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull FuncLong func) {
		TaskSpec.ofFunc(func).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull FuncLong func, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull FuncLong func, @Nullable String name) {
		TaskSpec.ofFunc(func).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull FuncLong func, @Nullable String name,
						@Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull FuncLong func, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Procedure procedure) {
		TaskSpec.ofProcedure(procedure).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Procedure procedure, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(@NotNull Object key, @NotNull Procedure procedure, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, @NotNull Action0 action) {
		TaskSpec.ofAction(action).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, @NotNull Action0 action, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, @NotNull Action0 action, @Nullable String name) {
		TaskSpec.ofAction(action).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, @NotNull Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, @NotNull Action0 action, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	protected abstract @NotNull TaskOneByOneQueue getAndLockQueue(@NotNull Object key);

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, @NotNull FuncLong func) {
		TaskSpec.ofFunc(func).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, @NotNull FuncLong func, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, @NotNull FuncLong func, @Nullable String name) {
		TaskSpec.ofFunc(func).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, @NotNull FuncLong func, @Nullable String name, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	// 为了避免装箱,这里区分出类型,子类需要优化的时候重载.
	protected void execute(int key, @NotNull TaskOneByOneQueue.Task task) {
		executeAndUnlock(getAndLockQueue(key), task);
	}

	// 为了避免装箱,这里区分出类型,子类需要优化的时候重载.
	protected void execute(long key, @NotNull TaskOneByOneQueue.Task task) {
		executeAndUnlock(getAndLockQueue(key), task);
	}

	// 其他类型.
	protected void execute(@NotNull Object key, @NotNull TaskOneByOneQueue.Task task) {
		executeAndUnlock(getAndLockQueue(key), task);
	}

	protected static void executeAndUnlock(@NotNull TaskOneByOneQueue lockedQueue,
										   @NotNull TaskOneByOneQueue.Task task) {
		Runnable submit;
		try {
			submit = lockedQueue.submit(task);
		} finally {
			lockedQueue.unlock();
		}
		if (submit != null)
			submit.run();
	}

	protected static void executeAndUnlock(@NotNull TaskOneByOneQueue lockedQueue,
										   @NotNull TaskOneByOneQueue.Task task, int lockTimes) {
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
	public void Execute(int key, @NotNull FuncLong func, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, @NotNull Procedure procedure) {
		TaskSpec.ofProcedure(procedure).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, @NotNull Procedure procedure, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(int key, @NotNull Procedure procedure, @Nullable Action0 cancel, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, @NotNull Action0 action) {
		TaskSpec.ofAction(action).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, @NotNull Action0 action, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, @NotNull Action0 action, @Nullable String name) {
		TaskSpec.ofAction(action).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, @NotNull Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofAction(action).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, @NotNull Action0 action, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofAction(action).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, @NotNull FuncLong func) {
		TaskSpec.ofFunc(func).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, @NotNull FuncLong func, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, @NotNull FuncLong func, @Nullable String name) {
		TaskSpec.ofFunc(func).name(name).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, @NotNull FuncLong func, @Nullable String name, @Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).name(name).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofFunc(func).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, @NotNull FuncLong func, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		TaskSpec.ofFunc(func).name(name).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, @NotNull Procedure procedure) {
		TaskSpec.ofProcedure(procedure).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, @NotNull Procedure procedure, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).dispatchMode(mode).executeOneByOne(key, this);
	}

	/** @deprecated 请使用 {@code TaskSpec.ofProcedure(procedure).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this)}。 */
	@Deprecated
	public void Execute(long key, @NotNull Procedure procedure, @Nullable Action0 cancel, @Nullable DispatchMode mode) {
		TaskSpec.ofProcedure(procedure).onCancel(cancel).dispatchMode(mode).executeOneByOne(key, this);
	}

}
