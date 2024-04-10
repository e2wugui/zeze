package Zeze.Util;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 用于几秒内的计数，
 * 每秒删除队头的计数，
 */
public class TimeCounter extends ReentrantLock {
	private final CounterSecond[] counters; // 5-10个。
	private int lastIndex;

	public static class CounterSecond {
		private long seconds;
		private int counter;
	}

	public void increment() {
		increment(GlobalTimer.getCurrentSeconds());
	}

	public void increment(long nowSeconds) {
		lock();
		try {
			var last = counters[lastIndex];
			if (nowSeconds == last.seconds) {
				// 同一秒，简单计数
				last.counter++;
			} else {
				if (++lastIndex >= counters.length)
					lastIndex = 0;
				last = counters[lastIndex];
				last.seconds = nowSeconds;
				last.counter = 1;
			}
		} finally {
			unlock();
		}
	}

	public long count() {
		lock();
		try {
			var count = 0L;
			for (var c : counters)
				count += c.counter;
			return count;
		} finally {
			unlock();
		}
	}

	public TimeCounter(int seconds) {
		this(seconds, true);
	}

	// for debug
	public TimeCounter(int seconds, boolean enableDiscardTask) {
		if (seconds <= 0)
			throw new IllegalArgumentException("seconds = " + seconds + " <= 0");
		counters = new CounterSecond[seconds];
		for (var i = 0; i < counters.length; ++i)
			counters[i] = new CounterSecond();

		// 目前这个用于provider，数量不会很多，简单起见，每个计数启用一个定时任务。
		if (enableDiscardTask)
			Task.scheduleUnsafe(Random.getInstance().nextLong(1000), 1000, this::discard);
	}

	public void discard() {
		discard(GlobalTimer.getCurrentSeconds());
	}

	public void discard(long nowSeconds) {
		lock();
		try {
			var headIndex = lastIndex;

			for (var i = 0; i < counters.length; ++i) {
				if (++headIndex >= counters.length)
					headIndex = 0;
				var head = counters[headIndex];
				if (nowSeconds - head.seconds <= counters.length)
					break;

				// reset
				// 这个结合上面的if，会导致不必要的reset；这里用Long.MAX_VALUE，可以避免多余的reset，
				// 但要求记住headIndex，不能从lastIndex+1开始判断。
				head.seconds = 0;
				head.counter = 0;
			}
		} finally {
			unlock();
		}
	}
}
