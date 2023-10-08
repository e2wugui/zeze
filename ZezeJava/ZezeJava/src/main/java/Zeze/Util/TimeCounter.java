package Zeze.Util;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用于几秒内的计数，
 * 每秒删除队头的计数，
 */
public class TimeCounter {
	private final CounterSecond[] counters; // 5-10个。
	private int lastIndex;

	public static class CounterSecond {
		private long timestamp;
		private int counter;
	}

	public synchronized void increment() {
		var now = System.currentTimeMillis();
		var last = counters[lastIndex];
		if (now / 1000 == last.timestamp / 1000) {
			// 同一秒，简单计数
			last.counter++;
		} else {
			lastIndex = (lastIndex + 1) % counters.length;
			last = counters[lastIndex];
			last.timestamp = now;
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
		counters = new CounterSecond[seconds];
		for (var i = 0; i < counters.length; ++i)
			counters[i] = new CounterSecond();

		// 目前这个用于provider，数量不会很多，简单起见，每个计数启用一个定时任务。
		Task.scheduleUnsafe(Random.getInstance().nextLong(1000), 1000, this::discard);
	}

	public synchronized void discard() {
		var nowSeconds = System.currentTimeMillis() / 1000;
		var headIndex = lastIndex + 1;
		if (headIndex >= counters.length)
			headIndex = 0;

		for (var i = 0; i < counters.length; ++i) {
			var head = counters[headIndex];
			if (nowSeconds - head.timestamp / 1000 < counters.length)
				break;

			// reset
			head.timestamp = 0;
			head.counter = 0;
		}
	}
}
