package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.jspecify.annotations.Nullable;

/**
 * 回调式异步锁（不可重入）：无竞争时直接执行，排队回调逐个交给指定执行器。
 * 执行器拒绝任务（抛出 {@link RejectedExecutionException}）或在 execute 内直接回调时，
 * 退化为在当前线程内联循环推进队列。
 */
public final class AsyncLock extends AbstractAsyncLock {
	private static final VarHandle inlineDriverThreadHandle;

	static {
		try {
			inlineDriverThreadHandle = MethodHandles.lookup()
					.findVarHandle(AsyncLock.class, "inlineDriverThread", Thread.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private final Executor executor;
	private volatile @Nullable Thread inlineDriverThread;

	public AsyncLock(Executor executor) {
		this.executor = executor;
	}

	@Override
	public void enter(Action0 onEnter) {
		if (tryLock())
			runWithLeave(onEnter);
		else {
			readyQueue.offer(onEnter);
			if (tryLock()) // 入队与释放并发时，由胜出的线程继续派发。
				tryNextAsync();
		}
	}

	private void runWithLeave(Action0 onReady) {
		try {
			runCallback(onReady);
		} finally {
			leave();
		}
	}

	// 仅检测 execute 尚未返回时的同线程内联执行；仅由 submitter 读写，无需 volatile。
	private final class Dispatch implements Runnable {
		private final Action0 onReady;
		private final Thread submitter = Thread.currentThread();
		private boolean submitting = true;

		private Dispatch(Action0 onReady) {
			this.onReady = onReady;
		}

		@Override
		public void run() {
			if (Thread.currentThread() == submitter && submitting)
				driveInline(onReady);
			else
				runWithLeave(onReady);
		}
	}

	private void tryNextAsync() {
		var onReady = pollOrUnlock();
		if (onReady == null)
			return;
		var dispatch = new Dispatch(onReady);
		try {
			executor.execute(dispatch);
		} catch (RuntimeException e) {
			// 执行器拒绝或抛未检异常时保持派发权，在当前线程内联推进队列避免锁毒化。
			logger.warn("AsyncLock: dispatch exception, fallback inline run", e);
			driveInline(onReady);
		} finally {
			dispatch.submitting = false;
		}
	}

	private void driveInline(Action0 first) {
		inlineDriverThread = Thread.currentThread();
		try {
			drainInline(first);
		} finally {
			// 发布 state=0 后新驱动可能已经接管，不能清掉新一代的标记。
			inlineDriverThreadHandle.compareAndSet(this, Thread.currentThread(), null);
		}
	}

	@Override
	public void leave() {
		if (clearOwner() && inlineDriverThread != Thread.currentThread())
			tryNextAsync();
	}
}
