package Zeze.Util;

import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
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
			for (var e : group.entrySet()) {
				var sum = e.getValue().value;
				executeAndUnlock(e.getKey(), new TaskOneByOneQueue.TaskBarrierProcedure(barrier, sum, mode), sum);
			}
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
			for (var e : group.entrySet()) {
				var sum = e.getValue().value;
				executeAndUnlock(e.getKey(), new TaskOneByOneQueue.TaskBarrierAction(barrier, sum, mode), sum);
			}
		} finally {
			unlock();
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

		public void run(@NotNull T key) throws Exception {
			try {
				action.run(key);
			} finally {
				if (keysCount.decrementAndGet() == 0)
					runBatchEndDirect(batchEnd); // batchEnd 的异常策略只有这一处实现，不能在这里裸调 batchEnd.run()。
			}
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
		for (var key : keys)
			execute(key, new TaskOneByOneQueue.TaskBodyTask(new TaskBody.OfAction(() -> batch.run(key)), null, null, mode));
	}

	public void executeBatch(@NotNull LongList keys, @NotNull Action1<Long> action, @NotNull Action0 batchEnd,
							 @Nullable DispatchMode mode) {
		if (keys.isEmpty()) {
			runBatchEndDirect(batchEnd);
			return;
		}
		var batch = new Batch<>(keys.size(), action, batchEnd);
		keys.foreach((key) -> execute(key, new TaskOneByOneQueue.TaskBodyTask(new TaskBody.OfAction(() -> batch.run(key)), null, null, mode)));
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
