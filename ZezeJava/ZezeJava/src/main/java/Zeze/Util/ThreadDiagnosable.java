package Zeze.Util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThreadDiagnosable extends Thread {
	private static final Logger logger = LogManager.getLogger(ThreadDiagnosable.class);
	private static final ConcurrentHashSet<ThreadDiagnosable> workers = new ConcurrentHashSet<>();
	private static final AtomicLong currentSerial = new AtomicLong();

	// 这个线程池子绝对不能饥饿，使用独立的。
	private static final ScheduledExecutorService diagnoseScheduler
			= Executors.newScheduledThreadPool(5, new ThreadFactoryWithName("DiagnoseThread"));

	public static volatile boolean disableInterrupt = Reflect.inDebugMode;

	private final ConcurrentHashSet<Timeout> timeouts = new ConcurrentHashSet<>();

	// 这个实现方式预计还需要修改。
	private boolean critical = true;

	// 修改period，请重新调用。
	public static void startDiagnose(long period) {
		var current = currentSerial.incrementAndGet();
		diagnoseScheduler.schedule(new Runnable() {
			@Override
			public void run() {
				if (current != currentSerial.get())
					return;
				try {
					var now = System.currentTimeMillis();
					for (var worker : workers)
						worker.onTimer(now);
				} catch (Throwable e) { // logger.error
					logger.error("", e);
				} finally {
					diagnoseScheduler.schedule(this, period, TimeUnit.MILLISECONDS);
				}
			}
		}, period, TimeUnit.MILLISECONDS);
	}

	// 停止诊断检测。
	public static void stopDiagnose() {
		currentSerial.incrementAndGet(); // 让正在运行的诊断任务退出。
	}

	void diagnose() {
		if (disableInterrupt)
			return;
		logger.warn("INTERRUPT thread '{}' for task timeout", getName());
		interrupt();
		// todo more more ...
	}

	public Timeout createTimeout(long timeout) {
		var t = new Timeout(timeout);
		timeouts.add(t);
		return t;
	}

	public Critical enterCritical(boolean c) {
		var cry = new Critical(critical);
		critical = c;
		return cry;
	}

	public class Critical implements AutoCloseable {
		private final boolean saved;

		public Critical(boolean current) {
			this.saved = current;
		}

		@Override
		public void close() {
			// restore
			critical = saved;
		}
	}

	public void onTimer(long now) {
		for (var timeout : timeouts) {
			if (timeout.timeoutTime <= now) {
				// 每个timeout仅触发一次.
				if (timeouts.remove(timeout) != null)
					diagnoseScheduler.execute(timeout);
				break; // 开始诊断,就不需要继续了. 【如果需要继续检查，要注意此时在遍历timeouts】。
			}
		}
	}

	public class Timeout implements Runnable, AutoCloseable {
		private final long timeoutTime;

		public Timeout(long timeout) {
			timeoutTime = System.currentTimeMillis() + timeout;
		}

		@Override
		public void close() {
			timeouts.remove(this);
		}

		// 诊断主流程.
		@Override
		public void run() {
			diagnose();
		}
	}

	private ThreadDiagnosable(String threadName, Runnable r, int priority) {
		super(r, threadName);
		setDaemon(true);
		if (getPriority() != priority)
			setPriority(priority);
		setUncaughtExceptionHandler((__, e) -> logger.error("uncaught exception", e));
	}

	@Override
	public void run() {
		workers.add(this);
		try {
			super.run();
		} catch (Throwable e) { // logger.error
			logger.error("", e);
		} finally {
			workers.remove(this);
		}
	}

	private static class WorkerFactory implements ThreadFactory {
		private final String threadNamePrefix;
		private final int priority;
		private final AtomicLong threadNumber = new AtomicLong();

		public WorkerFactory(String executorName, int priority) {
			threadNamePrefix = executorName + '-';
			this.priority = priority;
		}

		@Override
		public Thread newThread(Runnable r) {
			return new ThreadDiagnosable(threadNamePrefix + threadNumber.incrementAndGet(), r, priority);
		}
	}

	/**
	 * 根据给定的名字创建线程工厂。这个工厂创建的线程都会以此名字为前缀命名。 一般用于 ThreadPoolExecutor。
	 *
	 * @param executorName executorName
	 * @return ThreadFactory
	 */
	public static ThreadFactory newFactory(String executorName, int priority) {
		return new WorkerFactory(executorName, priority);
	}

	public static ThreadFactory newFactory(String executorName) {
		return new WorkerFactory(executorName, Thread.NORM_PRIORITY);
	}
}
