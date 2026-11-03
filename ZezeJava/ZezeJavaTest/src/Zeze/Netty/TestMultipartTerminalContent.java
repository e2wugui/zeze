package Zeze.Netty;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import harness.Fast;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestMultipartTerminalContent {
	@Test
	public void emptyLastContentCompletesDecoderBeforeEndRequest() {
		completeUpload(true);
	}

	@Test
	public void bodyInLastContentTreatsDecoderExhaustionAsSuccess() {
		completeUpload(false);
	}

	private static void completeUpload(boolean emptyLast) {
		var result = new AtomicReference<String>();
		try (var server = new HttpServer()) {
			server.addHandler("/upload", TransactionLevel.None, DispatchMode.Direct, new HttpMultipartHandle() {
				@Override
				public void onEndRequest(HttpExchange x, InterfaceHttpPostRequestDecoder decoder) throws Exception {
					var fields = decoder.getBodyHttpDatas();
					result.set(((Attribute)fields.getFirst()).getValue());
				}
			});
			var channel = new EmbeddedChannel(server);
			try {
				var request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/upload");
				request.headers().set(HttpHeaderNames.CONTENT_TYPE, "multipart/form-data; boundary=B");
				channel.writeInbound(request);
				var body = Unpooled.copiedBuffer(
						"--B\r\nContent-Disposition: form-data; name=\"field\"\r\n\r\nvalue\r\n--B--\r\n",
						StandardCharsets.US_ASCII);
				if (emptyLast) {
					channel.writeInbound(new DefaultHttpContent(body));
					channel.writeInbound(LastHttpContent.EMPTY_LAST_CONTENT);
				} else
					channel.writeInbound(new DefaultLastHttpContent(body));
				assertEquals("value", result.get());
			} finally {
				channel.finishAndReleaseAll();
			}
		}
	}
}
