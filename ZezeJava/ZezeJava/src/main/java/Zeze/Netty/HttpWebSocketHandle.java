package Zeze.Netty;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;

@SuppressWarnings("RedundantThrows")
public interface HttpWebSocketHandle {
	default void onOpen(HttpExchange x) throws Exception {
	}

	default void onClose(HttpExchange x, int status, String reason) throws Exception {
	}

	default void onPing(HttpExchange x, ByteBuf content) throws Exception {
		x.context().write(new PongWebSocketFrame(content.retain()));
	}

	default void onPong(HttpExchange x, ByteBuf content) throws Exception {
	}

	default void onText(HttpExchange x, String text) throws Exception {
	}

	default void onBinary(HttpExchange x, ByteBuf content) throws Exception {
	}
}
