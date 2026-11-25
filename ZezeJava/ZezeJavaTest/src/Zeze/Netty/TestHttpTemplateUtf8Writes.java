package Zeze.Netty;

import java.io.ByteArrayOutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
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

	@Test
	void streamingWriterWritesHtmlContentTypeAndResponseHeaders() throws Exception {
		var channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter());
		try {
			var exchange = new HttpExchange(new HttpServer(), channel.pipeline().firstContext());
			exchange.addHeader("X-Test", "1");
			exchange.addCookie("sid", "abc");
			try (var writer = new HttpExchangeStreamWriter(exchange)) {
				writer.write("<p>ok</p>");
			}
			var first = channel.readOutbound();
			try {
				assertInstanceOf(HttpResponse.class, first);
				var res = (HttpResponse)first;
				assertEquals(HttpResponseStatus.OK, res.status());
				assertEquals("text/html; charset=utf-8", res.headers().get(HttpHeaderNames.CONTENT_TYPE));
				assertEquals("1", res.headers().get("X-Test"));
				assertTrue(res.headers().getAll(HttpHeaderNames.SET_COOKIE).contains("sid=abc"));
				assertEquals(HttpHeaderValues.KEEP_ALIVE.toString(), res.headers().get(HttpHeaderNames.CONNECTION));
			} finally {
				ReferenceCountUtil.release(first);
			}
		} finally {
			channel.finishAndReleaseAll();
		}
	}

	@Test
	void streamingWriterOmitsConnectionHeaderOnHttp2() throws Exception {
		var channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter());
		try {
			channel.attr(HttpExchange.h2StreamKey).set(Boolean.TRUE);
			var exchange = new HttpExchange(new HttpServer(), channel.pipeline().firstContext());
			exchange.addHeader("X-H2", "yes");
			exchange.addCookie("h2sid", "xyz");
			try (var writer = new HttpExchangeStreamWriter(exchange)) {
				writer.write("<p>h2</p>");
			}
			var first = channel.readOutbound();
			try {
				assertInstanceOf(HttpResponse.class, first);
				var res = (HttpResponse)first;
				assertEquals(HttpResponseStatus.OK, res.status());
				assertEquals("text/html; charset=utf-8", res.headers().get(HttpHeaderNames.CONTENT_TYPE));
				assertEquals("yes", res.headers().get("X-H2"));
				assertTrue(res.headers().getAll(HttpHeaderNames.SET_COOKIE).contains("h2sid=xyz"));
				assertFalse(res.headers().contains(HttpHeaderNames.CONNECTION));
			} finally {
				ReferenceCountUtil.release(first);
			}
		} finally {
			channel.finishAndReleaseAll();
		}
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
