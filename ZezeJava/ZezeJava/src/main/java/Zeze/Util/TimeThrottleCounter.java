package Zeze.Util;

import java.util.concurrent.Future;

public class TimeThrottleCounter implements TimeThrottle {
	private final int limit;
	private final int bandwidthLimit;
	private int counter;
	private int bandwidth;
	private final Future<?> timer;

	public TimeThrottleCounter(int seconds, int limit, int bandwidthLimit) {
		if (seconds < 1 || limit < 1 || bandwidthLimit < 1)
			throw new IllegalArgumentException();

		this.limit = limit * seconds;
		this.bandwidthLimit = bandwidthLimit * seconds;
		timer = Task.scheduleUnsafe(seconds * 1000L, seconds * 1000L, this::onTimer);
	}

	private void onTimer() {
		counter = 0;
		bandwidth = 0;
	}

	@Override
	public boolean checkNow(int size) {
		++counter;
		bandwidth += size; // 变成负数以后一直失败。
		return counter < limit && Integer.compareUnsigned(bandwidth, bandwidthLimit) < 0;
	}

	@Override
	public void close() {
		timer.cancel(true);
	}
}
