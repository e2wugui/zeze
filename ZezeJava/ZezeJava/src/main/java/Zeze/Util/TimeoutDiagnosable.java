package Zeze.Util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import static Zeze.Util.DeadlockBreaker.MAX_DEPTH;

public class TimeoutDiagnosable extends Timeout {
	private static final Logger logger = LogManager.getLogger(TimeoutDiagnosable.class);
	private final Thread thread = Thread.currentThread();

	public static @Nullable Timeout createTimeout(long timeoutMs) {
		return createTimeout(timeoutMs, false);
	}

	public static @Nullable Timeout createTimeout(long timeoutMs, boolean critical) {
		if (Reflect.inDebugMode || critical)
			return null;
		return Task.getTimeoutManager().schedule(timeoutMs, () -> {}, TimeoutDiagnosable::new);
	}

	TimeoutDiagnosable(TimeoutManager.TimeoutSet timeoutSet, Action0 action0) {
		super(timeoutSet, action0);
	}

	@Override
	public void run() {
		// 最多只会触发一次。
		interruptThread(thread);
	}

	@Override
	public void close() {
		super.close();
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
