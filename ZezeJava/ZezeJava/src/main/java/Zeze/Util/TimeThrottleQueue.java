package Zeze.Util;

import java.util.ArrayDeque;

public class TimeThrottleQueue implements TimeThrottle {
	public static final int eMaxMarksSize = 4096;

	private final ArrayDeque<Packet> marks = new ArrayDeque<>();
	private final int expire;
	private final int limit;
	private final int bandwidthLimit;
	private int bandwidth;

	public static final class Packet {
		public final long timestamp;
		public final int size;

		public Packet(long t, int s) {
			timestamp = t;
			size = s;
		}
	}

	/**
	 * seconds 秒内限制 limit 个 mark。
	 *
	 * @param seconds 限制时间范围。
	 * @param limit   限制数量。
	 */
	public TimeThrottleQueue(int seconds, int limit, int bandwidthLimit) {
		if (seconds < 1 || limit < 1 || bandwidthLimit < 1)
			throw new IllegalArgumentException();
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
		var now = System.currentTimeMillis();
		var start = now - expire;
		for (var t = marks.peek(); null != t; t = marks.peek()) {
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
	}
}
