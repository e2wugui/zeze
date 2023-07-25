package Zeze.Util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 低精度超时定时器实现。当延迟到达时执行任务。特性：
 * <ul>
 * <li><b>专用的定时器实现，not well-defined.</b>
 * <li><b>不适合用来调度大量同时存在的超时任务。现在仅用于实现 xdb.Executor 的超时保护。</b>
 * <li>适用于超时任务实际上很少发生的情形。
 * <li>低精度。秒级。
 * <li>采用ConcurrentMap实现，提高并发性。
 * <li>积极回收内存（尽快释放对象引用），remove 即 cancel。
 * <li>超时发生时，直接在调度线程中执行。所以目标任务必须是很快完成的。
 * <li>调度目标Runnable最好不重载实现Object的hashCode和equals，否则可能对效率造成较大影响。
 * <li>超时任务不使用接口Runnable，而是定义新的接口Timeout，这样便于在同一个类中实现。
 * </ul>
 */
public final class TimeoutManager {
	private static final Logger logger = LogManager.getLogger(TimeoutExecutor.class);

	public static final TimeoutManager instance = new TimeoutManager();
	public Future<?> timer;

	public void start() {
		start(30_000); // 30 seconds
	}

	public synchronized void start(long period) {
		if (null != timer)
			timer.cancel(true);
		timer = Task.scheduleUnsafe(Random.getInstance().nextLong(period), period, this::onTimer);
	}

	public synchronized void stop() {
		if (null != timer)
			timer.cancel(true);
		timer = null;
	}

	private final ConcurrentMap<Timeout, Long> tasks = new ConcurrentHashMap<>();

	public interface Timeout {
		/**
		 * 超时发生时回调这个函数。仅限于实现一些简单快捷的操作。
		 */
		void onTimeout();
	}

	/**
	 * 调度超时任务。
	 * 
	 * @param task  执行目标。
	 * @param timeout 单位为毫秒。虽然实际精度是秒级。
	 * @return 返回 target。
	 */
	public Timeout schedule(Timeout task, long timeout) {
		if (task == null)
			throw new NullPointerException();

		if (timeout < 0)
			timeout = 0;

		tasks.put(task, System.currentTimeMillis() + timeout);
		return task;
	}

	public boolean remove(Timeout target) {
		return null != tasks.remove(target);
	}

	private void onTimer() {
		long now = System.currentTimeMillis();
		for (Entry<Timeout, Long> e : tasks.entrySet()) {
			final Timeout target = e.getKey();
			if (e.getValue() <= now && remove(target)) {
				try {
					target.onTimeout();
				} catch (Throwable x) {
					logger.error("", x);
				}
			}
		}
	}
}
