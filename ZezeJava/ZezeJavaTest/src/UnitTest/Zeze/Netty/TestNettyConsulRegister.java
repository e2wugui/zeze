package UnitTest.Zeze.Netty;

import harness.Fast;
import java.net.InetSocketAddress;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import Zeze.Netty.Consul;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND-N2-6回归:Consul健康检查URL直接拼serviceId("@ip:port@serviceName"),
 * host部分不是合法主机名,consul agent探活必然失败,服务持续被判不健康。
 * 修复后按Consul API拼标准URL"http://ip:port/path"(IPv6字面量加方括号)。
 * 不依赖真实consul:通过注入捕获型ConsulClient断言注册的check URL。
 */
@Fast
public class TestNettyConsulRegister {
	private static final String KeepAlivePath = "/Zeze_Netty_Consul_PassiveKeepAlivePath";
	private static Netty netty;
	private static HttpServer server;

	// ecwid的ConsulClient按需发起http调用(构造不连网),覆盖agentServiceRegister捕获注册参数
	public static final class CapturingClient extends ConsulClient {
		public volatile @Nullable NewService captured;

		public CapturingClient() {
			super("127.0.0.1");
		}

		@Override
		public @Nullable Response<Void> agentServiceRegister(@NotNull NewService newService) {
			captured = newService;
			return null;
		}
	}

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		netty = new Netty(1);
		server = new HttpServer();
		var channel = server.start(netty, 0).sync().channel();
		Assertions.assertTrue(((InetSocketAddress)channel.localAddress()).getPort() > 0);
	}

	@AfterAll
	public static void tearDown() {
		server.close();
		netty.close();
	}

	@Test
	public void testRegisterCheckUrl() throws Exception {
		var capturing = new CapturingClient();
		var consul = new Consul(capturing);
		consul.register("testService", server);

		var registered = capturing.captured;
		Assertions.assertNotNull(registered, "agentServiceRegister must be called");
		Assertions.assertEquals("testService", registered.getName());
		Assertions.assertEquals(server.getPort(), registered.getPort().intValue());

		// 核心断言:check URL是标准形式,host为导出ip(修复前是"@ip:port@serviceName",探活必败)
		var ip = server.getExportIp();
		var host = ip.contains(":") ? "[" + ip + "]" : ip; // IPv6字面量在URL中加方括号
		var expected = "http://" + host + ":" + server.getPort() + KeepAlivePath;
		Assertions.assertNotNull(registered.getCheck(), "check must be set");
		Assertions.assertEquals(expected, registered.getCheck().getHttp());
		Assertions.assertFalse(registered.getCheck().getHttp().contains("@"),
				"check url must not contain serviceId");

		// 注册的保活handler确实可路由(返回200)
		var handler = server.getHandler(KeepAlivePath);
		Assertions.assertNotNull(handler, "keepalive handler must be registered");
	}
}
