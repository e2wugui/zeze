package Zeze.Util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;

public class ThreadFactoryWithName implements ThreadFactory {
	private static final @NotNull Thread.Builder.OfVirtual virtualThreadBuilder = Thread.ofVirtual();

	public static boolean isVirtualThreadEnabled() {
		return true;
	}

	protected final AtomicLong threadNumber = new AtomicLong();
	protected final @NotNull String namePrefix;
	protected final int priority;
	protected final boolean canBeVirtualThread;

	public ThreadFactoryWithName(@NotNull String poolName) {
		this(poolName, Thread.NORM_PRIORITY, true);
	}

	public ThreadFactoryWithName(@NotNull String poolName, int priority) {
		this(poolName, priority, true);
	}

	public ThreadFactoryWithName(@NotNull String poolName, int priority, boolean canBeVirtualThread) {
		namePrefix = poolName + '-';
		this.priority = priority;
		this.canBeVirtualThread = priority == Thread.NORM_PRIORITY && canBeVirtualThread;
	}

	@Override
	public @NotNull Thread newThread(@NotNull Runnable r) {
		Thread t;
		if (canBeVirtualThread) {
			t = virtualThreadBuilder.unstarted(r);
			t.setName(namePrefix + threadNumber.incrementAndGet());
		} else {
			t = new Thread(r, namePrefix + threadNumber.incrementAndGet());
			t.setDaemon(true); // 先不考虑安全关闭，以后再调整。
			if (t.getPriority() != priority)
				t.setPriority(priority);
		}
		t.setUncaughtExceptionHandler((__, e) -> Task.logger.error("uncaught exception:", e));
		return t;
	}
}
