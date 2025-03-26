package Zeze.Netty;

import java.nio.charset.StandardCharsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("RedundantThrows")
public interface HttpWebSocketHandle {
	default void onOpen(@NotNull HttpExchange x) throws Exception {
	}

	// status==WebSocketCloseStatus.ABNORMAL_CLOSURE.code()时表示连接被强制关闭
	default void onClose(@NotNull HttpExchange x, int status, @NotNull String reason) throws Exception {
	}

	// 注意参数content需要遵循netty的引用管理(ReferenceCounted),带出方法外需要retain,然后不用时再release
	default void onPing(@NotNull HttpExchange x, @NotNull ByteBuf content) throws Exception {
		x.context().write(new PongWebSocketFrame(content.retain()));
	}

	// 注意参数content需要遵循netty的引用管理(ReferenceCounted),带出方法外需要retain,然后不用时再release
	default void onPong(@NotNull HttpExchange x, @NotNull ByteBuf content) throws Exception {
	}

	// 收到一段内容content,可能不是完整帧,isFinal=false时,说明还需要拼接后续onContent提供的content,直到isFinal=true
	// 注意参数content需要遵循netty的引用管理(ReferenceCounted),带出方法外需要retain,然后不用时再release
	default void onContent(@NotNull HttpExchange x, @NotNull ByteBuf content, boolean isText, boolean isFinal)
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
	default void onText(@NotNull HttpExchange x, @NotNull String text) throws Exception {
	}

	// 收到一个完整的二进制帧
	// 注意参数content需要遵循netty的引用管理(ReferenceCounted),带出方法外需要retain,然后不用时再release
	default void onBinary(@NotNull HttpExchange x, @NotNull ByteBuf content) throws Exception {
	}
}
