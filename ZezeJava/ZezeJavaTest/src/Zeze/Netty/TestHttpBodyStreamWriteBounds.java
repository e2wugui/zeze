package Zeze.Netty;

import java.io.ByteArrayOutputStream;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.ReferenceCountUtil;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestHttpBodyStreamWriteBounds {
	@Test
	void chunkedWritesRejectTheWholeSourceRange() throws Exception {
		checkWrites(0);
	}

	@Test
	void fixedLengthWritesValidateBeforeConsumingTheirCapacity() throws Exception {
		checkWrites(2);
	}

	private static void checkWrites(int length) throws Exception {
		var channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter());
		try {
			var exchange = new HttpExchange(new HttpServer(), channel.pipeline().firstContext());
			try (var output = HttpResponseWithBodyStream.sendHeadersAndGetBody(exchange, HttpResponseStatus.OK, null, length)) {
				assertThrows(IndexOutOfBoundsException.class, () -> output.write(new byte[]{1}, 0, 3));
				assertThrows(IndexOutOfBoundsException.class, () -> output.write(new byte[]{1}, 0, Integer.MAX_VALUE));
				assertThrows(IndexOutOfBoundsException.class, () -> output.write(new byte[]{1}, -1, 0));
				output.write(new byte[]{8, 9});
			}
			var body = new ByteArrayOutputStream();
			for (Object message; (message = channel.readOutbound()) != null; ) {
				try {
					if (message instanceof HttpContent content) {
						var bytes = new byte[content.content().readableBytes()];
						content.content().readBytes(bytes);
						body.write(bytes);
					}
				} finally {
					ReferenceCountUtil.release(message);
				}
			}
			assertArrayEquals(new byte[]{8, 9}, body.toByteArray());
		} finally {
			channel.finishAndReleaseAll();
		}
	}
}
