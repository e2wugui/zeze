package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/** 回调锁的公共协议：临界区结束后自动释放，等待通知时保存回调并在唤醒后从头执行。 */
public abstract class AbstractAsyncLock {
	protected static final Logger logger = LogManager.getLogger(AbstractAsyncLock.class);
	private static final VarHandle stateHandle;

	static {
		try {
			stateHandle = MethodHandles.lookup().findVarHandle(AbstractAsyncLock.class, "state", int.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	// state 表示派发权，交接任务尚未开始时也保持 1。
	private volatile int state;
	protected final ConcurrentLinkedQueue<Action0> readyQueue = new ConcurrentLinkedQueue<>();
	private @Nullable ArrayDeque<Action0> waitQueue;
	private @Nullable Action0 current;
	private @Nullable Thread ownerThread;

	public final boolean isLocked() {
		return state != 0;
	}

	// 锁忙时仅入队，不阻塞等待回调完成；回调返回时自动释放。
	public abstract void enter(Action0 onEnter);

	// 非持锁线程调用无效。释放后不得再依赖临界区保护。
	public abstract void leave();

	protected final boolean tryLock() {
		return stateHandle.compareAndSet(this, 0, 1);
	}

	// 仅由派发权持有者调用。返回 null 时已交出派发权，否则仍独占派发权。
	protected final @Nullable Action0 pollOrUnlock() {
		for (; ; ) {
			var onReady = readyQueue.poll();
			if (onReady != null)
				return onReady;
			state = 0;
			// 释放后复查，避免并发入队的任务无人推进；重获后即使再次 poll 为空也必须释放。
			if (readyQueue.isEmpty() || !tryLock())
				return null;
		}
	}

	protected final void runCallback(Action0 onReady) {
		ownerThread = Thread.currentThread();
		current = onReady;
		try {
			onReady.run();
		} catch (Throwable e) {
			logger.error("AsyncLock callback exception:", e);
		}
	}

	protected final boolean clearOwner() {
		if (ownerThread != Thread.currentThread())
			return false;
		ownerThread = null;
		current = null;
		return true;
	}

	// 同步派发和异步拒绝兜底共用；first 是已经出队的首个回调，也可为 null。
	// 调用期间 leave() 必须只清持锁身份，后续派发由本循环推进，避免深队列递归。
	protected final void drainInline(@Nullable Action0 first) {
		var onReady = first;
		for (; ; ) {
			if (onReady == null && (onReady = pollOrUnlock()) == null)
				return;
			try {
				runCallback(onReady);
			} finally {
				clearOwner();
			}
			onReady = null;
		}
	}

	// 必须在持锁回调内调用，随后由调用方 return；通知后回调从头执行，不保留调用栈。
	public final void leaveAndWaitNotify() {
		var onReady = current;
		assert ownerThread == Thread.currentThread() && onReady != null;
		var queue = waitQueue;
		if (queue == null)
			waitQueue = queue = new ArrayDeque<>();
		queue.addFirst(onReady); // 保持原有等待者后进先出的顺序。
		leave();
	}

	// 必须在持锁回调内调用，将等待者移回就绪队列。
	public final void notifyAllWait() {
		assert ownerThread == Thread.currentThread();
		var queue = waitQueue;
		if (queue != null && !queue.isEmpty()) {
			readyQueue.addAll(queue);
			queue.clear();
		}
	}
}
