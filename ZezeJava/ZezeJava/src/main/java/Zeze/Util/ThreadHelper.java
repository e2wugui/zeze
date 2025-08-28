package Zeze.Util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class ThreadHelper extends Thread {
	private static final @NotNull Logger logger = LogManager.getLogger(ThreadHelper.class);

	private volatile boolean running = true;
	private boolean idle = true;
	private final ReentrantLock thisLock = new ReentrantLock();
	private final Condition thisCond = thisLock.newCondition();

	public ThreadHelper(@NotNull String name) {
		super(name);
		this.setDaemon(true);
	}

	public ThreadHelper(@NotNull String name, boolean daemon) {
		super(name);
		this.setDaemon(daemon);
	}

	public ThreadHelper(@NotNull Runnable task, @NotNull String name, boolean daemon) {
		super(task, name);
		this.setDaemon(daemon);
	}

	public final boolean isRunning() {
		return running;
	}

	public @NotNull Lock getThisLock() {
		return thisLock;
	}

	public @NotNull Condition getThisCond() {
		return thisCond;
	}

	/**
	 * 忽略中断异常，必须等待线程结束。
	 */
	public final void joinAssuring() {
		while (true) {
			try {
				join();
				break;
			} catch (Exception ex) {
				logger.warn("{} shutdown. ignore ex:", getClass().getName(), ex);
			}
		}
	}

	public void shutdown() {
		running = false;
		wakeup();
		joinAssuring();
	}

	public void wakeup() {
		thisLock.lock();
		try {
			idle = false;
			thisCond.signal();
		} finally {
			thisLock.unlock();
		}
	}

	/**
	 * 挂起当前线程一段时间。
	 * <p>
	 * 通过 shutdown 或者 wakeup 打断。
	 */
	public final void sleepIdle(long ms) {
		thisLock.lock();
		try {
			if (idle) {
				if (!thisCond.await(ms, TimeUnit.MILLISECONDS))
					logger.trace("await {} ms timeout", ms);
			}
		} catch (InterruptedException ex) {
			logger.warn("{} sleepOut. ex:", getClass().getName(), ex);
		} finally {
			idle = true;
			thisLock.unlock();
		}
	}

	public @NotNull Runnable cock() {
		return ThreadHelper.this::wakeup;
	}
}
