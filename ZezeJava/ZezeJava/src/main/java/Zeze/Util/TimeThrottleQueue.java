package Zeze.Util;

import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantLock;

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
		if (seconds < 1 || limit < 0 || bandwidthLimit < 0)
			throw new IllegalArgumentException();
		// 三个乘积都必须保持在 int 正数范围内：溢出回绕后 expire 为负会让 checkNow 的过期清理
		// 循环立即break(marks永不淘汰，退化为恒return false的全拒绝)，limit/bandwidthLimit 同理静默失真。
		if (seconds > Integer.MAX_VALUE / 1000
				|| limit > Integer.MAX_VALUE / seconds
				|| bandwidthLimit > Integer.MAX_VALUE / seconds)
			throw new IllegalArgumentException("TimeThrottleQueue overflow: seconds=" + seconds
					+ " limit=" + limit + " bandwidthLimit=" + bandwidthLimit);
		this.expire = seconds * 1000;
		this.limit = limit * seconds;
		this.bandwidthLimit = bandwidthLimit * seconds;
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
