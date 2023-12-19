package Zeze.Util;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class ThreadDiagnosable {
	private static final Logger logger = LogManager.getLogger(ThreadDiagnosable.class);
	private static final AtomicLong currentSerial = new AtomicLong();
	private static final ConcurrentHashSet<Timeout> timeouts = new ConcurrentHashSet<>();
	public static volatile boolean disableInterrupt = Reflect.inDebugMode;

	// 修改period，请重新调用。
	public static void startDiagnose(long period) {
		var current = currentSerial.incrementAndGet();
		var thread = new Thread(() -> { // 这个线程绝对不能饥饿，使用独立的
			while (current == currentSerial.get()) {
				if (!disableInterrupt) {
					try {
						var now = System.nanoTime();
						for (var timeout : timeouts) {
							if (timeout.timeoutTime <= now && !Boolean.TRUE.equals(Critical.tlCritical.get())
									&& timeouts.remove(timeout) != null) { // 每个timeout仅触发一次
								//noinspection SynchronizationOnLocalVariableOrMethodParameter
								synchronized (timeout) {
									var t = timeout.thread;
									if (t != null && !t.isInterrupted() && t.isAlive()
											&& t.getPriority() <= Thread.NORM_PRIORITY) {
										logger.warn("INTERRUPT thread '{}' for task timeout", t.getName());
										t.interrupt();
										// more more ...
									}
								}
							}
						}
					} catch (Throwable e) { // logger.error
						logger.error("diagnose exception:", e);
					}
				}
				try {
					//noinspection BusyWait
					Thread.sleep(period);
				} catch (Exception e) {
					logger.info("sleep exception:", e);
					break;
				}
			}
		}, "DiagnoseThread-" + current);
		thread.setDaemon(true);
		thread.setPriority(Thread.NORM_PRIORITY + 2);
		thread.start();
	}

	// 停止诊断检测。
	public static void stopDiagnose() {
		currentSerial.incrementAndGet(); // 让正在运行的诊断任务退出。
	}

	public static final class Timeout implements AutoCloseable {
		private @Nullable Thread thread = Thread.currentThread();
		private final long timeoutTime;

		// 注意必须使用try包装,确保new和close配对
		public Timeout(long timeout) {
			timeoutTime = System.nanoTime() + timeout * 1_000_000;
			timeouts.add(this);
		}

		@Override
		public void close() {
			if (timeouts.remove(this) == null) { // 判断是否已经触发了打断
				synchronized (this) {
					thread = null; // 阻止后续再打断
					//noinspection ResultOfMethodCallIgnored
					Thread.interrupted(); // 清除interrupted标记,避免影响后续任务
				}
			}
		}
	}

	public static final class Critical implements AutoCloseable {
		static final ThreadLocal<Boolean> tlCritical = new ThreadLocal<>();
		private final @Nullable Boolean saved = tlCritical.get();

		// 注意必须使用try包装,确保new和close配对
		public Critical(boolean current) {
			tlCritical.set(current);
		}

		@Override
		public void close() {
			if (saved != null)
				tlCritical.set(saved);
			else
				tlCritical.remove();
		}
	}

	private ThreadDiagnosable() {
	}
}
