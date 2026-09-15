package UnitTest.Zeze.Netty;

import harness.Fast;
import java.net.InetSocketAddress;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
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
 *
 * FND6-17回归:register失败回滚(catch回滚services条目与保活handler后原样抛出,
 * 重试注册同一server不得残留duplicate register/duplicate path)与stop逐项隔离
 * (单个deregister失败只记日志,不中断其余清理,最终map清空)。
 */
@Fast
public class TestNettyConsulRegister {
	private static final String KeepAlivePath = "/Zeze_Netty_Consul_PassiveKeepAlivePath";
	private static Netty netty;
	private static HttpServer server;

	// ecwid的ConsulClient按需发起http调用(构造不连网),覆盖agentServiceRegister捕获注册参数。
	// 扩展点:failRegisterOnce注入一次性注册失败(模拟consul瞬断)、failDeregisterIds按serviceId
	// 注入注销失败(模拟单个服务注销网络异常),registered/deregistered记录全部请求供断言。
	public static final class CapturingClient extends ConsulClient {
		public volatile @Nullable NewService captured;
		// 全部注册请求(含注入失败的那些),便于多服务场景断言
		public final CopyOnWriteArrayList<NewService> registered = new CopyOnWriteArrayList<>();
		// 全部注销请求(先记录再注入失败,失败的尝试也留在列表里),断言stop逐项尽力清理
		public final CopyOnWriteArrayList<String> deregistered = new CopyOnWriteArrayList<>();
		// 非null时下一次agentServiceRegister抛出并清空——只失败一次,便于紧跟着验证重试成功
		public volatile @Nullable RuntimeException failRegisterOnce;
		// 命中这些serviceId的agentServiceDeregister抛RuntimeException(命中即移除,便于后续重试)
		public final CopyOnWriteArraySet<String> failDeregisterIds = new CopyOnWriteArraySet<>();

		public CapturingClient() {
			super("127.0.0.1");
		}

		@Override
		public @Nullable Response<Void> agentServiceRegister(@NotNull NewService newService) {
			captured = newService;
			registered.add(newService);
			var ex = failRegisterOnce;
			if (null != ex) {
				failRegisterOnce = null; // 一次性失败:抛出后恢复,后续注册走正常路径
				throw ex;
			}
			return null;
		}

		@Override
		public @Nullable Response<Void> agentServiceDeregister(@NotNull String serviceId) {
			deregistered.add(serviceId); // 先记录:注入失败的尝试也必须算"被尝试过"
			if (failDeregisterIds.remove(serviceId))
				throw new RuntimeException("simulated deregister failure: " + serviceId);
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

		// check自愈配置:10s间隔探活;进程消亡后探活持续失败,critical满5m由consul自动注销——
		// 兜底stop失败/超时反转产生的孤儿注册(不依赖真实consul,直接断言捕获到的NewService.Check参数)
		Assertions.assertEquals("10s", registered.getCheck().getInterval(), "check interval");
		Assertions.assertEquals("5m", registered.getCheck().getDeregisterCriticalServiceAfter(),
				"check deregisterCriticalServiceAfter");

		// 注册的保活handler确实可路由(返回200)
		var handler = server.getHandler(KeepAlivePath);
		Assertions.assertNotNull(handler, "keepalive handler must be registered");
	}

	// FND6-17:注册时consul瞬断(agentServiceRegister抛RuntimeException)必须回滚本地登记
	// (services条目+保活handler)后原样抛出——否则重试注册同一server恒抛duplicate register,
	// 且handler路径残留导致addHandler再抛duplicate path,server从此注册不上。
	// 独立起server而非复用静态server:静态server上残留本类其他用例的保活handler,
	// 会把本用例的失败点从agentServiceRegister错位成addHandler的重复path。
	@Test
	public void testRegisterRollbackOnConsulFailure() throws Exception {
		var httpServer = new HttpServer();
		httpServer.start(netty, 0).sync();
		try {
			var capturing = new CapturingClient();
			capturing.failRegisterOnce = new RuntimeException("simulated consul down"); // 模拟注册时consul瞬断
			var consul = new Consul(capturing);

			// 首次注册失败:异常原样抛出,本地登记与handler都必须回滚
			var ex = Assertions.assertThrows(RuntimeException.class,
					() -> consul.register("rollbackService", httpServer));
			Assertions.assertEquals("simulated consul down", ex.getMessage());
			Assertions.assertNull(httpServer.getHandler(KeepAlivePath),
					"失败注册的保活handler必须被回滚移除");

			// 立即重试注册同一server必须成功:services条目已回滚(否则抛duplicate register),
			// handler路径已回滚(addHandler能重新添加)
			consul.register("rollbackService", httpServer);
			Assertions.assertNotNull(httpServer.getHandler(KeepAlivePath), "重试后handler必须重新登记");

			// 第三次注册必须抛duplicate register——证明重试确实重建了services条目(回滚-重建闭环,
			// 而非首次失败时从未登记过)
			var dup = Assertions.assertThrows(IllegalStateException.class,
					() -> consul.register("rollbackService", httpServer));
			Assertions.assertTrue(dup.getMessage().contains("duplicate register"), dup.getMessage());
		} finally {
			httpServer.close();
		}
	}

