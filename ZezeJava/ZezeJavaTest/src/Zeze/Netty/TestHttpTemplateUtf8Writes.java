package Zeze.Netty;

import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.util.ReferenceCountUtil;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestHttpTemplateUtf8Writes {
	@Test
	void bufferedWriterKeepsCharactersAcrossWriteBoundaries() throws Exception {
		checkSplitCharacter(false, "A\ud83d\ude00B");
	}

	@Test
	void streamingWriterKeepsCharactersAcrossWriteAndFlushBoundaries() throws Exception {
		checkSplitCharacter(true, "A\ud83d\ude00B");
	}

	@Test
	void endOfInputFinishesUnpairedCharactersWithTheStandardReplacement() throws Exception {
		checkSplitCharacter(false, "\udc00X\ud800");
		checkSplitCharacter(true, "\udc00X\ud800");
	}

	private static void checkSplitCharacter(boolean streaming, String text) throws Exception {
		var channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter());
		try {
			var exchange = new HttpExchange(new HttpServer(), channel.pipeline().firstContext());
			try (Writer writer = streaming ? new HttpExchangeStreamWriter(exchange) : new HttpExchangeContentLengthWriter(exchange)) {
				for (int i = 0; i < text.length(); i++) {
					writer.write(text.charAt(i));
					writer.flush();
				}
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
			assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), body.toByteArray());
		} finally {
			channel.finishAndReleaseAll();
		}
	}
}
