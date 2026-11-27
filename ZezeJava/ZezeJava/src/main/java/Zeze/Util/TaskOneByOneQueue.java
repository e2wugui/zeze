package Zeze.Util;

import java.util.Objects;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/** 单桶串行任务队列：支持批量调度、屏障任务、shutdown 取消补偿与 waitComplete 等待。 */
public class TaskOneByOneQueue extends ReentrantLock {
	private static final Logger logger = LogManager.getLogger(TaskOneByOneQueue.class);
	private static final ConcurrentHashMap<Dispatch, Executor> pendingDispatches = new ConcurrentHashMap<>();
	private final Condition cond = newCondition();
	private final BatchTask batch = new BatchTask();
	private ArrayDeque<Task> queue = new ArrayDeque<>();
	private final @Nullable Executor executor;
	private volatile boolean isShutdown;
	private boolean shutdownCancel; // shutdown(cancel) 的模式，runNext 收尾判断用；锁内读写
	// 锁外执行中的取消补偿计数（锁内读写），防止多路补偿交错提前放行 waitComplete
	private int pendingCancelCount;
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
		@Nullable Task @Nullable [] tasks;
		int count;
		@Nullable DispatchMode mode;
		int processedCount;

		public void prepare() {
			if (!queue.isEmpty()) {
				var max = Math.min(queue.size(), 1000);
				if (tasks == null || max > tasks.length || max < tasks.length / 2)
					// 批量增大时扩容，需求不足半容时收缩以释放峰值内存
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
				//noinspection DataFlowIssue
				var task = Objects.requireNonNull(tasks[processedCount]);
				tasks[processedCount++] = null; // gc, 下标索引转换成count。
				if (!task.process(this))
					return; // 任务调度终端，当前任务以后完成的时候会触发runNext;
				if (isShutdown)
					// shutdown 后中止当前批量连续执行，交由 runNext 决定收尾补偿或逐个续跑
					break;
			}
			TaskOneByOneQueue.this.runNext(processedCount);
		}

