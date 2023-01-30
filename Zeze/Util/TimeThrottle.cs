
using System;
using System.Collections.Generic;
using System.Runtime.CompilerServices;

namespace Zeze.Util;

public abstract class TimeThrottle
{
	public abstract bool MarkNow();
	public abstract void Close();

	public static TimeThrottle Create(Zeze.Net.SocketOptions options)
	{
		return Create(options.TimeThrottle, options.TimeThrottleSeconds, options.TimeThrottleLimit);
	}

	public static TimeThrottle Create(string name, int? seconds, int? limit)
	{
		if (string.IsNullOrEmpty(name) || seconds == null || limit == null)
			return null;
		switch (name)
		{
			case "queue":
				return new TimeThrottleQueue(seconds.Value, limit.Value);

			case "counter":
				return new TimeThrottleCounter(seconds.Value, limit.Value);
		}
		throw new Exception("unknown time throttle " + name);
	}
}

public class TimeThrottleQueue : TimeThrottle
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
	public TimeThrottleQueue(int seconds, int limit)
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
	public override bool MarkNow()
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

    public override void Close()
    {
    }
}

public class TimeThrottleCounter : TimeThrottle
{
	private readonly int limit;
	private int count;
	private SchedulerTask timer;

	public TimeThrottleCounter(int seconds, int limit)
	{
		this.limit = limit;
		timer = Scheduler.Schedule(onTimer, seconds * 1000, seconds * 1000);
	}

	private void onTimer(SchedulerTask ThisTask)
	{
		count = 0;
	}

	public override bool MarkNow()
	{
		++count;
		return count < limit;
	}

    public override void Close()
    {
		timer.Cancel();
    }
}