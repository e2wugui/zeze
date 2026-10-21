package Zeze.Util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;

// 异步锁. 不支持重入.
// 派发模式由构造参数决定(实例级不可变配置)：
// 异步(默认)：每次派发把单个回调投入线程池执行，回调收尾的leave()继续派发下一个；
// 同步：拿到派发权的线程在dispatchLoop里同线程顺序内联执行整个队列，循环独占释放，
// leave()只清ownerThread/current、不派发(重入派发不可能发生，深队列无SOE)。
public final class AsyncLock {
	private static final VarHandle stateHandle;
	private static final VarHandle inlineDriverThreadHandle;
	// 按原始执行器登记，兼容 execute 包装 Runnable 的线程池；shutdownNow 的返回值
	// 无法可靠识别交接。只有未开始的驱动可由停机线程认领，已运行回调仍自行 leave。
	private static final ConcurrentHashMap<PendingDispatch, Executor> pendingDispatches = new ConcurrentHashMap<>();

	private final class PendingDispatch extends AtomicBoolean implements Runnable {
		private final Action0 onReady;

		private PendingDispatch(Action0 onReady) {
			this.onReady = onReady;
		}

		@Override
		public void run() {
			if (claim())
				runWithLeave(onReady);
		}

		private void fallback() {
			if (claim())
				driveRejectedInline(onReady);
		}

		private boolean claim() {
			if (!compareAndSet(false, true))
				return false;
			pendingDispatches.remove(this);
			return true;
		}
	}

	// Task 已关闭旧池后在 taskLock 外调用；回调与拒绝投递时一样内联续走。
	static void cancelPendingDispatches(Executor executor) {
		for (var entry : pendingDispatches.entrySet()) {
			if (entry.getValue() == executor)
				entry.getKey().fallback();
		}
	}

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

	// 获取锁成功时回调onEnter,回调回程中可以leave,回调完成时也会强制leave
	public void enter(Action0 onEnter) {
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
	private void runWithLeave(Action0 onReady) {
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
				var dispatch = new PendingDispatch(onReady);
				try {
					// poolOrThrow：池未初始化/已停机（shutdownPools先置null）时
					// getThreadPool()返回null，裸execute直接NPE且无回滚。
					var executor = Task.poolOrThrow(false);
					pendingDispatches.put(dispatch, executor);
					executor.execute(dispatch);
				} catch (RuntimeException e) {
					// 派发被拒：不回滚不复位——复位后不复查队列，与并发enter的
					// offer后重试CAS竞态会把回调滞留成无派发者真空（直到下一个enter才自愈）。
					// 就地内联续走整个派发链；回调已实际执行故不重抛（重抛会让调用方二次应答）。
					// 拒绝当前仅停机窗口可达：Task默认池无界队列（虚拟线程每任务一线程），
					// 饱和不拒、仅shutdown后拒；经initThreadPool装有界池使饱和拒绝成为
					// 常态时，本兜底转为热路径，需重估。
					Task.logger.warn("AsyncLock: dispatch rejected, fallback inline run, {} pending callback(s)",
							readyQueue.size() + 1, e);
					dispatch.fallback();
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
	private void driveRejectedInline(Action0 first) {
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
				while (onReady == null) {
					// 队列空：发布释放后按需重获。重获CAS胜出后重取仍空时不得返回——胜出
					// 即此刻无他人接管派发，带state==1返回将无人推进、锁永久毒化；回环
					// 再发布直到拿到回调或干净退出（与tryNextAsync尾段语义一致）。
					state = 0;
					if (readyQueue.isEmpty() || !stateHandle.compareAndSet(this, 0, 1)) // retry, rare-path
						return;
					onReady = readyQueue.poll();
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

	// 在获取锁的情况下,释放锁并等到有通知且获取锁时回调自身
	public void leaveAndWaitNotify() {
		assert state == 1 && current != null;
		waitQueue.addFirst(current);
		leave();
	}

	// 在获取锁的情况下,发出通知激活整个等待队列
	public void notifyAllWait() {
		assert state == 1;
		readyQueue.addAll(waitQueue);
		waitQueue.clear();
	}
}
