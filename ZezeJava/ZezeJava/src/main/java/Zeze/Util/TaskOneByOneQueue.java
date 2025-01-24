package Zeze.Util;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TaskOneByOneQueue extends ReentrantLock {
	private static final @NotNull Logger logger = LogManager.getLogger(TaskOneByOneQueue.class);
	private final @NotNull Condition cond = newCondition();
	private final BatchTask batch = new BatchTask();
	private @NotNull ArrayDeque<Task> queue = new ArrayDeque<>();
	private final @Nullable Executor executor;
	private boolean isShutdown;
	private boolean removed;

	void setRemoved() {
		removed = true;
	}

	public boolean isRemoved() {
		return removed;
	}

	public TaskOneByOneQueue(@Nullable Executor executor) {
		this.executor = executor;
	}

	public int sizeUnderLock() {
		return queue.size();
	}

	public int size() {
		lock();
		try {
			return queue.size();
		} finally {
			unlock();
		}
	}

	public final class BatchTask implements Runnable {
		Task[] tasks;
		int count;
		@Nullable DispatchMode mode;
		int processedCount;

		public void prepare() {
			if (!queue.isEmpty()) {
				var max = Math.min(queue.size(), 1000);
				if (tasks == null || max > tasks.length)
					tasks = new Task[max];
				mode = queue.peekFirst().mode;
				var i = 0;
				for (var task : queue) {
					if (mode != task.mode)
						break;
					tasks[i++] = task;
					if (i >= max || task.isBarrier()) // barrier任务大多会中断批量任务,所以遇到这种任务就不再加后续任务了,能提高点性能
						break;
				}
				count = i;
			} else {
				mode = DispatchMode.Normal;
				count = 0;
			}
		}

		@Override
		public void run() {
			for (processedCount = 0; processedCount < count; ) {
				var task = tasks[processedCount];
				tasks[processedCount++] = null; // gc, 下标索引转换成count。
				if (!task.process(this))
					return; // 任务调度终端，当前任务以后完成的时候会触发runNext;
			}
			TaskOneByOneQueue.this.runNext(processedCount);
		}

		public void runNext() {
			TaskOneByOneQueue.this.runNext(processedCount);
		}
	}

	/*
	public Runnable submit(@NotNull Action0 action, @Nullable String name, @Nullable Action0 cancel,
						   @Nullable DispatchMode mode) {
		return submit(new TaskAction(action, name, cancel, mode));
	}

	public Runnable submit(@NotNull FuncLong func, @Nullable String name, @Nullable Action0 cancel,
						   @Nullable DispatchMode mode) {
		return submit(new TaskFunc(func, name, cancel, mode));
	}


	public Runnable executeBarrier(@NotNull BarrierProcedure barrier, int sum, @Nullable DispatchMode mode) {
		return submit(new TaskBarrierProcedure(barrier, sum, mode));
	}

	public Runnable executeBarrier(@NotNull BarrierAction barrier, int sum, @Nullable DispatchMode mode) {
		return submit(new TaskBarrierAction(barrier, sum, mode));
	}
	*/

	public Runnable submit(@NotNull Task task) {
		if (!isShutdown) {
			queue.addLast(task);
			if (queue.size() != 1)
				return null; // 有任务正在执行,不需要进一步调度.
			batch.prepare();
			return () -> {
				if (executor != null)
					executor.execute(batch);
				else {
					var threadPool = batch.mode == DispatchMode.Critical
							? Zeze.Util.Task.getCriticalThreadPool()
							: Zeze.Util.Task.getThreadPool();
					threadPool.execute(batch);
				}
			};
		}
		if (task.cancel != null) {
			return () -> {
				try {
					task.cancel.run();
				} catch (Throwable e) { // logger.error
					logger.error("CancelAction={}", task.name, e);
				}
			};
		}
		return null;
	}

	private void runNext(int count) {
		lock();
		try {
			while (count-- > 0)
				queue.pollFirst();
			if (queue.isEmpty()) {
				if (isShutdown)
					cond.signalAll();
				return;
			}
			batch.prepare();
		} finally {
			unlock();
		}
		if (executor != null) {
			executor.execute(batch);
		} else {
			var threadPool = batch.mode == DispatchMode.Critical
					? Zeze.Util.Task.getCriticalThreadPool()
					: Zeze.Util.Task.getThreadPool();
			threadPool.execute(batch);
		}
	}

	public void shutdown(boolean cancel) {
		ArrayDeque<Task> oldQueue;
		lock();
		try {
			if (isShutdown)
				return;
			isShutdown = true;
			oldQueue = queue;
			Task firstTask;
			if (!cancel || (firstTask = oldQueue.pollFirst()) == null)
				return;
			queue = new ArrayDeque<>(); // clear
			queue.addLast(firstTask); // put back running task back
		} finally {
			unlock();
		}
		for (Task task : oldQueue) {
			if (task.cancel != null) {
				try {
					task.cancel.run();
				} catch (Throwable e) { // logger.error
					logger.error("CancelAction={}", task.name, e);
				}
			}
		}
	}

	public void waitComplete() throws InterruptedException {
		lock();
		try {
			while (!queue.isEmpty())
				cond.await(); // wait running task
		} finally {
			unlock();
		}
	}

	@Override
	public @NotNull String toString() {
		var sb = new StringBuilder().append('[');
		lock();
		try {
			for (var task : queue)
				sb.append(task.name).append(',');
		} finally {
			unlock();
		}
		int n = sb.length();
		if (n > 1)
			sb.setLength(n - 1);
		return sb.append(']').toString();
	}

	public static abstract class Task {
		final @NotNull String name;
		final @Nullable Action0 cancel;
		final @Nullable DispatchMode mode;

		public Task(@NotNull String name, @Nullable Action0 cancel, @Nullable DispatchMode mode) {
			this.name = name;
			this.cancel = cancel;
			this.mode = mode;
		}

		public abstract boolean isBarrier();

		public abstract boolean process(@NotNull BatchTask batch);
	}

	public static final class TaskAction extends Task {
		private final @NotNull Action0 action;

		public TaskAction(@NotNull Action0 action, @Nullable String name, @Nullable Action0 cancel,
						  @Nullable DispatchMode mode) {
			super(name != null ? name : action.getClass().getName(), cancel, mode);
			this.action = action;
		}

		@Override
		public boolean isBarrier() {
			return false;
		}

		@Override
		public boolean process(@NotNull BatchTask batch) {
			try {
				action.run();
			} catch (Throwable e) { // logger.error
				logger.error("TaskOneByOne: {}", name, e);
			}
			return true;
		}
	}

	public static final class TaskFunc extends Task {
		private final @NotNull FuncLong func;

		public TaskFunc(@NotNull FuncLong func, @Nullable String name, @Nullable Action0 cancel, @Nullable DispatchMode mode) {
			super(name != null ? name : func.getClass().getName(), cancel, mode);
			this.func = func;
		}

		@Override
		public boolean isBarrier() {
			return false;
		}

		@Override
		public boolean process(@NotNull BatchTask batch) {
			try {
				func.call();
			} catch (Throwable e) { // logger.error
				logger.error("TaskOneByOne: {}", name, e);
			}
			return true;
		}
	}

	public static final class TaskBarrierProcedure extends Task {
		private final @NotNull BarrierProcedure barrier;
		private final int sum;

		public TaskBarrierProcedure(@NotNull BarrierProcedure barrier, int sum, @Nullable DispatchMode mode) {
			super(barrier.getName(), barrier::cancel, mode);
			this.barrier = barrier;
			this.sum = sum;
		}

		@Override
		public boolean isBarrier() {
			return true;
		}

		@Override
		public boolean process(@NotNull BatchTask batch) {
			return barrier.reach(batch, sum);
		}
	}

	public static final class TaskBarrierAction extends Task {
		private final @NotNull BarrierAction barrier;
		private final int sum;

		public TaskBarrierAction(@NotNull BarrierAction barrier, int sum, @Nullable DispatchMode mode) {
			super(barrier.actionName, barrier::cancel, mode);
			this.barrier = barrier;
			this.sum = sum;
		}

		@Override
		public boolean isBarrier() {
			return true;
		}

		@Override
		public boolean process(@NotNull BatchTask batch) {
			return barrier.reach(batch, sum);
		}
	}

	public static abstract class Barrier extends ReentrantLock {
		private final HashSet<BatchTask> reached = new HashSet<>();
		private final @Nullable Action0 cancelAction;
		private int count;
		private boolean canceled;

		public Barrier(int count, @Nullable Action0 cancelAction) {
			this.cancelAction = cancelAction;
			this.count = count;
		}

		public abstract @NotNull String getName();

		public abstract void run() throws Exception;

		private void reachedRunNext() {
			for (var batch : reached)
				batch.runNext();
		}

		public boolean reach(@NotNull BatchTask batch, int sum) {
			lock();
			try {
				if (canceled)
					return true;

				reached.add(batch);

				count -= sum;
				if (count > 0)
					return false;

				try {
					run();
				} catch (Throwable ex) { // logger.error
					logger.error("{} run exception", getName(), ex);
				} finally {
					// 成功执行
					// 1. 触发所有桶的runNext，
					// 2. 自己也返回false，不再继续runNext。
					reachedRunNext();
				}
				return false; // 返回false
			} finally {
				unlock();
			}
		}

		public void cancel() {
			lock();
			try {
				if (canceled)
					return;

				canceled = true;
				try {
					if (cancelAction != null)
						cancelAction.run();
				} catch (Throwable ex) { // logger.error
					logger.error("{} cancel exception", getName(), ex);
				} finally {
					// 取消的时候，
					// 1. 如果相关桶的任务已经执行，需要runNext。
					// 2. 如果相关桶的任务没有执行，不需要处理。相应的任务以后会发现已经取消，自动忽略执行。
					reachedRunNext();
				}
			} finally {
				unlock();
			}
		}
	}

	public static final class BarrierProcedure extends Barrier {
		private final @NotNull Procedure procedure;

		public BarrierProcedure(@NotNull Procedure procedure, int count, @Nullable Action0 cancelAction) {
			super(count, cancelAction);
			this.procedure = procedure;
		}

		@Override
		public @NotNull String getName() {
			return procedure.getActionName();
		}

		@Override
		public void run() {
			procedure.call();
		}
	}

	public static final class BarrierAction extends Barrier {
		private final @NotNull Action0 action;
		private final @NotNull String actionName;

		public BarrierAction(@NotNull String actionName, @NotNull Action0 action, int count, @Nullable Action0 cancelAction) {
			super(count, cancelAction);
			this.action = action;
			this.actionName = actionName;
		}

		@Override
		public @NotNull String getName() {
			return actionName;
		}

		@Override
		public void run() throws Exception {
			action.run();
		}
	}
}
