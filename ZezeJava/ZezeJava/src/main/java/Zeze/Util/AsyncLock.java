package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 异步锁. 暂不支持重入
public final class AsyncLock {
	// 非 final：static final boolean 是编译期常量(JIT 常量折叠)，测试无法在运行时切换验证同步模式。
	public static volatile boolean tryNextSync = "true".equalsIgnoreCase(System.getProperty("AsyncLock.tryNextSync"));
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
	// 派发中重入标记(仅同步模式使用)：回调内部或收尾的leave()→tryNextSync检测到后直接返回，
	// 由外层派发循环继续poll下一个回调，替代旧的嵌套递归执行(深等待队列会StackOverflowError)。
	// 只有派发线程在持有派发权(state==1)期间读写，无需同步。
	private boolean dispatching;

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
	public void enterAsync(@NotNull Action0 onEnter) {
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
		if (dispatching)
			return; // 正在派发循环内(回调里的leave()触发)：释放与后续派发都由外层循环统一处理。
		for (; ; ) {
			var onReady = readyQueue.poll(); // onEnter or onNotify
			if (onReady != null) {
				dispatching = true;
				try {
					ownerThread = Thread.currentThread();
					current = onReady;
					onReady.run();
				} catch (Throwable e) { // print stacktrace.
					Task.logger.error("AsyncLock.tryNext exception:", e);
				} finally {
					leave(); // 内层tryNextSync因dispatching直接返回，不再嵌套递归执行
					dispatching = false;
				}
				continue; // 同线程顺序内联执行下一个回调；每回调占用固定栈帧，深队列不再SOE。
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
