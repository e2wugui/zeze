package Zeze.Util;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadFactoryWithName implements ThreadFactory {
	private static final Object ofVirtual;
	private static final Method unstartedMethod;
	// private static final AtomicInteger poolNumber = new AtomicInteger(1);

	protected final AtomicInteger threadNumber = new AtomicInteger(1);
	protected final String namePrefix;

	static {
		Object ofVirtual0 = null;
		Method unstartedMethod0 = null;
		try {
			//noinspection JavaReflectionMemberAccess
			ofVirtual0 = Thread.class.getMethod("ofVirtual", (Class<?>[])null).invoke(null);
			unstartedMethod0 = Class.forName("java.lang.Thread.Builder").getMethod("unstarted", Runnable.class);
			Task.logger.info("ThreadFactoryWithName use virtual thread");
		} catch (ReflectiveOperationException ignored) {
		}
		ofVirtual = ofVirtual0;
		unstartedMethod = unstartedMethod0;
	}

	public ThreadFactoryWithName(String poolName) {
		namePrefix = poolName + '-';
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t;
		if (unstartedMethod != null) {
			try {
				t = (Thread)unstartedMethod.invoke(ofVirtual, r);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException(e);
			}
		} else {
			t = new Thread(null, r, namePrefix + threadNumber.getAndIncrement(), 0);
			//if (t.isDaemon())
			//    t.setDaemon(false);
			t.setDaemon(true); // 先不考虑安全关闭，以后再调整。
			if (t.getPriority() != Thread.NORM_PRIORITY)
				t.setPriority(Thread.NORM_PRIORITY);
		}
		t.setUncaughtExceptionHandler((t2, e) -> Task.logger.error("fatal exception", e));
		return t;
	}
}
