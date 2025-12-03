package Zeze.Util;

import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

public class TimeThrottleCounter implements TimeThrottle {
	private final int limit;
	private final int bandwidthLimit;
	private int counter;
	private int bandwidth;
	private final Future<?> timer;
	private final ReentrantLock mutex = new ReentrantLock();

	public TimeThrottleCounter(int seconds, int limit, int bandwidthLimit) {
		if (seconds < 1 || limit < 0 || bandwidthLimit < 0)
			throw new IllegalArgumentException();

		this.limit = limit * seconds;
		this.bandwidthLimit = bandwidthLimit * seconds;
		timer = Task.scheduleUnsafe(seconds * 1000L, seconds * 1000L, this::onTimer);
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
