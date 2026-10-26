package Zeze.Netty;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import harness.Fast;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRawUploadNamedFields {
	@Test
	void namedCollectionAndSingleFieldQueriesExposeTheSameRawUpload() throws Exception {
		var observed = new AtomicBoolean();
		var handler = new HttpFileUploadHandle() {
			@Override
			public void onEndRequest(HttpExchange exchange, InterfaceHttpPostRequestDecoder decoder) {
				var file = decoder.getBodyHttpData(getFileNameQueryKey());
				assertNotNull(file);
				assertEquals(List.of(file), decoder.getBodyHttpDatas(getFileNameQueryKey()));
				assertEquals(List.of(file), decoder.getBodyHttpDatas());
				assertTrue(decoder.getBodyHttpDatas("other").isEmpty());
				assertNull(decoder.getBodyHttpData("other"));
				assertTrue(decoder.hasNext());
				assertSame(file, decoder.next());
				assertFalse(decoder.hasNext());
				assertNull(decoder.next());
				observed.set(true);
			}
		};
		var channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter());
		var exchange = new HttpExchange(new HttpServer(), channel.pipeline().firstContext());
		try {
			exchange.request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/?filename=photo.dat");
			handler.onBeginStream(exchange, 0, 2, 3);
			var content = new DefaultHttpContent(Unpooled.wrappedBuffer(new byte[]{1, 2, 3}));
			try {
				handler.onStreamContent(exchange, content);
			} finally {
				content.release();
			}
			handler.onEndStream(exchange);
			assertTrue(observed.get());
		} finally {
			HttpFileUploadHandle.releaseChannelFileUpload(exchange);
			channel.finishAndReleaseAll();
		}
	}
}
