package Zeze.Util;

import Zeze.Net.SocketOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 限流器接口：按配置名创建 queue（滑动窗口）或 counter（固定窗口）实现
public interface TimeThrottle extends AutoCloseable {
	boolean checkNow(int size);

	@Override
	default void close() {
	}

	static @Nullable TimeThrottle create(@NotNull SocketOptions options) {
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
