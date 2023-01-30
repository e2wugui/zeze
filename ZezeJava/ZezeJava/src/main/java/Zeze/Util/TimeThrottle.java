package Zeze.Util;

public abstract class TimeThrottle {
	public abstract boolean markNow();
	public abstract void close();

	public static TimeThrottle create(Zeze.Net.SocketOptions options) {
		return create(options.getTimeThrottle(), options.getTimeThrottleSeconds(), options.getTimeThrottleLimit());
	}

	public static TimeThrottle create(String name, Integer seconds, Integer limit) {
		if (null == name || name.isBlank() || null == seconds || null == limit)
			return null;

		switch (name)
		{
		case "queue":
			return new TimeThrottleQueue(seconds, limit);
		case "counter":
			return new TimeThrottleCounter(seconds, limit);
		}
		throw new RuntimeException("unknown time throttle " + name);
	}
}
