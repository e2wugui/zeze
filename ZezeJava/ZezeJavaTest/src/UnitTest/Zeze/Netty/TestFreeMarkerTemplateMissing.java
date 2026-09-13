package UnitTest.Zeze.Netty;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import Zeze.Netty.FreeMarker;
import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// FND5-17同族（复审）：模板缺失（getTemplate在Writer创建之前抛出）不经过fail()/close
// 路径——修复前默认事务路径下异常被triggerActions吞掉、生命周期close(null)是
// CLOSE_FINISH不发响应不断连，客户端挂到idle超时后零字节关闭。修复后直接单发500，
// 且异常仍向上传播（与渲染失败路径语义一致）。
@Fast
public class TestFreeMarkerTemplateMissing {
	@Test
	public void testTemplateMissingSend500(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var channel = new EmbeddedChannel(new ChannelOutboundHandlerAdapter());
		try {
			var freeMarker = new FreeMarker(tempDir.toFile()); // 空目录：模板必不存在
			var x = new HttpExchange(new HttpServer(), channel.pipeline().firstContext());

			Assertions.assertThrows(Exception.class, () -> freeMarker.sendResponse(x, null),
					"模板缺失仍应向上传播异常");

			var res = (FullHttpResponse)channel.readOutbound();
			Assertions.assertNotNull(res, "模板缺失必须发出响应（修复前客户端挂到idle超时）");
			Assertions.assertEquals(500, res.status().code());
			Assertions.assertTrue(res.content().toString(StandardCharsets.UTF_8).contains("template not found"),
					"500正文应指明模板缺失");
			Assertions.assertNull(channel.readOutbound(), "只发一个响应");
		} finally {
			channel.finishAndReleaseAll();
		}
	}
}
