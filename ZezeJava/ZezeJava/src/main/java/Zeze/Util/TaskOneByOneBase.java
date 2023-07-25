package Zeze.Util;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TaskOneByOneBase {
	private static void executeAndUnlock(TaskOneByOneQueue queue, TaskOneByOneQueue.Task task) {
		Runnable job;
		try {
			job = queue.submit(task);
		} finally {
			queue.unlockAllHoldCount();
		}
		if (null != job)
			job.run();
	}

	public synchronized <T extends Comparable<T>> void executeCyclicBarrier(@NotNull List<T> keys, @NotNull Procedure procedure,
													  @Nullable Action0 cancel, @Nullable DispatchMode mode) {
		if (keys.isEmpty())
			throw new IllegalArgumentException("CyclicBarrier keys is empty.");

		keys.sort(new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return o1.compareTo(o2);
			}
		});
		var group = new HashMap<TaskOneByOneQueue, OutInt>();
		int count = 0;
		for (var key : keys) {
			group.computeIfAbsent(getAndLockQueue(key), __ -> new OutInt()).value++;
			count++;
		}
		var barrier = new TaskOneByOneQueue.BarrierProcedure(procedure, count, cancel);
		for (var e : group.entrySet()) {
			var sum = e.getValue().value;
			// todo 边提交边解锁. 需要确认.
			executeAndUnlock(e.getKey(), new TaskOneByOneQueue.TaskBarrierProcedure(barrier, sum, mode));
		}
	}

	public synchronized <T extends Comparable<T>> void executeCyclicBarrier(@NotNull List<T> keys, @NotNull String actionName,
													  @NotNull Action0 action, @Nullable Action0 cancel,
													  @Nullable DispatchMode mode) {
		if (keys.isEmpty())
			throw new IllegalArgumentException("CyclicBarrier keys is empty.");

		keys.sort(new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				return o1.compareTo(o2);
			}
		});

		var group = new HashMap<TaskOneByOneQueue, OutInt>();
		int count = 0;
		for (var key : keys) {
			group.computeIfAbsent(getAndLockQueue(key), __ -> new OutInt()).value++;
			count++;
		}
		var barrier = new TaskOneByOneQueue.BarrierAction(actionName, action, count, cancel);
		for (var e : group.entrySet()) {
			var sum = e.getValue().value;
			executeAndUnlock(e.getKey(), new TaskOneByOneQueue.TaskBarrierAction(barrier, sum, mode));
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
			Execute(key, () -> batch.run(key), mode);
	}

	public void executeBatch(@NotNull LongList keys, @NotNull Action1<Long> action, @NotNull Action0 batchEnd,
							 @Nullable DispatchMode mode) {
		var batch = new Batch<>(keys.size(), action, batchEnd);
		keys.foreach((key) -> Execute(key, () -> batch.run(key), mode));
	}

	public void Execute(@NotNull Object key, @NotNull Action0 action) {
		Execute(key.hashCode(), action, DispatchMode.Normal);
	}

	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable DispatchMode mode) {
		Execute(key.hashCode(), action, mode);
	}

	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable String name) {
		Execute(key.hashCode(), action, name, DispatchMode.Normal);
	}

	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable String name,
						@Nullable DispatchMode mode) {
		Execute(key.hashCode(), action, name, mode);
	}

	public void Execute(@NotNull Object key, @NotNull Action0 action, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		Execute(key.hashCode(), action, name, cancel, mode);
	}

	public void Execute(@NotNull Object key, @NotNull FuncLong func) {
		Execute(key.hashCode(), func, DispatchMode.Normal);
	}

	public void Execute(@NotNull Object key, @NotNull FuncLong func, @Nullable DispatchMode mode) {
		Execute(key.hashCode(), func, mode);
	}

	public void Execute(@NotNull Object key, @NotNull FuncLong func, @Nullable String name) {
		Execute(key.hashCode(), func, name, DispatchMode.Normal);
	}

	public void Execute(@NotNull Object key, @NotNull FuncLong func, @Nullable String name,
						@Nullable DispatchMode mode) {
		Execute(key.hashCode(), func, name, mode);
	}

	public void Execute(@NotNull Object key, @NotNull FuncLong func, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		Execute(key.hashCode(), func, name, cancel, mode);
	}

	public void Execute(@NotNull Object key, @NotNull Procedure procedure) {
		Execute(key.hashCode(), procedure, DispatchMode.Normal);
	}

	public void Execute(@NotNull Object key, @NotNull Procedure procedure, @Nullable DispatchMode mode) {
		Execute(key.hashCode(), procedure, mode);
	}

	public void Execute(@NotNull Object key, @NotNull Procedure procedure, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		Execute(key.hashCode(), procedure, cancel, mode);
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

	private void execute(int key, TaskOneByOneQueue.Task task) {
		TaskOneByOneQueue queue = getAndLockQueue(key);
		Runnable submit;
		try {
			submit = queue.submit(task);
		} finally {
			queue.unlockAllHoldCount();
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
		Execute(Long.hashCode(key), action, DispatchMode.Normal);
	}

	public void Execute(long key, @NotNull Action0 action, @Nullable DispatchMode mode) {
		Execute(Long.hashCode(key), action, mode);
	}

	public void Execute(long key, @NotNull Action0 action, @Nullable String name) {
		Execute(Long.hashCode(key), action, name, DispatchMode.Normal);
	}

	public void Execute(long key, @NotNull Action0 action, @Nullable String name, @Nullable DispatchMode mode) {
		Execute(Long.hashCode(key), action, name, mode);
	}

	public void Execute(long key, @NotNull Action0 action, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		Execute(Long.hashCode(key), action, name, cancel, mode);
	}

	public void Execute(long key, @NotNull FuncLong func) {
		Execute(Long.hashCode(key), func, DispatchMode.Normal);
	}

	public void Execute(long key, @NotNull FuncLong func, @Nullable DispatchMode mode) {
		Execute(Long.hashCode(key), func, mode);
	}

	public void Execute(long key, @NotNull FuncLong func, @Nullable String name) {
		Execute(Long.hashCode(key), func, name, DispatchMode.Normal);
	}

	public void Execute(long key, @NotNull FuncLong func, @Nullable String name, @Nullable DispatchMode mode) {
		Execute(Long.hashCode(key), func, name, mode);
	}

	public void Execute(long key, @NotNull FuncLong func, @Nullable String name, @Nullable Action0 cancel,
						@Nullable DispatchMode mode) {
		Execute(Long.hashCode(key), func, name, cancel, mode);
	}

	public void Execute(long key, @NotNull Procedure procedure) {
		Execute(Long.hashCode(key), procedure, DispatchMode.Normal);
	}

	public void Execute(long key, @NotNull Procedure procedure, @Nullable DispatchMode mode) {
		Execute(Long.hashCode(key), procedure, mode);
	}

	public void Execute(long key, @NotNull Procedure procedure, @Nullable Action0 cancel, @Nullable DispatchMode mode) {
		Execute(Long.hashCode(key), procedure, cancel, mode);
	}

}
