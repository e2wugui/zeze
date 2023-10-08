package Zeze.Util;

/**
 * 用于几秒内的计数，
 * 每秒删除队头的计数，
 */
public class TimeCounter {
	private final CounterSecond[] counters; // 5-10个。
	private int lastIndex;

	public static class CounterSecond {
		private long seconds;
		private int counter;
	}

	public void increment() {
		increment(System.currentTimeMillis());
	}

	public synchronized void increment(long now) {
		var last = counters[lastIndex];
		var nowSeconds = now / 1000;
		if (nowSeconds == last.seconds) {
			// 同一秒，简单计数
			last.counter++;
		} else {
			lastIndex = (lastIndex + 1) % counters.length;
			last = counters[lastIndex];
			last.seconds = nowSeconds;
			last.counter = 1;
		}
	}

	public synchronized long count() {
		var count = 0L;
		for (var c : counters)
			count += c.counter;
		return count;
	}

	public TimeCounter(int seconds) {
		this(seconds, true);
	}

	// for debug
	public TimeCounter(int seconds, boolean enableDiscardTask) {
		counters = new CounterSecond[seconds];
		for (var i = 0; i < counters.length; ++i)
			counters[i] = new CounterSecond();

		// 目前这个用于provider，数量不会很多，简单起见，每个计数启用一个定时任务。
		if (enableDiscardTask)
			Task.scheduleUnsafe(Random.getInstance().nextLong(1000), 1000, this::discard);
	}

	public synchronized void discard() {
		discard(System.currentTimeMillis());
	}

	public synchronized void discard(long now) {
		var nowSeconds = now / 1000;
		var headIndex = lastIndex + 1;
		if (headIndex >= counters.length)
			headIndex = 0;

		for (var i = 0; i < counters.length; ++i) {
			var head = counters[(headIndex + i) % counters.length];
			if (nowSeconds - head.seconds <= counters.length)
				break;

			// reset
			head.seconds = 0; // 这个结合上面的if，会导致不必要的reset；这里用Long.MAX_VALUE，可以避免多余的reset，
							  // 但要求记住headIndex，不能从lastIndex+1开始判断。
			head.counter = 0;
		}
	}
}
