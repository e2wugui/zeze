package Zeze.Netty;

import java.nio.charset.StandardCharsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;

/**
 * WebSocket 事件回调接口。
 */
@SuppressWarnings("RedundantThrows")
public interface HttpWebSocketHandle {
	default void onOpen(HttpExchange x) throws Exception {
	}

	// status==WebSocketCloseStatus.ABNORMAL_CLOSURE.code()时表示连接被强制关闭
	default void onClose(HttpExchange x, int status, String reason) throws Exception {
	}

	// 注意参数content需要遵循netty的引用管理(ReferenceCounted),带出方法外需要retain,然后不用时再release
	default void onPing(HttpExchange x, ByteBuf content) throws Exception {
		// 必须 writeAndFlush：HttpServer.channelRead 不向尾部透传帧，此处是唯一的 pong 路径，避免空闲连接滞留。
		x.context().writeAndFlush(new PongWebSocketFrame(content.retain()));
	}

	// 注意参数content需要遵循netty的引用管理(ReferenceCounted),带出方法外需要retain,然后不用时再release
	default void onPong(HttpExchange x, ByteBuf content) throws Exception {
	}

	// 收到一段内容content,可能不是完整帧,isFinal=false时,说明还需要拼接后续onContent提供的content,直到isFinal=true
	// 注意参数content需要遵循netty的引用管理(ReferenceCounted),带出方法外需要retain,然后不用时再release
	default void onContent(HttpExchange x, ByteBuf content, boolean isText, boolean isFinal)
			throws Exception {
		if (isFinal) {
			var c = x.content() == Unpooled.EMPTY_BUFFER ? content : x.addContent(content.retain());
			try {
				if (isText)
					onText(x, c.toString(StandardCharsets.UTF_8));
				else
					onBinary(x, c);
			} finally {
				x.releaseContent();
			}
		} else
			x.addContent(content.retain());
	}

	// 收到一个完整的文本帧
	default void onText(HttpExchange x, String text) throws Exception {
	}

	// 收到一个完整的二进制帧
	// 注意参数content需要遵循netty的引用管理(ReferenceCounted),带出方法外需要retain,然后不用时再release
	default void onBinary(HttpExchange x, ByteBuf content) throws Exception {
	}
}
