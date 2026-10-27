package Zeze.Util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// 可停循环线程基类：isRunning/sleepIdle 控制生命周期，派生类实现 run 主体
public class ThreadHelper extends Thread {
	private static final Logger logger = LogManager.getLogger(ThreadHelper.class);

	private volatile boolean running = true;
	private boolean idle = true;
	private final ReentrantLock thisLock = new ReentrantLock();
	private final Condition thisCond = thisLock.newCondition();

	public ThreadHelper(String name) {
		super(name);
		this.setDaemon(true);
	}

	public ThreadHelper(String name, boolean daemon) {
		super(name);
		this.setDaemon(daemon);
	}

	public ThreadHelper(Runnable task, String name, boolean daemon) {
		super(task, name);
		this.setDaemon(daemon);
	}

	public final boolean isRunning() {
		return running;
	}

	public Lock getThisLock() {
		return thisLock;
	}

	public Condition getThisCond() {
		return thisCond;
	}

	/**
	 * 忽略中断异常，必须等待线程结束。
	 * @throws IllegalStateException 当前线程不能等待自身结束。
	 */
	public final void joinAssuring() {
		if (Thread.currentThread() == this)
			throw new IllegalStateException("A thread cannot await its own termination");
		while (true) {
			try {
				join();
				break;
			} catch (Exception ex) {
				logger.warn("{} shutdown. ignore ex:", getClass().getName(), ex);
			}
		}
	}

	/** 请求停止并唤醒线程；可在线程自身或其他线程调用，不等待退出。 */
	public final void requestShutdown() {
		running = false;
		wakeup();
	}

	/** 请求停止；外部调用等待退出，线程自身调用在发布停止请求后返回。 */
	public void shutdown() {
		requestShutdown();
		if (Thread.currentThread() != this)
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
			Thread.currentThread().interrupt();
		} finally {
			idle = true;
			thisLock.unlock();
		}
	}

	public Runnable cock() {
		return ThreadHelper.this::wakeup;
	}
}
