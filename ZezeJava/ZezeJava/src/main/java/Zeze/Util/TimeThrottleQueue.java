package Zeze.Util;

import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 滑动窗口限流（队列版）：保留最近 expire 毫秒内每个请求的 mark，checkNow 先淘汰过期
 * mark 再比较剩余的数量与带宽，窗口随请求精确滑动，没有 Counter 版的边界突刺；
 * 代价是每个请求一个 mark 的内存。marks 超过 {@link #eMaxMarksSize} 时拒绝新 mark，
 * 防止恶意请求撑爆队列。
 */
public class TimeThrottleQueue implements TimeThrottle {
	public static final int eMaxMarksSize = 4096;

	private final ArrayDeque<Packet> marks = new ArrayDeque<>();
	private final int expire;
	private final int limit;
	private final int bandwidthLimit;
	private int bandwidth;
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
		checkThreshold(seconds, limit, bandwidthLimit);
		this.expire = seconds * 1000;
		this.limit = limit * seconds;
		this.bandwidthLimit = bandwidthLimit * seconds;
	}

	/**
	 * 只负责校验并抛异常，乘法由构造器自己做。seconds*1000、limit*seconds、
	 * bandwidthLimit*seconds 都必须保持在 int 正数范围内：溢出回绕成负值后阈值
	 * 静默失真，expire 为负会让 checkNow 的过期清理循环立即 break
	 * （marks 永不淘汰），退化为恒 return false 的全拒绝。
	 */
	private static void checkThreshold(int seconds, int limit, int bandwidthLimit) {
		if (seconds < 1 || limit < 0 || bandwidthLimit < 0)
			throw new IllegalArgumentException();
		if (seconds > Integer.MAX_VALUE / 1000
				|| limit > Integer.MAX_VALUE / seconds
				|| bandwidthLimit > Integer.MAX_VALUE / seconds)
			throw new IllegalArgumentException("TimeThrottleQueue overflow: seconds=" + seconds
					+ " limit=" + limit + " bandwidthLimit=" + bandwidthLimit);
	}

	/**
	 * mark with current time
	 *
	 * @return false if overflow
	 */
	@Override
	public boolean checkNow(int size) {
		mutex.lock();
		try {
			var now = System.currentTimeMillis();
			var start = now - expire;
			for (var t = marks.peek(); t != null; t = marks.peek()) {
				if (t.timestamp > start)
					break;
				bandwidth -= t.size;
				marks.poll();
			}
			if (marks.size() > eMaxMarksSize)
				return false; // 防止客户端发送大量请求，造成marks过大，此时不加入mark。
			bandwidth += size; // 变成负数以后一直失败。
			marks.offer(new Packet(now, size)); // 不重新读取now了。
			return marks.size() <= limit && Integer.compareUnsigned(bandwidth, bandwidthLimit) < 0;
		} finally {
			mutex.unlock();
		}
	}
}
