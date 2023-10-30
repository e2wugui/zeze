package Zeze.Util;

import Zeze.Net.SocketOptions;

public interface TimeThrottle extends AutoCloseable {
	boolean checkNow(int size);

	@SuppressWarnings("override")
	default void close() {
	}

	static TimeThrottle create(SocketOptions options) {
		return create(options.getTimeThrottle(), options.getTimeThrottleSeconds(),
				options.getTimeThrottleLimit(), options.getTimeThrottleBandwidth());
	}

	static TimeThrottle create(String name, Integer seconds, Integer limit, Integer bandwidth) {
		if (null == name || name.isBlank() || null == seconds || null == limit || null == bandwidth)
			return null;

		switch (name) {
		case "queue":
			return new TimeThrottleQueue(seconds, limit, bandwidth);
		case "counter":
			return new TimeThrottleCounter(seconds, limit, bandwidth);
		}
		throw new UnsupportedOperationException("unknown time throttle " + name);
	}
}
