package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 异步锁. 暂不支持重入.
// 派发模式由构造参数决定(实例级不可变配置)：
// 异步(默认)：每次派发把单个回调投入线程池执行，回调收尾的leave()继续派发下一个；
// 同步：拿到派发权的线程在dispatchLoop里同线程顺序内联执行整个队列，循环独占释放，
// leave()只清ownerThread/current、不派发(重入派发不可能发生，深队列无SOE)。
public final class AsyncLock {
	private static final @NotNull VarHandle stateHandle;

	static {
		try {
			stateHandle = MethodHandles.lookup().findVarHandle(AsyncLock.class, "state", int.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	// 不可变配置而非运行时状态：取代旧的系统属性全局开关(其生效依赖类惰性加载时机，
	// 调用方晚于静态初始化set的属性随时可能静默失效)。
	private final boolean syncDispatch;
	private volatile int state;
	private final ConcurrentLinkedQueue<Action0> readyQueue = new ConcurrentLinkedQueue<>();
	private final ArrayDeque<Action0> waitQueue = new ArrayDeque<>();
	private @Nullable Action0 current;
	private @Nullable Thread ownerThread;

	public AsyncLock() {
		this(false);
	}

	public AsyncLock(boolean syncDispatch) {
		this.syncDispatch = syncDispatch;
	}

	public boolean isLocked() {
		return state != 0;
	}

	public boolean isHeldByCurrentThread() {
		return ownerThread == Thread.currentThread();
	}

	public @Nullable Action0 getCurrent() {
		return current;
	}

	public void setCurrent(@Nullable Action0 current) {
		this.current = current;
	}

	// 获取锁成功时回调onEnter,回调回程中可以leave,回调完成时也会强制leave
	public void enter(@NotNull Action0 onEnter) {
		if (syncDispatch) {
			// 入队后进派发循环：快路径同样排队，严格FIFO不插队
			readyQueue.offer(onEnter);
			if (stateHandle.compareAndSet(this, 0, 1)) // try lock
				dispatchLoop();
			return;
		}
		if (stateHandle.compareAndSet(this, 0, 1)) { // try lock, fast-path
			runWithLeave(onEnter);
		} else {
			readyQueue.offer(onEnter);
			if (stateHandle.compareAndSet(this, 0, 1)) // retry lock, rare-path
				tryNextAsync();
		}
	}

	// 异步模式回调执行：finally必经leave()（回调内已显式leave()则空过）保证释放并继续派发。
	// 调用者线程不限：enter快路径为进入线程，tryNextAsync为线程池线程。
	private void runWithLeave(@NotNull Action0 onReady) {
		try {
			ownerThread = Thread.currentThread();
			current = onReady;
			onReady.run();
		} catch (Throwable e) { // print stacktrace.
			Task.logger.error("AsyncLock.runWithLeave exception:", e);
		} finally {
			leave();
		}
	}

	// 同步模式派发循环：当前线程独占派发权(state==1)直到队列耗尽。
	// 回调收尾不调leave()：回调内已显式leave()时ownerThread已清空、此处跳过，
	// 否则就地内联释放，循环继续poll下一个。每回调固定栈帧，深队列无SOE。
	private void dispatchLoop() {
		for (; ; ) {
			var onReady = readyQueue.poll(); // onEnter or onNotify
			if (onReady != null) {
				try {
					ownerThread = Thread.currentThread();
					current = onReady;
					onReady.run();
				} catch (Throwable e) { // print stacktrace.
					Task.logger.error("AsyncLock.dispatchLoop exception:", e);
				} finally {
					if (ownerThread == Thread.currentThread()) { // 回调内部未显式leave()时就地释放
						ownerThread = null;
						current = null;
					}
				}
				continue; // 同线程顺序内联执行下一个回调
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
				Task.getThreadPool().execute(() -> runWithLeave(onReady));
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
		if (!syncDispatch)
			tryNextAsync(); // 同步模式：派发循环独占释放(state==1保持到回调返回)，这里派发会造成重入递归
	}

	// 在获取锁的情况下,释放锁并等到有通知且获取锁时回调onNotify
	public void leaveAndWaitNotify(@NotNull Action0 onNotify) {
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
