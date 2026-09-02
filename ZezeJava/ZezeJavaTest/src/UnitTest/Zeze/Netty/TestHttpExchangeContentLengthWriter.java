package UnitTest.Zeze.Netty;

import java.nio.charset.StandardCharsets;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpExchangeContentLengthWriter;
import Zeze.Netty.HttpServer;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// N-30回归：close()把池化html的所有权转给x.send后无状态标记，二次close会把同一个
// （发送后已由netty释放的）ByteBuf再发一次。Writer实现Closeable，JDK契约要求
// close幂等（已关闭时调用无效果）。模拟未来调用方按Writer契约自行close、
// try-with-resources再close、以及包装层第三次close，连接上必须只产生一个响应。
@Fast
public class TestHttpExchangeContentLengthWriter {
	@Test
	public void testCloseIdempotent() throws Exception {
		Task.tryInitThreadPool();
		var channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter());
		try {
			var x = new HttpExchange(new HttpServer(), channel.pipeline().firstContext());
			var out = new HttpExchangeContentLengthWriter(x);
			out.write("hello");
			out.close(); // 调用方按Closeable契约自行close
			out.close(); // 外层包装再补一次close
			out.close(); // 更外层再补一次close

			var res = (FullHttpResponse)channel.readOutbound();
			Assertions.assertNotNull(res, "close必须发送响应");
			Assertions.assertEquals(200, res.status().code());
			Assertions.assertEquals("5", res.headers().get(HttpHeaderNames.CONTENT_LENGTH));
			Assertions.assertEquals("text/html; charset=utf-8", res.headers().get(HttpHeaderNames.CONTENT_TYPE));
			Assertions.assertEquals("hello", res.content().toString(StandardCharsets.UTF_8));
			Assertions.assertNull(channel.readOutbound(), "重复close必须是no-op，不得再发第二个响应");
		} finally {
			channel.finishAndReleaseAll();
		}
	}
}
