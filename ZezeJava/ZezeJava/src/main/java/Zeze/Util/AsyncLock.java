package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 异步锁. 暂不支持重入
public final class AsyncLock {
	public static final boolean tryNextSync = "true".equalsIgnoreCase(System.getProperty("AsyncLock.tryNextSync"));
	private static final @NotNull VarHandle stateHandle;

	static {
		try {
			stateHandle = MethodHandles.lookup().findVarHandle(AsyncLock.class, "state", int.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private volatile int state;
	private final ConcurrentLinkedQueue<Action0> readyQueue = new ConcurrentLinkedQueue<>();
	private final ArrayDeque<Action0> waitQueue = new ArrayDeque<>();
	private @Nullable Action0 current;
	private @Nullable Thread ownerThread;

	public boolean isLocked() {
		return state != 0;
	}

	public boolean isHeldByCurrentThread() {
		return ownerThread == Thread.currentThread();
	}

	public Action0 getCurrent() {
		return current;
	}

	public void setCurrent(Action0 current) {
		this.current = current;
	}

	// 获取锁成功时回调onEnter,回调回程中可以leave,回调完成时也会强制leave
	public void enter(Action0 onEnter) {
		if (stateHandle.compareAndSet(this, 0, 1)) { // try lock, fast-path
			try {
				ownerThread = Thread.currentThread();
				current = onEnter;
				onEnter.run();
			} catch (Throwable e) { // print stacktrace.
				Task.logger.error("AsyncLock.enter exception:", e);
			} finally {
				leave();
			}
		} else {
			readyQueue.offer(onEnter);
			if (stateHandle.compareAndSet(this, 0, 1)) // retry lock, rare-path
				tryNext();
		}
	}

	// 同enter, 只是立即取到锁也异步执行onEnter
	public void enterAsync(Action0 onEnter) {
		readyQueue.offer(onEnter);
		if (stateHandle.compareAndSet(this, 0, 1)) // try lock
			tryNext();
	}

	private void tryNext() {
		if (tryNextSync)
			tryNextSync();
		else
			tryNextAsync();
	}

	private void tryNextSync() {
		for (; ; ) {
			var onReady = readyQueue.poll(); // onEnter or onNotify
			if (onReady != null) {
				try {
					ownerThread = Thread.currentThread();
					current = onReady;
					onReady.run();
				} catch (Throwable e) { // print stacktrace.
					Task.logger.error("AsyncLock.tryNext exception:", e);
				} finally {
					leave();
				}
				return;
			}
			state = 0;
			if (readyQueue.isEmpty() || !stateHandle.compareAndSet(this, 0, 1)) // retry, rare-path
				return;
		}
	}

	private void tryNextAsync() {
		for (; ; ) {
			var onReady = readyQueue.poll(); // onEnter or onNotify
			if (onReady != null) {
				Task.getThreadPool().execute(() -> {
					try {
						ownerThread = Thread.currentThread();
						current = onReady;
						onReady.run();
					} catch (Throwable e) { // print stacktrace.
						Task.logger.error("AsyncLock.tryNext exception:", e);
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

	// 释放锁,可能触发其它线程获取锁的回调
	public void leave() {
		if (ownerThread != Thread.currentThread())
			return;
		ownerThread = null;
		current = null;
		tryNext();
	}

	// 在获取锁的情况下,释放锁并等到有通知且获取锁时回调onNotify
	public void leaveAndWaitNotify(Action0 onNotify) {
		assert state == 1;
		waitQueue.addLast(onNotify);
		leave();
	}

	// 在获取锁的情况下,释放锁并等到有通知且获取锁时回调自身
	public void leaveAndWaitNotify() {
		assert state == 1 && current != null;
		waitQueue.addFirst(current);
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
