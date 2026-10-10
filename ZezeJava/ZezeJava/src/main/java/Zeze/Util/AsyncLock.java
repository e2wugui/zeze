package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 异步锁. 不支持重入.
// 派发模式由构造参数决定(实例级不可变配置)：
// 异步(默认)：每次派发把单个回调投入线程池执行，回调收尾的leave()继续派发下一个；
// 同步：拿到派发权的线程在dispatchLoop里同线程顺序内联执行整个队列，循环独占释放，
// leave()只清ownerThread/current、不派发(重入派发不可能发生，深队列无SOE)。
public final class AsyncLock {
	private static final @NotNull VarHandle stateHandle;
	private static final @NotNull VarHandle inlineDriverThreadHandle;

	static {
		try {
			stateHandle = MethodHandles.lookup().findVarHandle(AsyncLock.class, "state", int.class);
			inlineDriverThreadHandle = MethodHandles.lookup()
					.findVarHandle(AsyncLock.class, "inlineDriverThread", Thread.class);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	// 不可变配置而非运行时状态：取代旧的系统属性全局开关(其生效依赖类惰性加载时机，
	// 调用方晚于静态初始化set的属性随时可能静默失效)。
	private final boolean syncDispatch;
	private volatile int state;
	// 拒绝兜底的内联驱动线程（见driveRejectedInline）：驱动期间该线程的leave()只清
	// owner不派发，派发由驱动循环推进——否则runWithLeave→leave→tryNextAsync逐回调
	// 递归，深队列栈溢出后state==1无人派发，锁永久毒化。
	private volatile @Nullable Thread inlineDriverThread;
	// 测试钩子（一次性）：非null时在拒绝兜底驱动"队列耗尽、state=0发布后"的边界执行并
	// 自动置null——用于确定性复现驱动交接交错（旧驱动停在此处，新线程接管派发）。仅测试使用。
	Action0 testHookAtInlineDriverEmptyBoundary;
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
				try {
					// poolOrThrow：池未初始化/已停机（shutdownPools先置null）时
					// getThreadPool()返回null，裸execute直接NPE且无回滚。
					Task.poolOrThrow(false).execute(() -> runWithLeave(onReady));
				} catch (RuntimeException e) {
					// 派发被拒：不回滚不复位——复位后不复查队列，与并发enter的
					// offer后重试CAS竞态会把回调滞留成无派发者真空（直到下一个enter才自愈）。
					// 就地内联续走整个派发链；回调已实际执行故不重抛（重抛会让调用方二次应答）。
					Task.logger.warn("AsyncLock: dispatch rejected, fallback inline run, {} pending callback(s)",
							readyQueue.size() + 1, e);
					driveRejectedInline(onReady);
				}
				return;
			}
			state = 0;
			if (readyQueue.isEmpty() || !stateHandle.compareAndSet(this, 0, 1)) // retry, rare-path
				return;
		}
	}

	// 拒绝兜底的循环驱动（借用同步dispatchLoop的所有权协议）：每回调固定栈帧，深队列
	// 不再递归SOE。驱动线程的leave()只清owner/current、不派发（inlineDriverThread标记），
	// 派发由本循环推进；整个队列就地内联耗尽后释放派发权，池恢复由其后的正常派发路径
	// 检测（驱动中逐个探测投递会因逐回调告警日志拖垮深队列）。
	private void driveRejectedInline(@NotNull Action0 first) {
		inlineDriverThread = Thread.currentThread();
		try {
			var onReady = first;
			for (; ; ) {
				try {
					ownerThread = Thread.currentThread();
					current = onReady;
					onReady.run();
				} catch (Throwable e) { // print stacktrace.
					Task.logger.error("AsyncLock.inline dispatch exception:", e);
				} finally {
					if (ownerThread == Thread.currentThread()) { // 回调内部未显式leave()时就地释放
						ownerThread = null;
						current = null;
					}
				}
				onReady = readyQueue.poll();
				if (onReady == null) {
					state = 0;
					var hook = testHookAtInlineDriverEmptyBoundary;
					if (hook != null) {
						testHookAtInlineDriverEmptyBoundary = null; // 一次性
						try {
							hook.run(); // 测试注入：确定性停在"state=0已发布、驱动尚未退场"的交接窗口
						} catch (Exception e) {
							throw Task.forceThrow(e);
						}
					}
					if (readyQueue.isEmpty() || !stateHandle.compareAndSet(this, 0, 1)) // retry, rare-path
						return;
					onReady = readyQueue.poll();
					if (onReady == null)
						return; // CAS胜出后队列被并发取走：他人已接管派发，让位
				}
			}
		} finally {
			// 按驱动身份清理：state=0发布后到此处之间，新线程可能已CAS接管派发并写入自己的
			// 驱动标记，无条件置null会抹掉新代标记——新驱动回调显式leave()时误判"非内联驱动"
			// 而启动第二条派发链，池恢复后与内联循环并发执行回调，互斥破坏。身份CAS：标记仍
			// 属于本线程才清理；先读后写的非原子判断同样存在覆盖新代标记的窗口，必须CAS。
			inlineDriverThreadHandle.compareAndSet(this, Thread.currentThread(), null);
		}
	}

	// 释放锁,可能触发其它线程获取锁的回调
	public void leave() {
		if (ownerThread != Thread.currentThread())
			return;
		ownerThread = null;
		current = null;
		// 同步模式：派发循环独占释放(state==1保持到回调返回)，这里派发会造成重入递归；
		// 内联驱动（拒绝兜底）同理：派发由driveRejectedInline循环推进。
		if (!syncDispatch && inlineDriverThread != Thread.currentThread())
			tryNextAsync();
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
