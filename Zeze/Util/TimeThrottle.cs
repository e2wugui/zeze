
using System;
using System.Collections.Generic;

namespace Zeze.Util;

public class TimeThrottle
{
	public const int eMaxMarksSize = 4096;
	private readonly Queue<long> marks = new();
	private readonly int expire;
	private readonly int limit;

	/**
	 * seconds 秒内限制 limit 个 mark。
	 * @param seconds 限制时间范围。
	 * @param limit 限制数量。
	 */
	public TimeThrottle(int seconds, int limit)
	{
		if (seconds < 1)
			throw new ArgumentException();
		if (limit < 1)
			throw new ArgumentException();
		this.expire = seconds * 1000;
		this.limit = limit;
	}

	/**
	 * mark with current time
	 * @return false if overflow
	 */
	public bool MarkNow()
	{
		var now = Time.NowUnixMillis;
		var start = now - expire;
		while (true)
		{
			if (false == marks.TryPeek(out var t))
				break;
            if (t > start)
                break;
            marks.Dequeue();
		}
		if (marks.Count > eMaxMarksSize)
			return false; // 防止客户端发送大量请求，造成marks过大，此时不加入mark。
		marks.Enqueue(now); // 不重新读取now了。
		return marks.Count <= limit;
	}
}
