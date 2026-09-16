package UnitTest.Zeze.Netty;

import java.nio.charset.StandardCharsets;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpExchangeStreamWriter;
import Zeze.Netty.HttpServer;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// FND7-25回归：chunked流式响应的终结符写在endStream里，而该写在close的detached CAS幂等
// 保护之外——HttpExchangeStreamWriter实现Closeable（双重close是JDK契约内的合法用法，参考
// 兄弟类HttpExchangeContentLengthWriter的closed标志），二次close向keep-alive连接写出第二个
// chunked终结符（0\r\n\r\n），客户端把它当作下一响应的前缀垃圾，后续响应状态行解析失败且
// 完全静默。修复后endStream本身幂等：任何调用方重复调用（Writer多重close、DbWeb的try/catch
// 补收尾）都只允许写一个终结符。
@Fast
public class TestFnd725StreamCloseIdempotent {

	// 模拟调用方按Writer契约自行close、外层try-with-resources再close、包装层第三次close：
	// 连接上必须只有一个chunked终结符。
	@Test
	public void testWriterMultipleCloseSingleTerminator() throws Exception {
		Task.tryInitThreadPool();
		var channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter());
		try {
			var x = new HttpExchange(new HttpServer(), channel.pipeline().firstContext());
			var out = new HttpExchangeStreamWriter(x);
			out.write("hello");
			out.close(); // 调用方按Closeable契约自行close
			out.close(); // 外层包装再补一次close
			out.close(); // 更外层再补一次close

			var res = (HttpResponse)channel.readOutbound();
			Assertions.assertNotNull(res, "beginStream必须写出响应头");
			Assertions.assertEquals(200, res.status().code());
			Assertions.assertEquals(HttpHeaderValues.CHUNKED.toString(),
					res.headers().get(HttpHeaderNames.TRANSFER_ENCODING));

			var content = (HttpContent)channel.readOutbound();
			Assertions.assertNotNull(content, "write必须写出数据块");
			Assertions.assertEquals("hello", content.content().toString(StandardCharsets.UTF_8));

			var last = (LastHttpContent)channel.readOutbound();
			Assertions.assertNotNull(last, "close必须写chunked终结符");
			Assertions.assertNull(channel.readOutbound(), "重复close必须是no-op，不得写第二个终结符");
		} finally {
			channel.finishAndReleaseAll();
		}
	}

	// 根因级：endStream自身的幂等性（不依赖Writer包装层）。直接调用方（DbWeb等）异常路径
	// try/catch重复调用endStream，同样只允许写一个终结符。
	@Test
	public void testEndStreamIdempotent() throws Exception {
		Task.tryInitThreadPool();
		var channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter());
		try {
			var x = new HttpExchange(new HttpServer(), channel.pipeline().firstContext());
			x.beginStream(HttpResponseStatus.OK, HttpServer.setDate(new DefaultHttpHeaders())
					.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE));
			x.sendStream("hello".getBytes(StandardCharsets.UTF_8));
			x.endStream();
			x.endStream(); // 重复调用不得再写终结符

			Assertions.assertNotNull(channel.readOutbound(), "响应头");
			Assertions.assertNotNull(channel.readOutbound(), "数据块");
			Assertions.assertNotNull((LastHttpContent)channel.readOutbound(), "终结符");
			Assertions.assertNull(channel.readOutbound(), "二次endStream必须是no-op");
		} finally {
			channel.finishAndReleaseAll();
		}
	}
}
