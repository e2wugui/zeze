package Zeze.Util;

import Zeze.Net.SocketOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface TimeThrottle extends AutoCloseable {
	boolean checkNow(int size);

	@SuppressWarnings("override")
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
