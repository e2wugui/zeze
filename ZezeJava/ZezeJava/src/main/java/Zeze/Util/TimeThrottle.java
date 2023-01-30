package Zeze.Util;

public interface TimeThrottle {
	public boolean checkNow(int size);
	public void close();

	public static TimeThrottle create(Zeze.Net.SocketOptions options) {
		return create(options.getTimeThrottle(), options.getTimeThrottleSeconds(),
				options.getTimeThrottleLimit(), options.getTimeThrottleBandwidth());
	}

	public static TimeThrottle create(String name, Integer seconds, Integer limit, Integer bandwidth) {
		if (null == name || name.isBlank() || null == seconds || null == limit || null == bandwidth)
			return null;

		switch (name)
		{
		case "queue":
			return new TimeThrottleQueue(seconds, limit, bandwidth);
		case "counter":
			return new TimeThrottleCounter(seconds, limit, bandwidth);
		}
		throw new RuntimeException("unknown time throttle " + name);
	}
}
