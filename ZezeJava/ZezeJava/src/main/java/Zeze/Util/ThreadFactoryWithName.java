package Zeze.Util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;

// 命名线程工厂：前缀+编号命名，支持优先级与虚拟线程，未捕获异常统一记日志
public class ThreadFactoryWithName implements ThreadFactory {
	private static final @NotNull Thread.UncaughtExceptionHandler uncaughtHandler =
			(__, e) -> Task.logger.error("uncaught exception:", e);
	private static final @NotNull Thread.Builder.OfVirtual virtualThreadBuilder =
			Thread.ofVirtual().uncaughtExceptionHandler(uncaughtHandler);

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
			// 虚拟线程只能是daemon的,无法设置priority
		} else {
			t = new Thread(r, namePrefix + threadNumber.incrementAndGet());
			t.setDaemon(true); // daemon：不阻塞 JVM 退出
			if (t.getPriority() != priority)
				t.setPriority(priority);
			t.setUncaughtExceptionHandler(uncaughtHandler);
		}
		return t;
	}
}
