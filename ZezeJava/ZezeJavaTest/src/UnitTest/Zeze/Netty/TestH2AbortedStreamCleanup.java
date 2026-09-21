package UnitTest.Zeze.Netty;

import java.util.concurrent.ConcurrentLinkedQueue;

import Zeze.Netty.HttpExchange;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * NY1-F2回归：h2中止流（RST_STREAM）永久泄漏exchange——janitor（closeInFlightExchanges）
 * 按responseOrderKey工作，h2子channel不经响应序化器，对其恒no-op，exchanges表条目无人close。
 * 修复：channelInactive/exceptionCaught善后点seq==null时按channel id取出exchange直接close
 * （复用既有幂等机制）。客户端开stream发请求后复位stream，服务端exchange必须被终结且出表。
 */
@Fast
public class TestH2AbortedStreamCleanup {

	static final class CollectingServer extends HttpServer {
		final ConcurrentLinkedQueue<HttpExchange> created = new ConcurrentLinkedQueue<>();

		@Override
		public @NotNull HttpExchange createHttpExchange(@NotNull ChannelHandlerContext context) {
			var x = new HttpExchange(this, context);
			created.add(x);
			return x;
		}

		// exchanges为protected：暴露快照给测试断言泄漏
		int liveExchangeCount() {
			return exchanges.size();
		}
	}

	@Test
	public void testAbortedStreamClosesAndRemovesExchange() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new CollectingServer();
		// detach且不响应：模拟慢/挂起handler，中止后exchange只能靠善后点回收
		server.addHandler("/hang", 8192, TransactionLevel.None, DispatchMode.Normal, x -> x.detach());
		try (var client = new H2TestClient()) {
			var port = ((java.net.InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();
			client.connect(port);

			client.requestThenReset(HttpMethod.GET, "/hang");

			// 修复前：h2子channel的善后对janitor不可见，exchange永滞exchanges表（isClosed恒false）
			var deadline = System.currentTimeMillis() + 10_000;
			while (true) {
				var allClosed = server.created.stream().allMatch(HttpExchange::isClosed);
				// request释放并入等待条件：close流程分阶段（置死→出表→释放retain），
				// isClosed()为真时request()可能尚未置null（1/20假红：循环出口立刻断言撞上释放前瞬态）
				var allReleased = server.created.stream().allMatch(x -> x.request() == null);
				if (allClosed && allReleased && server.liveExchangeCount() == 0 && !server.created.isEmpty())
					break;
				Assertions.assertTrue(System.currentTimeMillis() < deadline,
						"中止流必须close exchange并从exchanges表移除: created=" + server.created.size()
								+ " live=" + server.liveExchangeCount());
				//noinspection BusyWait
				Thread.sleep(20);
			}
		} finally {
			server.close();
			netty.close();
		}
	}
}
