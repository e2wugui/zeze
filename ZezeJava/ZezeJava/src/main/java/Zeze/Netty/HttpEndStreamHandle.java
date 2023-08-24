package Zeze.Netty;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface HttpEndStreamHandle {
	void onEndStream(@NotNull HttpExchange x) throws Exception;
}
