package Zeze.Util;

import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import static Zeze.Util.DeadlockBreaker.MAX_DEPTH;

public class TimeoutDiagnosable extends Timeout {
	private static final Logger logger = LogManager.getLogger(TimeoutDiagnosable.class);
	// 任务存活握手：close 先于派发执行时，中断动作不得落在池线程正在跑的下一个任务上
	// （桶 poll 到派发执行之间任务可能已结束；派发车道积压时该窗口会被拉宽）。
	// thread 置 null 即已 close（单一事实来源）：读写都必须在 handshake 锁内，
	// 且打断必须锁内执行——锁外打断与 close 的清标志存在先置位后清除的窗口。
	private final ReentrantLock handshake = new ReentrantLock();
	private @Nullable Thread thread = Thread.currentThread();

	public static @Nullable Timeout create(long timeoutMs) {
		return create(timeoutMs, false);
	}

	public static @Nullable Timeout create(long timeoutMs, boolean critical) {
		if (Reflect.inDebugMode || critical)
			return null;
		var timeoutManager = Task.getTimeoutManagerOrNull();
		if (timeoutManager == null)
			return null; // shutdown 窗口（管理器先于池关闭）：降级为无看门狗，不抛错
		return timeoutManager.schedule(timeoutMs, () -> {}, TimeoutDiagnosable::new);
	}

	TimeoutDiagnosable(TimeoutManager.TimeoutSet timeoutSet, Action0 action0) {
		super(timeoutSet, action0);
	}

	@Override
	public void run() {
		// 最多只会触发一次。thread 为 null = 已 close，不再打断（interruptThread 内部已判 null）。
		handshake.lock();
		try {
			interruptThread(thread);
		} finally {
			handshake.unlock();
		}
	}

	@Override
	public void close() {
		handshake.lock();
		try {
			thread = null;
		} finally {
			handshake.unlock();
		}
		super.close();
		// 清除本线程残留的中断标志（锁外：若 run 正在锁内打断，这里等它完成后必清），
		// 防止毒化池线程上的下一个任务。
		//noinspection ResultOfMethodCallIgnored
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
		if (t != null && !t.isInterrupted() && t.isAlive()
			&& t.getPriority() <= Thread.NORM_PRIORITY) {
			var sb = new StringBuilder();
			formatStackTrace(t.getStackTrace(), sb);
			logger.warn("INTERRUPT thread '{}' for task timeout\n{}", t.getName(), sb.toString());
			t.interrupt();
		}
	}
}
