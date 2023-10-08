package Zeze.Util;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用于几秒内的计数，
 * 每秒删除队头的计数，
 */
public class TimeCounter {
	private final ArrayDeque<CounterSecond> counters = new ArrayDeque<>(); // 5-10个。
	public static class CounterSecond {
		private final long timestamp;
		private final AtomicLong counter = new AtomicLong();
		public CounterSecond(long now) {
			timestamp = now;
			counter.getAndSet(1);
		}
	}

	public void increace(long now) {
		if (counters.isEmpty()) {
			counters.add(new CounterSecond(now));
		} else if (now / 1000 == counters.getLast().timestamp / 1000) {
			counters.getLast().counter.incrementAndGet();
		} else {
			counters.add(new CounterSecond(now));
		}
	}

	public long count() {
		var count = 0L;
		for (var c : counters)
			count += c.counter.get();
		return count;
	}

	public TimeCounter() {
		// 目前这个用于provider，数量不会很多，简单起见，每个计数启用一个定时任务。
		Task.scheduleUnsafe(Random.getInstance().nextLong(1000), 1000, this::discardHead);
	}

	public void discardHead() {
		counters.poll();
	}
}
