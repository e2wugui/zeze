package Zeze.Util;

import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;

/** 秒级滑动窗口计数器：按秒轮转桶统计最近数秒内的计数。 */
public class TimeCounter extends ReentrantLock implements AutoCloseable {
	private final CounterSecond[] counters; // 5-10个。
	private int lastIndex;
	private final @Nullable TimerFuture<Long> discardTimer;

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

	public TimeCounter(int seconds, boolean enableDiscardTask) {
		if (seconds <= 0)
			throw new IllegalArgumentException("seconds = " + seconds + " <= 0");
		counters = new CounterSecond[seconds];
		for (var i = 0; i < counters.length; ++i)
			counters[i] = new CounterSecond();

		// 保存定时任务句柄，供 close() 取消以防泄漏
		discardTimer = enableDiscardTask
				? TaskSpec.ofAction(this::discard).schedulePeriodNow(Random.getInstance().nextLong(1000), 1000)
				: null;
	}

	/** 取消每秒清理定时任务，实例生命周期结束时须调用。 */
	@Override
	public void close() {
		if (discardTimer != null)
			discardTimer.cancel(false);
	}

	public void discard() {
		discard(GlobalTimer.getCurrentSeconds());
	}

	public void discard(long nowSeconds) {
		lock();
		try {
			// 墙钟回拨后，环上的写入顺序不再等于时间顺序。
			for (var bucket : counters) {
				if (nowSeconds - bucket.seconds > counters.length)
					bucket.counter = 0;
			}
		} finally {
			unlock();
		}
	}
}
