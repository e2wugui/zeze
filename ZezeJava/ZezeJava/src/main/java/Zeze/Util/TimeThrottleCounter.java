package Zeze.Util;

import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 固定窗口限流（计数器版）：定时器每 seconds 秒把计数清零，checkNow 递增后与窗口内总量比较。
 * 与 Queue 版相比没有 per-request 内存、判断 O(1)，但窗口整段跳变，窗口切换瞬间最多
 * 放过两倍限额（边界突刺），适合登录队列这类按秒粗限的场景。
 */
public class TimeThrottleCounter implements TimeThrottle {
	private final int limit;
	private final int bandwidthLimit;
	private int counter;
	private int bandwidth;
	private final Future<?> timer;
	private final ReentrantLock mutex = new ReentrantLock();

	public TimeThrottleCounter(int seconds, int limit, int bandwidthLimit) {
		checkThreshold(seconds, limit, bandwidthLimit);
		this.limit = limit * seconds;
		this.bandwidthLimit = bandwidthLimit * seconds;
		timer = TaskSpec.ofAction(this::onTimer).schedulePeriodNow(seconds * 1000L, seconds * 1000L);
	}

	/**
	 * 只负责校验并抛异常，乘法由构造器自己做。seconds*1000、limit*seconds、
	 * bandwidthLimit*seconds 都必须保持在 int 正数范围内：溢出回绕成负值后阈值
	 * 静默失真，checkNow 的 {@code counter < limit} 恒 false，退化为全拒绝。
	 */
	private static void checkThreshold(int seconds, int limit, int bandwidthLimit) {
		if (seconds < 1 || limit < 0 || bandwidthLimit < 0)
			throw new IllegalArgumentException();
		if (seconds > Integer.MAX_VALUE / 1000
				|| limit > Integer.MAX_VALUE / seconds
				|| bandwidthLimit > Integer.MAX_VALUE / seconds)
			throw new IllegalArgumentException("TimeThrottleCounter overflow: seconds=" + seconds
					+ " limit=" + limit + " bandwidthLimit=" + bandwidthLimit);
	}

	private void onTimer() {
		mutex.lock();
		try {
			counter = 0;
			bandwidth = 0;
		} finally {
			mutex.unlock();
		}
	}

	@Override
	public boolean checkNow(int size) {
		mutex.lock();
		try {
			++counter;
			bandwidth += size; // 变成负数以后一直失败。
			return counter < limit && Integer.compareUnsigned(bandwidth, bandwidthLimit) < 0;
		} finally {
			mutex.unlock();
		}
	}

	@Override
	public void close() {
		timer.cancel(true);
	}
}
