
using System;
using System.Collections.Generic;
using System.Runtime.CompilerServices;

namespace Zeze.Util;

public abstract class TimeThrottle
{
	public abstract bool CheckNow(int size);
	public abstract void Close();

	public static TimeThrottle Create(Zeze.Net.SocketOptions options)
	{
		return Create(options.TimeThrottle, options.TimeThrottleSeconds, options.TimeThrottleLimit, options.TimeThrottleBandwitdh);
	}

	public static TimeThrottle Create(string name, int? seconds, int? limit, int? bandwidth)
	{
		if (string.IsNullOrEmpty(name) || seconds == null || limit == null || bandwidth == null)
			return null;
		switch (name)
		{
			case "queue":
				return new TimeThrottleQueue(seconds.Value, limit.Value, bandwidth.Value);

			case "counter":
				return new TimeThrottleCounter(seconds.Value, limit.Value, bandwidth.Value);
		}
		throw new Exception("unknown time throttle " + name);
	}
}

public class TimeThrottleQueue : TimeThrottle
{
	public const int eMaxMarksSize = 4096;
	private readonly Queue<(long, int)> marks = new();
	private readonly int expire;
	private readonly int limit;
	private readonly int bandwidthLimit;
	private int bandwidth;

	/**
	 * seconds 秒内限制 limit 个 mark。
	 * @param seconds 限制时间范围。
	 * @param limit 限制数量。
	 */
	public TimeThrottleQueue(int seconds, int limit, int bandwidthLimit)
	{
		if (seconds < 1 || limit < 1 || bandwidthLimit < 1)
			throw new ArgumentException();
		this.expire = seconds * 1000;
		this.limit = limit * seconds;
		this.bandwidthLimit = bandwidthLimit * seconds;
	}

	/**
	 * mark with current time
	 * @return false if overflow
	 */
	public override bool CheckNow(int size)
	{
		var now = Time.NowUnixMillis;
		var start = now - expire;
		while (true)
		{
			if (false == marks.TryPeek(out var t))
				break;
            if (t.Item1 > start)
                break;
			bandwidth -= t.Item2;
            marks.Dequeue();
		}
		if (marks.Count > eMaxMarksSize)
			return false; // 防止客户端发送大量请求，造成marks过大，此时不加入mark。
		bandwidth += size; // 变成负数以后一直失败。
        marks.Enqueue((now, size)); // 不重新读取now了。
		return marks.Count <= limit && (bandwidth >= 0 && bandwidth < bandwidthLimit);
	}

    public override void Close()
    {
    }
}

public class TimeThrottleCounter : TimeThrottle
{
	private readonly int limit;
	private readonly int bandwidthLimit;
	private int count;
	private int bandwitdh;
	private readonly SchedulerTask timer;

	public TimeThrottleCounter(int seconds, int limit, int bandwidthLimit)
	{
		this.limit = limit * seconds;
		this.bandwidthLimit = bandwidthLimit * seconds;
		timer = Scheduler.Schedule(onTimer, seconds * 1000, seconds * 1000);
	}

	private void onTimer(SchedulerTask ThisTask)
	{
		count = 0;
		bandwitdh = 0;
	}

	public override bool CheckNow(int size)
	{
		++count;
		bandwitdh += size; // 变成负数以后一直失败。
		return count < limit && (bandwitdh >= 0 && bandwitdh < bandwidthLimit);
	}

    public override void Close()
    {
		timer.Cancel();
    }
}