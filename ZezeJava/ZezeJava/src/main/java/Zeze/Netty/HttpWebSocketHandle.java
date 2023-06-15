package Zeze.Netty;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("RedundantThrows")
public interface HttpWebSocketHandle {
	default void onOpen(@NotNull HttpExchange x) throws Exception {
	}

	// status==WebSocketCloseStatus.ABNORMAL_CLOSURE.code()时表示连接被强制关闭
	default void onClose(@NotNull HttpExchange x, int status, @NotNull String reason) throws Exception {
	}

	// 注意参数content需要遵循netty的引用管理(ReferenceCounted),带出方法外需要retain
	default void onPing(@NotNull HttpExchange x, @NotNull ByteBuf content) throws Exception {
		x.context().write(new PongWebSocketFrame(content.retain()));
	}

	// 注意参数content需要遵循netty的引用管理(ReferenceCounted),带出方法外需要retain
	default void onPong(@NotNull HttpExchange x, @NotNull ByteBuf content) throws Exception {
	}

	default void onText(@NotNull HttpExchange x, @NotNull String text) throws Exception {
	}

	// 注意参数content需要遵循netty的引用管理(ReferenceCounted),带出方法外需要retain
	default void onBinary(@NotNull HttpExchange x, @NotNull ByteBuf content) throws Exception {
	}
}
