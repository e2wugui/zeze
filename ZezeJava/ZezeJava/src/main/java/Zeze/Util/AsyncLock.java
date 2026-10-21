package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.jspecify.annotations.Nullable;

/**
 * 回调式异步锁，不支持重入。无竞争时直接执行，排队回调逐个交给构造时传入的执行器。
 * 执行器由调用方管理，必须保证已接受的任务最终执行且不静默丢弃；拒绝任务应抛出
 * {@link RejectedExecutionException}，此时本锁在提交线程上循环执行积压回调。
 * 调用方应先结束在途业务再关闭执行器。shutdownNow 丢弃任务后，本锁不负责补跑或恢复，
 * 也不会自动切换到新执行器。执行器在 execute 内直接运行任务时，同样采用内联循环。
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

	// 只检测 execute 尚未返回时的同线程执行。普通单线程池稍后在提交线程上运行任务时，
	// submitting 已是 false，仍保持逐回调派发。该字段只由 submitter 读写，无需 volatile。
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
		} catch (RejectedExecutionException e) {
			// 保持派发权，不回滚入队；整条队列循环推进，只记录一次拒绝。
			logger.warn("AsyncLock: dispatch rejected, fallback inline run", e);
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
