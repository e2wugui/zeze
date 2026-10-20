package Zeze.Util;

import Zeze.Net.SocketOptions;
import org.jspecify.annotations.Nullable;

// 限流器接口：按配置名创建 queue（滑动窗口）或 counter（固定窗口）实现
public interface TimeThrottle extends AutoCloseable {
	boolean checkNow(int size);

	@Override
	default void close() {
	}

	/** 两种窗口共用配置边界；累计流量可超过阈值，必须另用 long 保存。 */
	static void checkThreshold(int seconds, int limit, int bandwidthLimit) {
		if (seconds < 1 || limit < 0 || bandwidthLimit < 0)
			throw new IllegalArgumentException();
		if (seconds > Integer.MAX_VALUE / 1000
				|| limit > Integer.MAX_VALUE / seconds
				|| bandwidthLimit > Integer.MAX_VALUE / seconds)
			throw new IllegalArgumentException("TimeThrottle overflow: seconds=" + seconds
					+ " limit=" + limit + " bandwidthLimit=" + bandwidthLimit);
	}

	static @Nullable TimeThrottle create(SocketOptions options) {
		return create(options.getTimeThrottle(), options.getTimeThrottleSeconds(),
				options.getTimeThrottleLimit(), options.getTimeThrottleBandwidth());
	}

	static @Nullable TimeThrottle create(@Nullable String name, @Nullable Integer seconds, @Nullable Integer limit,
										 @Nullable Integer bandwidth) {
		if (name == null || name.isBlank() || seconds == null || limit == null || bandwidth == null)
			return null;

		return switch (name) {
			case "queue" -> new TimeThrottleQueue(seconds, limit, bandwidth);
			case "counter" -> new TimeThrottleCounter(seconds, limit, bandwidth);
			default -> throw new UnsupportedOperationException("unknown time throttle " + name);
		};
	}
}