		public void runNext() {
			TaskOneByOneQueue.this.runNext(processedCount);
		}
	}

	private Executor getExecutor(@Nullable DispatchMode mode) {
		return executor != null ? executor
			: Zeze.Util.Task.poolOrThrow(mode == DispatchMode.Critical);
	}

	public @Nullable Runnable submit(Task task) {
		if (!isShutdown) {
			// 入队前校验执行器可用，避免入队后派发失败导致桶永久卡死
			getExecutor(task.mode);

			queue.addLast(task);
			if (queue.size() != 1)
				return null; // 已有任务在执行，无需再次派发
			batch.prepare();
			return () -> executeOrRollback(task.mode, false);
		}
		if (task.cancel != null) {
			return () -> {
				try {
					task.cancel.run();
				} catch (Throwable e) {
					logger.error("CancelAction={}", task.name, e);
				}
			};
		}
		return null;
	}

	/** 锁外派发并在池不可用或拒绝时回滚未启动认领，防止桶永久卡死。 */
	private void executeOrRollback(@Nullable DispatchMode mode, boolean continuation) {
		Dispatch dispatch = null;
		try {
			var target = getExecutor(mode);
			dispatch = new Dispatch(target);
			pendingDispatches.put(dispatch, target);
			dispatch.submit(continuation);
		} catch (Throwable e) {
			// 仅回滚未启动的认领（已内联启动的由 CAS 排除）；清理异常不覆盖原异常
			rollbackFailedDispatch(dispatch, e);
			throw e;
		}
	}

	private void rollbackFailedDispatch(@Nullable Dispatch dispatch, Throwable failure) {
		try {
			if (dispatch == null)
				rollbackRejectedDispatch(failure);
			else
				dispatch.cancelBeforeRun(failure);
		} catch (Throwable cleanup) {
			if (cleanup != failure)
				failure.addSuppressed(cleanup);
		}
	}

	// 每次派发独立认领，不能在复用 BatchTask 上放状态：旧驱动可能迟到，而桶已派发新一批。
	private final class Dispatch extends TaskOneByOneDispatch {
		Dispatch(Executor target) {
			super(TaskOneByOneQueue.this, target);
		}

		@Override
		void execute() {
			try {
				target.execute(this);
			} catch (Throwable e) {
				rollbackFailedDispatch(this, e);
				throw e;
			}
		}

		@Override
		public void run() {
			if (!compareAndSet(0, 1))
				return;
			pendingDispatches.remove(this);
			batch.run();
		}

		void cancelBeforeRun(Throwable cause) {
			if (!compareAndSet(0, 2))
				return;
			pendingDispatches.remove(this);
			rollbackRejectedDispatch(cause);
		}
	}

	static void cancelPendingDispatches(Executor executor) {
		var cause = new RejectedExecutionException("one-by-one dispatch canceled by pool shutdownNow");
		for (var entry : pendingDispatches.entrySet()) {
			if (entry.getValue() == executor)
				entry.getKey().cancelBeforeRun(cause);
		}
	}

	/** 回滚未启动的认领，整队执行取消补偿并唤醒等待者。 */
	private void rollbackRejectedDispatch(Throwable cause) {
		ArrayDeque<Task> cancels;
		int cancelCount;
		lock();
		try {
			if (queue.isEmpty())
				return;
			cancels = queue;
			cancelCount = cancels.size();
			queue = new ArrayDeque<>();
			batch.count = 0; // 认领作废
			if (batch.tasks != null)
				Arrays.fill(batch.tasks, null); // 释放未运行驱动持有的任务引用
			// 递增 pendingCancelCount，确保 waitComplete 在锁外补偿完成后才放行
			pendingCancelCount++;
			cond.signalAll();
		} finally {
			unlock();
		}
		try {
			var names = new StringBuilder();
			for (var task : cancels)
				names.append(task.name).append(',');
			logger.warn("TaskOneByOneQueue: dispatch rejected, rollback & compensate {} task(s): [{}]",
					cancelCount, names, cause);
		} finally {
			runCancelAndSignal(cancels); // 日志故障也不能跳过补偿与 pending 核销
		}
	}

	private void runNext(int count) {
		ArrayDeque<Task> cancels = null;
		lock();
		try {
			while (count-- > 0)
				queue.pollFirst();
			if (queue.isEmpty()) {
				cond.signalAll();
				return;
			}
			if (isShutdown && shutdownCancel) {
				// shutdown(true) 收尾：锁外补偿剩余未执行任务，pendingCancelCount 挡住 waitComplete
				cancels = queue;
				queue = new ArrayDeque<>();
				batch.count = 0;
				if (batch.tasks != null)
					Arrays.fill(batch.tasks, null);
				pendingCancelCount++;
			} else
				batch.prepare();
		} finally {
			unlock();
		}
		if (cancels != null) {
			runCancelAndSignal(cancels);
			return;
		}
		executeOrRollback(batch.mode, true);
	}

	private static void runCancel(ArrayDeque<Task> tasks) {
		for (Task task; (task = tasks.pollFirst()) != null; ) {
			if (task.cancel != null) {
				try {
					task.cancel.run();
				} catch (Throwable e) {
					logger.error("CancelAction={}", task.name, e);
				}
			}
		}
	}

	private void runCancelAndSignal(ArrayDeque<Task> tasks) {
		try {
			runCancel(tasks);
		} finally {
			lock();
			try {
				pendingCancelCount--;
				cond.signalAll();
			} finally {
				unlock();
			}
		}
	}

	public void shutdown(boolean cancel) {
		ArrayDeque<Task> oldQueue;
		lock();
		try {
			if (isShutdown)
				return;
			isShutdown = true;
			shutdownCancel = cancel;
			if (!cancel)
				return;
			// 仅取消未被在飞批量认领的尾部任务；队头已认领区留给 runNext 按 processedCount 精确对齐
			oldQueue = queue;
			queue = new ArrayDeque<>();
			int keep = Math.min(batch.count, oldQueue.size());
			for (int i = 0; i < keep; i++)
				//noinspection DataFlowIssue
				queue.addLast(oldQueue.pollFirst());
			if (oldQueue.isEmpty())
				return;
			pendingCancelCount++; // 摘出队列的补偿也计入 waitComplete 等待范围
		} finally {
			unlock();
		}
		runCancelAndSignal(oldQueue); // 未认领任务立即补偿
	}

	public void waitComplete() throws InterruptedException {
		lock();
		try {
			while (!queue.isEmpty() || pendingCancelCount > 0)
				cond.await(); // 等待运行中任务与取消补偿完成
		} finally {
			unlock();
		}
	}

	@Override
	public String toString() {
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
		final String name;
		final @Nullable Action0 cancel;
		final @Nullable DispatchMode mode;

		public Task(String name, @Nullable Action0 cancel, @Nullable DispatchMode mode) {
			this.name = name;
			this.cancel = cancel;
			this.mode = mode;
		}

		public abstract boolean isBarrier();

		public abstract boolean process(BatchTask batch);
	}

	/**
	 * 统一载荷任务：用 {@link TaskBody} 一份代码覆盖 action/func/procedure/func0 四种载荷。
	 * 队列语义无返回值消费者，callRaw 的结果丢弃；名字解析见 {@link TaskBody#logName}。
	 */
	public static final class TaskBodyTask extends Task {
		private final TaskBody<?> body;

		public TaskBodyTask(TaskBody<?> body, @Nullable String name, @Nullable Action0 cancel,
		                    @Nullable DispatchMode mode) {
			super(body.logName(name), cancel, mode);
			this.body = body;
		}

		@Override
		public boolean isBarrier() {
			return false;
		}

		@Override
		public boolean process(BatchTask batch) {
			try {
				body.callRaw();
			} catch (Throwable e) {
				logger.error("TaskOneByOne: {}", name, e);
			}
			return true;
		}
	}

	public static final class TaskBarrierProcedure extends Task {
		private final BarrierProcedure barrier;
		private final int sum;

		public TaskBarrierProcedure(BarrierProcedure barrier, int sum, @Nullable DispatchMode mode) {
			super(barrier.getName(), barrier::cancel, mode);
			this.barrier = barrier;
			this.sum = sum;
		}

		@Override
		public boolean isBarrier() {
			return true;
		}

		@Override
		public boolean process(BatchTask batch) {
			return barrier.reach(batch, sum);
		}
	}

	public static final class TaskBarrierAction extends Task {
		private final BarrierAction barrier;
		private final int sum;

		public TaskBarrierAction(BarrierAction barrier, int sum, @Nullable DispatchMode mode) {
			super(barrier.actionName, barrier::cancel, mode);
			this.barrier = barrier;
			this.sum = sum;
		}

		@Override
		public boolean isBarrier() {
			return true;
		}

		@Override
		public boolean process(BatchTask batch) {
			return barrier.reach(batch, sum);
		}
	}

	public static abstract class Barrier extends ReentrantLock {
		private enum State { Waiting, Running, Finished, Canceling, Canceled }

		private final HashSet<BatchTask> reached = new HashSet<>();
		private final @Nullable Action0 cancelAction;
		private int count;
		private State state = State.Waiting;

		public Barrier(int count, @Nullable Action0 cancelAction) {
			this.cancelAction = cancelAction;
			this.count = count;
		}

		public abstract String getName();

		public abstract void run() throws Exception;

		// 锁内快照清空、锁外推进：避免持本屏障锁触发其他桶回滚时形成 barrier 间反向死锁
		private void reachedRunNext() {
			BatchTask[] batches;
			lock();
			try {
				if (reached.isEmpty())
					return;
				batches = reached.toArray(new BatchTask[0]);
				reached.clear();
			} finally {
				unlock();
			}
			Throwable failure = null;
			for (var batch : batches) {
				try {
					batch.runNext();
				} catch (Throwable e) {
					// reached已清空：派发拒绝或 Error 均不能跳过其余桶的收尾。
					if (failure == null)
						failure = e;
					else if (failure != e)
						failure.addSuppressed(e);
				}
			}
			if (failure != null)
				throw Zeze.Util.Task.forceThrow(failure);
		}

		public boolean reach(BatchTask batch, int sum) {
			lock();
			try {
				if (state != State.Waiting && state != State.Canceling)
					return true;

				reached.add(batch);
				if (state == State.Canceling)
					return false; // 补偿仍在执行，迟到桶也由取消方在补偿结束后推进。

				count -= sum;
				if (count > 0)
					return false;
				state = State.Running; // 与 cancel 在同一临界区决定归属，只有执行方负责推进。
			} finally {
				unlock();
			}
			try {
				run(); // 外部回调不持有屏障锁，允许提交新任务
			} catch (Throwable ex) {
				logger.error("{} run exception", getName(), ex);
			} finally {
				lock();
				try {
					state = State.Finished;
				} finally {
					unlock();
				}
				reachedRunNext();
			}
			return false;
		}

		public void cancel() {
			lock();
			try {
				if (state != State.Waiting)
					return;
				state = State.Canceling;
			} finally {
				unlock();
			}
			try {
				if (cancelAction != null)
					cancelAction.run();
			} catch (Throwable ex) {
				logger.error("{} cancel exception", getName(), ex);
			} finally {
				lock();
				try {
					state = State.Canceled;
				} finally {
					unlock();
				}
				// 未到达的桶以后直接消费已取消的屏障；已到达的桶在补偿结束后推进。
				reachedRunNext();
			}
		}
	}

	public static final class BarrierProcedure extends Barrier {
		private final Procedure procedure;

		public BarrierProcedure(Procedure procedure, int count, @Nullable Action0 cancelAction) {
			super(count, cancelAction);
			this.procedure = procedure;
		}

		@Override
		public String getName() {
			return procedure.getActionName();
		}

		@Override
		public void run() {
			procedure.call();
		}
	}

	public static final class BarrierAction extends Barrier {
		private final Action0 action;
		private final String actionName;

		public BarrierAction(String actionName, Action0 action, int count, @Nullable Action0 cancelAction) {
			super(count, cancelAction);
			this.action = action;
			this.actionName = actionName;
		}

		@Override
		public String getName() {
			return actionName;
		}

		@Override
		public void run() throws Exception {
			action.run();
		}
	}
}
