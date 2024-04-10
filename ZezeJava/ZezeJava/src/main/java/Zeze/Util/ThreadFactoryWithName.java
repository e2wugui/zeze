package Zeze.Util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThreadFactoryWithName implements ThreadFactory {
	private static final @Nullable Object virtualThreadBuilder;
	private static final @Nullable MethodHandle mhUnstarted;

	static {
		Object virtualThreadBuilder0;
		MethodHandle mhUnstarted0;
		try {
			var lookup = MethodHandles.lookup();
			virtualThreadBuilder0 = lookup.findStatic(Thread.class, "ofVirtual",
					MethodType.methodType(Class.forName("java.lang.Thread$Builder$OfVirtual"))).invoke();
			mhUnstarted0 = lookup.findVirtual(Class.forName("java.lang.Thread$Builder"), "unstarted",
					MethodType.methodType(Thread.class, Runnable.class));
			Task.logger.info("ThreadFactoryWithName can use virtual thread");
		} catch (Throwable ignored) {
			virtualThreadBuilder0 = null;
			mhUnstarted0 = null;
		}
		virtualThreadBuilder = virtualThreadBuilder0;
		mhUnstarted = mhUnstarted0;
	}

	public static boolean isVirtualThreadEnabled() {
		return mhUnstarted != null;
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
		if (mhUnstarted != null && canBeVirtualThread) {
			try {
				t = (Thread)mhUnstarted.invoke(virtualThreadBuilder, r);
				t.setName(namePrefix + threadNumber.incrementAndGet());
			} catch (Throwable e) {
				Task.forceThrow(e);
				return null; // never run here
			}
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
