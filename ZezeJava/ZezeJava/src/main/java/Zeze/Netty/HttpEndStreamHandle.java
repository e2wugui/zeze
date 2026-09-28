package Zeze.Netty;

import org.jetbrains.annotations.NotNull;

/**
 * 请求流结束（或完整请求就绪）的处理回调。
 */
@FunctionalInterface
public interface HttpEndStreamHandle {
	void onEndStream(@NotNull HttpExchange x) throws Exception;
}
