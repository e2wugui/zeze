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
	private volatile boolean isShutdown;
	private boolean shutdownCancel; // shutdown(cancel) 的模式，runNext 收尾判断用；锁内读写
	private boolean pendingCancels; // 存在锁外正在执行的收尾补偿；锁内读写
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
				if (tasks == null || max > tasks.length || max < tasks.length / 2)
					// 增长：批量变大时扩容；收缩(需求小于现容量一半)：空闲后释放峰值内存，避免长期驻留。
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
				if (isShutdown)
					// shutdown后批量内后续任务不再连续执行：shutdown(true)时队列剩余(均未运行)由runNext收尾补偿；
					// shutdown(false)时任务仍在队列，由下面的runNext重新prepare逐个调度执行。
					break;
			}
			TaskOneByOneQueue.this.runNext(processedCount);
		}

		public void runNext() {
			TaskOneByOneQueue.this.runNext(processedCount);
		}
	}

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
		ArrayDeque<Task> cancels = null;
		lock();
		try {
			while (count-- > 0)
				queue.pollFirst();
			if (queue.isEmpty()) {
				if (isShutdown)
					cond.signalAll();
				return;
			}
			if (isShutdown && shutdownCancel) {
				// shutdown(true)收尾：此时队列剩余的任务(在飞批量认领区内未执行的部分+认领后新提交的部分)
				// 均未运行，逐个补偿。补偿回调可能触发其他桶的runNext，必须在锁外执行；
				// 期间pendingCancels挡住waitComplete，保证shutdown等待者观察到补偿已全部执行。
				cancels = queue;
				queue = new ArrayDeque<>();
				pendingCancels = true;
			} else
				batch.prepare();
		} finally {
			unlock();
		}
		if (cancels != null) {
			runCancel(cancels);
			lock();
			try {
				pendingCancels = false;
				cond.signalAll();
			} finally {
				unlock();
			}
			return;
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

	private static void runCancel(@NotNull ArrayDeque<Task> tasks) {
		for (Task task; (task = tasks.pollFirst()) != null; ) {
			if (task.cancel != null) {
				try {
					task.cancel.run();
				} catch (Throwable e) { // logger.error
					logger.error("CancelAction={}", task.name, e);
				}
			}
		}
	}

	public void shutdown(boolean cancel) {
		ArrayDeque<Task> oldQueue = null;
		lock();
		try {
			if (isShutdown)
				return;
			isShutdown = true;
			shutdownCancel = cancel;
			if (!cancel)
				return;
			// 补偿边界：只取消"尚未被在飞批量认领"的任务。认领区(队头batch.count个，含已完成未出队与
			// 正在执行的)必须保留——对已执行成功的任务跑onCancel会造成重复发货/重复扣款类二次处理。
			// batch.count的写入(prepare)全部发生在queue锁内(submit的0->1转变与runNext)，锁内读取可见。
			// 保留的认领区由runNext按processedCount对齐出队，未执行部分在那里收尾补偿。
			oldQueue = queue;
			queue = new ArrayDeque<>(); // clear
			int keep = Math.min(batch.count, oldQueue.size());
			for (int i = 0; i < keep; i++)
				queue.addLast(oldQueue.pollFirst());
			if (oldQueue.isEmpty())
				return;
		} finally {
			unlock();
		}
		runCancel(oldQueue); // 未认领的任务：未运行，立即补偿
	}

	public void waitComplete() throws InterruptedException {
		lock();
		try {
			while (!queue.isEmpty() || pendingCancels)
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

	/**
	 * 统一载荷任务：用 {@link TaskBody} 一份代码覆盖 action/func/procedure/func0 四种载荷。
	 * 队列语义无返回值消费者，callRaw 的结果丢弃；名字解析见 {@link TaskBody#logName}。
	 */
	public static final class TaskBodyTask extends Task {
		private final @NotNull TaskBody<?> body;

		public TaskBodyTask(@NotNull TaskBody<?> body, @Nullable String name, @Nullable Action0 cancel,
		                    @Nullable DispatchMode mode) {
			super(body.logName(name), cancel, mode);
			this.body = body;
		}

		@Override
		public boolean isBarrier() {
			return false;
		}

		@Override
		public boolean process(@NotNull BatchTask batch) {
			try {
				body.callRaw();
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
