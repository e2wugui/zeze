package Zeze.Util;

import java.util.ArrayDeque;

public class TimeThrottleQueue extends TimeThrottle {
	public static final int eMaxMarksSize = 4096;
	private final ArrayDeque<Long> marks = new ArrayDeque<>();
	private final int expire;
	private final int limit;

	/**
	 * seconds 秒内限制 limit 个 mark。
	 * @param seconds 限制时间范围。
	 * @param limit 限制数量。
	 */
	public TimeThrottleQueue(int seconds, int limit) {
		if (seconds < 1)
			throw new IllegalArgumentException();
		if (limit < 1)
			throw new IllegalArgumentException();
		this.expire = seconds * 1000;
		this.limit = limit;
	}

	/**
	 * mark with current time
	 * @return false if overflow
	 */
	@Override
	public boolean markNow() {
		var now = System.currentTimeMillis();
		var start = now - expire;
		for (var t = marks.peek(); null != t; t = marks.peek()) {
			if (t > start)
				break;
			marks.poll();
		}
		if (marks.size() > eMaxMarksSize)
			return false; // 防止客户端发送大量请求，造成marks过大，此时不加入mark。
		marks.offer(now); // 不重新读取now了。
		return marks.size() <= limit;
	}

	@Override
	public void close() {

	}
}
