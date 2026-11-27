package Zeze.Util;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;

/**
 * 滑动窗口限流（队列版）：基于单调时钟保留最近 expireNs 纳秒内每个请求的 mark，checkNow 先淘汰过期
 * mark 再比较剩余的数量与带宽，窗口随请求精确滑动，没有 Counter 版的边界突刺，且不受系统墙钟回拨影响；
 * 代价是每个请求一个 mark 的内存。marks 超过 {@link #eMaxMarksSize} 时拒绝新 mark，
 * 防止恶意请求撑爆队列。
 */
public class TimeThrottleQueue implements TimeThrottle {
	public static final int eMaxMarksSize = 4096;

	private final ArrayDeque<Packet> marks = new ArrayDeque<>();
	private final long expireNs;
	private final int limit;
	private final int bandwidthLimit;
	private final LongSupplier nanoClock;
	private long bandwidth;
	private final ReentrantLock mutex = new ReentrantLock();

	public record Packet(long timestamp, int size) {
	}

	/**
	 * seconds 秒内限制 limit 个 mark。
	 *
	 * @param seconds 限制时间范围
	 * @param limit   限制数量
	 */
	public TimeThrottleQueue(int seconds, int limit, int bandwidthLimit) {
		this(seconds, limit, bandwidthLimit, System::nanoTime);
	}

	TimeThrottleQueue(int seconds, int limit, int bandwidthLimit, LongSupplier nanoClock) {
		TimeThrottle.checkThreshold(seconds, limit, bandwidthLimit);
		this.expireNs = seconds * 1_000_000_000L;
		this.limit = limit * seconds;
		this.bandwidthLimit = bandwidthLimit * seconds;
		this.nanoClock = Objects.requireNonNull(nanoClock);
	}

	/**
	 * 记录当前请求并检查是否超过数量或带宽限制。
	 *
	 * @return true 表示放行，false 表示超限
	 */
	@Override
	public boolean checkNow(int size) {
		mutex.lock();
		try {
			var now = nanoClock.getAsLong();
			for (var t = marks.peek(); t != null; t = marks.peek()) {
				if (now - t.timestamp < expireNs)
					break;
				bandwidth -= t.size;
				marks.poll();
			}
			if (marks.size() > eMaxMarksSize)
				return false; // 防止客户端发送大量请求，造成marks过大，此时不加入mark。
			bandwidth += size;
			marks.offer(new Packet(now, size)); // 不重新读取now了。
			return marks.size() <= limit && Long.compareUnsigned(bandwidth, bandwidthLimit) < 0;
		} finally {
			mutex.unlock();
		}
	}
}
