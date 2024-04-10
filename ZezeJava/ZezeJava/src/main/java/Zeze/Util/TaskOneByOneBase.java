package Zeze.Util;

import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TaskOneByOneBase extends ReentrantLock {
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
			action.run(key);
			if (keysCount.decrementAndGet() == 0)
				batchEnd.run();
		}
	}

	public <T> void executeBatch(@NotNull Collection<T> keys, @NotNull Action1<T> action, @NotNull Action0 batchEnd,
								 @Nullable DispatchMode mode) {
		var batch = new Batch<>(keys.size(), action, batchEnd);
		for (var key : keys)
			Execute(key, () -> batch.run(key), null, null, mode);
	}

	public void executeBatch(@NotNull LongList keys, @NotNull Action1<Long> action, @NotNull Action0 batchEnd,
							 @Nullable DispatchMode mode) {
		var batch = new Batch<>(keys.size(), action, batchEnd);
		keys.foreach((key) -> Execute(key, () -> batch.run(key), null, null, mode));
	}

	public void Execute(@NotNull Object key, @NotNull Action0 action) {
		Execute(key, action, null, null, DispatchMode.Normal);
	}

	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable DispatchMode mode) {
		Execute(key, action, null, null, mode);
	}

	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable String name) {
		Execute(key, action, name, null, DispatchMode.Normal);
	}

	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable String name,
						@Nullable DispatchMode mode) {
		Execute(key, action, name, null, mode);
	}

	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		execute(key, new TaskOneByOneQueue.TaskAction(action, name, cancel, mode));
	}

	public void Execute(@NotNull Object key, @NotNull FuncLong func) {
		Execute(key, func, null, null, DispatchMode.Normal);
	}

	public void Execute(@NotNull Object key, @NotNull FuncLong func, @Nullable DispatchMode mode) {
		Execute(key, func, null, null, mode);
	}

	public void Execute(@NotNull Object key, @NotNull FuncLong func, @Nullable String name) {
		Execute(key, func, name, null, DispatchMode.Normal);
	}

	public void Execute(@NotNull Object key, @NotNull FuncLong func, @Nullable String name,
						@Nullable DispatchMode mode) {
		Execute(key, func, name, null, mode);
	}

	public void Execute(@NotNull Object key, @NotNull FuncLong func, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		execute(key, new TaskOneByOneQueue.TaskFunc(func, name, cancel, mode));
	}

	public void Execute(@NotNull Object key, @NotNull Procedure procedure) {
		Execute(key, procedure, null, DispatchMode.Normal);
	}

	public void Execute(@NotNull Object key, @NotNull Procedure procedure, @Nullable DispatchMode mode) {
		Execute(key, procedure, null, mode);
	}

	public void Execute(@NotNull Object key, @NotNull Procedure procedure, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		execute(key, new TaskOneByOneQueue.TaskFunc(procedure::call, procedure.getActionName(), cancel, mode));
	}

	public void Execute(int key, @NotNull Action0 action) {
		Execute(key, action, null, null, DispatchMode.Normal);
	}

	public void Execute(int key, @NotNull Action0 action, @Nullable DispatchMode mode) {
		Execute(key, action, null, null, mode);
	}

	public void Execute(int key, @NotNull Action0 action, @Nullable String name) {
		Execute(key, action, name, null, DispatchMode.Normal);
	}

	public void Execute(int key, @NotNull Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		Execute(key, action, name, null, mode);
	}

	public void Execute(int key, @NotNull Action0 action, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		//noinspection ConstantValue
		if (action == null)
			throw new IllegalArgumentException("null action");
		execute(key, new TaskOneByOneQueue.TaskAction(action, name, cancel, mode));
	}

	protected abstract @NotNull TaskOneByOneQueue getAndLockQueue(@NotNull Object key);

	public void Execute(int key, @NotNull FuncLong func) {
		Execute(key, func, null, null, DispatchMode.Normal);
	}

	public void Execute(int key, @NotNull FuncLong func, @Nullable DispatchMode mode) {
		Execute(key, func, null, null, mode);
	}

	public void Execute(int key, @NotNull FuncLong func, @Nullable String name) {
		Execute(key, func, name, null, DispatchMode.Normal);
	}

	public void Execute(int key, @NotNull FuncLong func, @Nullable String name, @Nullable DispatchMode mode) {
		Execute(key, func, name, null, mode);
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

	protected static void executeAndUnlock(TaskOneByOneQueue lockedQueue, TaskOneByOneQueue.Task task) {
		Runnable submit;
		try {
			submit = lockedQueue.submit(task);
		} finally {
			lockedQueue.unlock();
		}
		if (submit != null)
			submit.run();
	}

	protected static void executeAndUnlock(TaskOneByOneQueue lockedQueue, TaskOneByOneQueue.Task task, int lockTimes) {
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

	public void Execute(int key, @NotNull FuncLong func, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		//noinspection ConstantValue
		if (func == null)
			throw new IllegalArgumentException("null func");
		execute(key, new TaskOneByOneQueue.TaskFunc(func, name, cancel, mode));
	}

	public void Execute(int key, @NotNull Procedure procedure) {
		Execute(key, procedure, null, DispatchMode.Normal);
	}

	public void Execute(int key, @NotNull Procedure procedure, @Nullable DispatchMode mode) {
		Execute(key, procedure, null, mode);
	}

	public void Execute(int key, @NotNull Procedure procedure, @Nullable Action0 cancel, @Nullable DispatchMode mode) {
		execute(key, new TaskOneByOneQueue.TaskFunc(procedure::call, procedure.getActionName(), cancel, mode));
	}

	public void Execute(long key, @NotNull Action0 action) {
		Execute(key, action, null, null, DispatchMode.Normal);
	}

	public void Execute(long key, @NotNull Action0 action, @Nullable DispatchMode mode) {
		Execute(key, action, null, null, mode);
	}

	public void Execute(long key, @NotNull Action0 action, @Nullable String name) {
		Execute(key, action, name, null, DispatchMode.Normal);
	}

	public void Execute(long key, @NotNull Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		Execute(key, action, name, null, mode);
	}

	public void Execute(long key, @NotNull Action0 action, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		execute(key, new TaskOneByOneQueue.TaskAction(action, name, cancel, mode));
	}

	public void Execute(long key, @NotNull FuncLong func) {
		Execute(key, func, null, null, DispatchMode.Normal);
	}

	public void Execute(long key, @NotNull FuncLong func, @Nullable DispatchMode mode) {
		Execute(key, func, null, null, mode);
	}

	public void Execute(long key, @NotNull FuncLong func, @Nullable String name) {
		Execute(key, func, name, null, DispatchMode.Normal);
	}

	public void Execute(long key, @NotNull FuncLong func, @Nullable String name, @Nullable DispatchMode mode) {
		Execute(key, func, name, null, mode);
	}

	public void Execute(long key, @NotNull FuncLong func, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		execute(key, new TaskOneByOneQueue.TaskFunc(func, name, cancel, mode));
	}

	public void Execute(long key, @NotNull Procedure procedure) {
		Execute(key, procedure, null, DispatchMode.Normal);
	}

	public void Execute(long key, @NotNull Procedure procedure, @Nullable DispatchMode mode) {
		Execute(key, procedure, null, mode);
	}

	public void Execute(long key, @NotNull Procedure procedure, @Nullable Action0 cancel, @Nullable DispatchMode mode) {
		execute(key, new TaskOneByOneQueue.TaskFunc(procedure::call, procedure.getActionName(), cancel, mode));
	}

}
