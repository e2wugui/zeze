package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

// 异步锁. 暂不支持重入
public final class AsyncLock {
	private static final VarHandle stateHandle;

	static {
		try {
			stateHandle = MethodHandles.lookup().findVarHandle(AsyncLock.class, "state", int.class);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private volatile int state;
	private final ConcurrentLinkedQueue<Action0> readyQueue = new ConcurrentLinkedQueue<>();
	private final ArrayDeque<Action0> waitQueue = new ArrayDeque<>();

	// 获取锁成功时回调onEnter,回调回程中可以leave,回调完成时也会强制leave
	public void enter(Action0 onEnter) {
		if (stateHandle.compareAndSet(this, 0, 1)) { // try lock, fast-path
			try {
				onEnter.run();
			} catch (Throwable e) {
				Task.logger.error("AsyncLock.enter exception:", e);
			} finally {
				leave();
			}
		} else {
			readyQueue.offer(onEnter);
			if (stateHandle.compareAndSet(this, 0, 1)) // retry lock, rare-path
				leave();
		}
	}

	// 释放锁,可能触发其它线程获取锁的回调
	public void leave() {
		if (state == 0)
			return;
		for (; ; ) {
			var onReady = readyQueue.poll(); // onEnter or onNotify
			if (onReady != null) {
				Task.getThreadPool().execute(() -> {
					try {
						onReady.run();
					} catch (Throwable e) {
						Task.logger.error("AsyncLock.leave->onReady exception:", e);
					} finally {
						leave();
					}
				});
				return;
			}
			state = 0;
			if (readyQueue.isEmpty() || !stateHandle.compareAndSet(this, 0, 1)) // retry, rare-path
				return;
		}
	}

	// 在获取锁的情况下,释放锁并等到有通知且获取锁时回调onNotify
	public void leaveAndWaitNotify(Action0 onNotify) {
		assert state == 1;
		waitQueue.addLast(onNotify);
		leave();
	}

	// 在获取锁的情况下,发出通知激活等待队列中的一个
	public void notifyOneWait() {
		assert state == 1;
		var onNotify = waitQueue.pollFirst();
		if (onNotify != null)
			readyQueue.offer(onNotify);
	}

	// 在获取锁的情况下,发出通知激活整个等待队列
	public void notifyAllWait() {
		assert state == 1;
		readyQueue.addAll(waitQueue);
		waitQueue.clear();
	}
}