	// FND6-17低成本覆盖:addHandler也在try守护内——保活path被占用时addHandler抛
	// IllegalStateException(重复path),同样走catch回滚services条目后原样抛出。
	// 注意按当前实现,回滚的removeHandler会把预占path上的handler一并清掉(为重试让路)。
	@Test
	public void testRegisterRollbackOnDuplicateHandlerPath() throws Exception {
		var httpServer = new HttpServer();
		httpServer.start(netty, 0).sync();
		try {
			// 预先占用保活path,把失败点定位到try块内的addHandler(agentServiceRegister之前)
			httpServer.addHandler(KeepAlivePath, 1024, null, null, x -> { });
			var capturing = new CapturingClient();
			var consul = new Consul(capturing);

			var ex = Assertions.assertThrows(IllegalStateException.class,
					() -> consul.register("dupPathService", httpServer));
			Assertions.assertTrue(ex.getMessage().contains("duplicate path"), ex.getMessage());
			// 失败发生在远端调用之前,consul注册请求不得发出
			Assertions.assertNull(capturing.captured, "addHandler失败时不得调用agentServiceRegister");
			// 回滚清掉了保活path(含预占的handler),为重试让路——按当前实现的最终状态断言
			Assertions.assertNull(httpServer.getHandler(KeepAlivePath), "回滚必须清空保活path");

			// 重试注册同一server必须成功:services条目已回滚(否则抛duplicate register),
			// path已清空(addHandler不再抛duplicate path)
			consul.register("dupPathService", httpServer);
			Assertions.assertNotNull(httpServer.getHandler(KeepAlivePath), "重试后handler必须重新登记");
		} finally {
			httpServer.close();
		}
	}

	// FND6-17:stop逐项隔离——单个deregister网络异常只记日志,不得中断循环:
	// 其余server照常注销、所有保活handler移除、最终services清空(重注册不抛duplicate)。
	// services按HttpServer keyed,同一server二次注册即duplicate,故用两个独立server(A/B)。
	@Test
	public void testStopIsolatesDeregisterFailure() throws Exception {
		var serverA = new HttpServer();
		var serverB = new HttpServer();
		serverA.start(netty, 0).sync();
		serverB.start(netty, 0).sync();
		try {
			var capturing = new CapturingClient();
			var consul = new Consul(capturing);
			consul.register("isoServiceA", serverA);
			consul.register("isoServiceB", serverB);

			// 按生产的serviceId拼法"@ip:port@serviceName"构造,精确注入A的注销失败
			var idA = "@" + serverA.getExportIp() + ":" + serverA.getPort() + "@isoServiceA";
			var idB = "@" + serverB.getExportIp() + ":" + serverB.getPort() + "@isoServiceB";
			capturing.failDeregisterIds.add(idA);

			// A的deregister抛RuntimeException被吞(记日志),stop整体不得抛出
			Assertions.assertDoesNotThrow(consul::stop);

			// A、B的注销都被尝试过:注入失败的A也算尝试,且A失败不挡B
			Assertions.assertTrue(capturing.deregistered.contains(idA), "A的注销必须被尝试");
			Assertions.assertTrue(capturing.deregistered.contains(idB), "A失败不得中断B的注销");

			// 两个server的保活handler都被移除——deregister失败的A同样清理handler
			Assertions.assertNull(serverA.getHandler(KeepAlivePath), "A的handler必须移除");
			Assertions.assertNull(serverB.getHandler(KeepAlivePath), "B的handler必须移除");

			// services已清空:stop后重注册同一对server必须成功(残留则抛duplicate register)
			Assertions.assertDoesNotThrow(() -> consul.register("isoServiceA", serverA));
			Assertions.assertDoesNotThrow(() -> consul.register("isoServiceB", serverB));
		} finally {
			serverA.close();
			serverB.close();
		}
	}
}
