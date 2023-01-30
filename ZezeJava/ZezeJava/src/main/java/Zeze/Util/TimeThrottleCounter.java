package Zeze.Util;

import java.util.concurrent.Future;

public class TimeThrottleCounter extends TimeThrottle {
	private final int limit;
	private int counter;
	private Future<?> timer;

	public TimeThrottleCounter(int seconds, int limit) {
		this.limit = limit;
		timer = Task.scheduleUnsafe(seconds * 1000L, seconds * 1000L, this::onTimer);
	}

	private void onTimer() {
		counter = 0;
	}

	@Override
	public boolean markNow() {
		++counter;
		return counter < limit;
	}

	@Override
	public void close() {
		timer.cancel(true);
	}
}
