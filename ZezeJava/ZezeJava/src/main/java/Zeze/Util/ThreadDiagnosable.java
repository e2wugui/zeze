package Zeze.Util;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ThreadDiagnosable extends Thread {
	private static final Logger logger = LogManager.getLogger(ThreadDiagnosable.class);
	private static final ConcurrentHashMap<Long, ThreadDiagnosable> workers = new ConcurrentHashMap<>();

	// 这个线程池子绝对不能饥饿，使用独立的。
	private static ExecutorService diagnoseExecutor;
	private final HashSet<Timeout> timeouts = new HashSet<>();
	private final ArrayList<Critical> criticalStack = new ArrayList<>();

	private static final AtomicLong startSerial = new AtomicLong();

	// 修改period，请重新调用。
	public static void startDiagnose(long period) {
		var current = startSerial.incrementAndGet();
		synchronized (workers) {
			if (null == diagnoseExecutor)
				diagnoseExecutor = Executors.newCachedThreadPool(new ThreadFactoryWithName("diagnoseExecutor"));
			diagnoseExecutor.execute(() -> {
				while (current == startSerial.get()) {
					try {
						Thread.sleep(period); // todo 可配置。
						var now = System.currentTimeMillis();
						for (var worker : workers.values()) {
							worker.onTimer(now);
						}
					} catch (Throwable ex) {
						logger.error("", ex);
					}
				}
			});
		}
	}

	// 停止诊断检测。
	public static void stopDiagnose() {
		synchronized (workers) {
			startSerial.incrementAndGet(); // 让正在运行的诊断任务退出。
			diagnoseExecutor.shutdown();
			diagnoseExecutor = null;
		}
	}

	void diagnose() {
		ThreadDiagnosable.this.interrupt();
		// todo more more ...
	}

	public Timeout createTimeout(long timeout) {
		var t = new Timeout(timeout);
		timeouts.add(t);
		return t;
	}

	public Critical enterCritical(boolean critical) {
		var c = new Critical(critical);
		criticalStack.add(c);
		return c;
	}

	public class Critical implements Closeable {
		final boolean critical;

		public Critical(boolean critical) {
			this.critical = critical;
		}

		@Override
		public void close() {
			// leave critical
			criticalStack.remove(criticalStack.size() - 1);
		}
	}
	public void onTimer(long now) {
		for (var timeout : timeouts) {
			if (timeout.timeoutTime > now) {
				// 每个timeout仅触发一次.
				timeouts.remove(timeout);
				diagnoseExecutor.execute(timeout);
				break; // 开始诊断,就不需要继续了. 【如果需要继续检查，要注意此时在遍历timeouts】。
			}
		}
	}

	public class Timeout implements Closeable, Runnable {
		final long timeoutTime;
		public Timeout(long timeout) {
			this.timeoutTime = System.currentTimeMillis() + timeout;
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

	private ThreadDiagnosable(String executorName, Runnable r) {
		super(r);
		this.setDaemon(true);
		this.setName(executorName + "." + this.getId());
	}

	@Override
	public void run() {
		workers.put(getId(), this);
		try {
			super.run();
		} catch (Throwable ex) {
			logger.error("", ex);
		} finally {
			workers.remove(getId());
		}
	}

	private static class WorkerFactory implements ThreadFactory {
		private final String executorName;

		public WorkerFactory(String executorName) {
			this.executorName = executorName;
		}

		@Override
		public Thread newThread(Runnable r) {
			return new ThreadDiagnosable(executorName, r);
		}
	};

	/**
	 * 根据给定的名字创建线程工厂。这个工厂创建的线程都会以此名字为前缀命名。 一般用于 ThreadPoolExecutor。
	 *
	 * @param executorName executorName
	 * @return ThreadFactory
	 */
	public static ThreadFactory newFactory(String executorName) {
		return new WorkerFactory(executorName);
	}
}
