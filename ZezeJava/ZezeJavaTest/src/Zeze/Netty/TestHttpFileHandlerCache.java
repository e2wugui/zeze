package Zeze.Netty;

import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestHttpFileHandlerCache {
	@Test
	void registeredCacheDurationReachesTheFileResponse(@TempDir Path directory) throws Exception {
		Task.tryInitThreadPool();
		Files.writeString(directory.resolve("item.txt"), "content");
		var server = new HttpServer();
		server.addFileHandler("/files/", directory.toString(), false, 47);
		var channel = new EmbeddedChannel(server);
		try {
			channel.writeInbound(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD, "/files/item.txt"));
			Object response = channel.readOutbound();
			try {
				var headers = assertInstanceOf(HttpResponse.class, response).headers();
				assertEquals("private, max-age=47", headers.get(HttpHeaderNames.CACHE_CONTROL));
				assertEquals(47, HttpServer.parseDate(headers.get(HttpHeaderNames.EXPIRES))
						- HttpServer.parseDate(headers.get(HttpHeaderNames.DATE)));
			} finally {
				ReferenceCountUtil.release(response);
			}
		} finally {
			channel.finishAndReleaseAll();
			server.close();
		}
	}
}
