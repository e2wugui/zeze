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
	private long counter;
	private long bandwidth;
	private final Future<?> timer;
	private final ReentrantLock mutex = new ReentrantLock();

	public TimeThrottleCounter(int seconds, int limit, int bandwidthLimit) {
		TimeThrottle.checkThreshold(seconds, limit, bandwidthLimit);
		this.limit = limit * seconds;
		this.bandwidthLimit = bandwidthLimit * seconds;
		timer = TaskSpec.ofAction(this::onTimer).schedulePeriodNow(seconds * 1000L, seconds * 1000L);
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
			bandwidth += size;
			// 与Queue版对齐——第limit个包计入本窗口并放行、第limit+1个拒绝
			// （不用counter<limit，同配置切换实现名断连临界点会差1）。
			return counter <= limit && Long.compareUnsigned(bandwidth, bandwidthLimit) < 0;
		} finally {
			mutex.unlock();
		}
	}

	@Override
	public void close() {
		timer.cancel(true);
	}
}
