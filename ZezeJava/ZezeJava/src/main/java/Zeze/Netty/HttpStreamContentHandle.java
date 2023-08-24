package Zeze.Netty;

import io.netty.handler.codec.http.HttpContent;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface HttpStreamContentHandle {
	// 注意参数content需要遵循netty的引用管理(ReferenceCounted),带出方法外需要retain
	void onStreamContent(@NotNull HttpExchange x, @NotNull HttpContent content) throws Exception;
}
