package Zeze.Util;

import java.util.concurrent.locks.ReentrantLock;

import Zeze.Transaction.DispatchMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import static Zeze.Util.DeadlockBreaker.MAX_DEPTH;

/** 可诊断超时看门狗：任务超时时记录目标线程堆栈并尝试中断，cancel 时确保不误伤复用线程的后续任务。 */
public class TimeoutDiagnosable extends Timeout {
	private static final Logger logger = LogManager.getLogger(TimeoutDiagnosable.class);
	// 存活握手：thread 在 handshake 锁内读写并置 null，中断亦在锁内执行，避免误伤复用该线程的后续任务
	private final ReentrantLock handshake = new ReentrantLock();
	private @Nullable Thread thread = Thread.currentThread();
	private boolean interruptedByTimeout;

	public static @Nullable Timeout create(long timeoutMs) {
		return create(timeoutMs, false);
	}

	public static @Nullable Timeout create(long timeoutMs, boolean critical) {
		if (Reflect.inDebugMode || critical)
			return null;
		var timeoutManager = Task.getTimeoutManagerOrNull();
		if (timeoutManager == null)
			return null; // shutdown 窗口（管理器先于池关闭）：降级为无看门狗，不抛错
		return timeoutManager.scheduleIfRunning(timeoutMs, () -> {}, TimeoutDiagnosable::new);
	}

	TimeoutDiagnosable(TimeoutManager.TimeoutSet timeoutSet, Action0 action0) {
		super(timeoutSet, action0);
	}

	@Override
	public void run() {
		// 最多触发一次；thread 为 null 表示已取消
		handshake.lock();
		try {
			interruptedByTimeout |= tryInterruptThread(thread);
		} finally {
			handshake.unlock();
		}
	}

	@Override
	public DispatchMode getDispatchMode() {
		return DispatchMode.Critical;
	}

	@Override
	public void cancel() {
		boolean clearInterrupt;
		handshake.lock();
		try {
			clearInterrupt = interruptedByTimeout && thread == Thread.currentThread();
			thread = null;
		} finally {
			handshake.unlock();
		}
		super.cancel();
		// 仅清理本看门狗留在目标线程上的中断标志。
		if (clearInterrupt)
			Thread.interrupted();
	}

	public static void formatStackTrace(StackTraceElement [] stackTrace, StringBuilder sb) {
		int i = 0;
		for (; i < stackTrace.length && i < MAX_DEPTH; i++)
			sb.append("\tat ").append(stackTrace[i]).append('\n');
		if (i < stackTrace.length)
			sb.append("\t...\n");
	}

	public static void interruptThread(@Nullable Thread t) {
		tryInterruptThread(t);
	}

	private static boolean tryInterruptThread(@Nullable Thread t) {
		if (t != null && !t.isInterrupted() && t.isAlive()
			&& t.getPriority() <= Thread.NORM_PRIORITY) {
			var sb = new StringBuilder();
			formatStackTrace(t.getStackTrace(), sb);
			logger.warn("INTERRUPT thread '{}' for task timeout\n{}", t.getName(), sb.toString());
			t.interrupt();
			return true;
		}
		return false;
	}
}
