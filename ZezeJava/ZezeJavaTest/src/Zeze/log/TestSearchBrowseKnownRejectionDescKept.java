package Zeze.log;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import Zeze.Config;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Builtin.LogService.BResult;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Services.Log4jQuery.Session;
import Zeze.Services.LogAgent;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.log.handle.BrowseLogHandle;
import Zeze.log.handle.SearchLogHandle;

import harness.Fast;

/**
 * /api/search 与 /api/browse 兜底 catch 的已知分诊透传直测：desc 是前端唯一错误
 * 通道（预编译前端错误分支只显示 r.desc），模块内为分诊精心措辞的拒绝——0 成员
 * 全服视图拒绝（FileSessionManager.resolve 的 IllegalStateException）与同 IP 在飞
 * 并发拒绝（operateRecovering 的 IllegalStateException）——此前被兜底 catch 一律
 * 坍缩为 "system error"，"全部日志服务器不可达"与"稍后重试"对用户不可区分。
 * 修复后兜底 catch 对已知族（IllegalStateException / 带信息的 TimeoutException）
 * 透传 message。驱动方式对齐 TestSearchBrowseUnknownServer：真回环 HttpServer 挂
 * 生产同款处理器，经反射注入 agent。
 *
 * <p>@Isolated：FileSessionManager/LogAgentManager 为 JVM 级静态状态，独占运行。</p>
 */
@Fast
@Isolated
@Extra
public class TestSearchBrowseKnownRejectionDescKept {

	/** 全服视图空注册表（0 成员拒绝）：desc 必须透出"无可达日志服务器"而非 system error。 */
	@Test
	public void testNoReachableServerDescKept() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/search", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new SearchLogHandle());
		server.addHandler("/api/browse", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new BrowseLogHandle());
		var client = HttpClient.newHttpClient();
		try (var unused = installAgent(newEmptyRegistryAgent())) {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			var searchUri = URI.create("http://127.0.0.1:" + port + "/api/search");
			var browseUri = URI.create("http://127.0.0.1:" + port + "/api/browse");

			assertEquals("no reachable log server for all-servers view (registered=0, members=0)",
					post(client, searchUri, allViewBody()).getString("desc"),
					"search：0 成员全服拒绝的 desc 必须透传（可分诊）");
			assertEquals("no reachable log server for all-servers view (registered=0, members=0)",
					post(client, browseUri, allViewBody()).getString("desc"),
					"browse：0 成员全服拒绝的 desc 必须透传（可分诊）");
		} finally {
			server.close();
			netty.close();
		}
	}

	/** 同 IP 在飞并发拒绝：desc 必须透出"稍后重试"的措辞而非 system error。 */
	@Test
	public void testConcurrentSameIpRejectionDescKept() throws Exception {
		Task.tryInitThreadPool();
		var blocking = newUninitialized(BlockingSession.class);
		blocking.searchStarted = new CountDownLatch(1);
		var agent = newSingleServerAgent("game1", blocking);
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/search", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new SearchLogHandle());
		var client = HttpClient.newHttpClient();
		ExecutorService pool = Executors.newSingleThreadExecutor();
		try (var unused = installAgent(agent)) {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			var uri = URI.create("http://127.0.0.1:" + port + "/api/search");

			// 第一个请求占住在飞守卫（blocking 会话不应答），第二个同 IP 请求必须拿到
			// 并发拒绝的明确 desc。
			var first = pool.submit(() -> post(client, uri, singleServerBody("game1")));
			assertTrue(blocking.searchStarted.await(5, TimeUnit.SECONDS), "第一个请求必须已进入会话操作");
			var second = post(client, uri, singleServerBody("game1"));
			assertEquals(500, second.getIntValue("status"), "并发拒绝必须是 errorResult 承载");
			assertEquals("concurrent search/browse on same session, retry after current request completes",
					second.getString("desc"),
					"同 IP 并发拒绝的 desc 必须透传（前端可提示重试）");

			blocking.release(); // 放行第一个请求
			first.get(10, TimeUnit.SECONDS); // 收尾，不泄漏 handler 线程
		} finally {
			pool.shutdownNow();
			server.close();
			netty.close();
			clearBindingsTable(); // 会话绑定静态残留不得泄漏给同 JVM 后续用例
		}
	}

	// ---------------------------------------------------------------- helpers

	/** 并发用例经 resolve 建立过绑定，静态表残留会污染同 JVM 后续用例（如
	 * TestSearchBrowseUnknownServer 的 assertNoSessionBinding）。 */
	@SuppressWarnings("unchecked")
	private static void clearBindingsTable() throws Exception {
		Field mapField = FileSessionManager.class.getDeclaredField("map");
		mapField.setAccessible(true);
		((java.util.Map<String, LogSessionBinding>)mapField.get(null)).clear();
	}

	private static String allViewBody() {
		return "{\"logName\":\"zeze\",\"words\":\"error\",\"pattern\":\"\",\"containsType\":1,\"limit\":10}";
	}

	private static String singleServerBody(String serverName) {
		return "{\"serverName\":\"" + serverName + "\",\"logName\":\"zeze\",\"words\":\"error\","
				+ "\"pattern\":\"\",\"containsType\":1,\"limit\":10,\"offsetFactor\":0.5}";
	}

	private static JSONObject post(HttpClient client, URI uri, String body) throws Exception {
		var request = HttpRequest.newBuilder(uri)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		var response = client.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(200, response.statusCode(), "错误承载在 HTTP 200 的 BaseResponse 内（处理器既有约定）");
		return JSON.parseObject(response.body());
	}

	/** 真实 LogAgent（SM disable，注册表恒空）+ 部署配置恒含 logName "zeze"（getLogConf 覆写）。 */
	private static LogAgent newEmptyRegistryAgent() throws Exception {
		var conf = new Config().loadAndParse();
		conf.setServiceManager("disable");
		return new ZezeLogConfAgent(conf);
	}

	/** 反射注入持有 stub agent 的 LogAgentManager：返回 AutoCloseable 恢复原静态值。 */
	private static AutoCloseable installAgent(LogAgent agent) throws Exception {
		@SuppressWarnings("unchecked")
		Constructor<LogAgentManager> ctor = (Constructor<LogAgentManager>)sun.reflect.ReflectionFactory
				.getReflectionFactory()
				.newConstructorForSerialization(LogAgentManager.class, Object.class.getDeclaredConstructor());
		var manager = ctor.newInstance();
		var agentField = LogAgentManager.class.getDeclaredField("logAgent");
		agentField.setAccessible(true);
		agentField.set(manager, agent);

		var staticField = LogAgentManager.class.getDeclaredField("logAgentManager");
		staticField.setAccessible(true);
		var prev = staticField.get(null);
		staticField.set(null, manager);
		return () -> staticField.set(null, prev);
	}

	/** 单服注册表 + newSession 返回注入会话的 stub agent（部署配置恒含 "zeze"）。 */
	private static LogAgent newSingleServerAgent(String serverName, BlockingSession session) throws Exception {
		var agent = newUninitialized(StubLogAgent.class);
		agent.registry = Set.of(serverName);
		agent.session = session;
		return agent;
	}

	/** 部署配置恒含请求所用的 logName "zeze"（摆脱对环境 xml 的依赖，先例
	 * TestSearchBrowseUnknownServer.ZezeLogConfAgent）。 */
	private static class ZezeLogConfAgent extends LogAgent {
		private final LogServiceConf deployConf = new LogServiceConf();

		ZezeLogConfAgent(Config config) throws Exception {
			super(config);
			var logConf = new LogServiceConf.LogConf();
			logConf.logActive = "zeze";
			deployConf.getLogConfs().put("zeze", logConf);
		}

		@Override
		public LogServiceConf getLogConf() {
			return deployConf;
		}
	}

	/** 行为由 preset 字段驱动（newUninitialized 不跑字段初始化器，字段由测试注入）：
	 * search 挂起至测试放行（占住在飞守卫）。 */
	private static final class BlockingSession extends Session {
		@SuppressWarnings("unused")
		BlockingSession() {
			super(null, null, null);
		}

		CountDownLatch searchStarted;
		private volatile TaskCompletionSource<BResult.Data> reply;

		@Override
		public TaskCompletionSource<BResult.Data> search(int limit, boolean reset,
				Zeze.Builtin.LogService.BCondition.Data condition) {
			var tcs = new TaskCompletionSource<BResult.Data>();
			reply = tcs;
			searchStarted.countDown();
			return tcs;
		}

		void release() {
			var data = new BResult.Data();
			data.setRemain(false);
			reply.setResult(data);
		}

		@Override
		public void close() {
			// 不触真实 RPC。
		}
	}

	private static final class StubLogAgent extends LogAgent {
		@SuppressWarnings("unused")
		StubLogAgent() throws Exception {
			super(null);
		}

		Set<String> registry = Set.of();
		BlockingSession session;
		private LogServiceConf deployConf; // 惰性构建（newUninitialized 不跑字段初始化器）

		@Override
		public Session newSession(String serverName, String logName) {
			return session;
		}

		@Override
		public Set<String> getLogServers() {
			return registry;
		}

		@Override
		public LogServiceConf getLogConf() {
			if (deployConf == null) {
				deployConf = new LogServiceConf();
				var logConf = new LogServiceConf.LogConf();
				logConf.logActive = "zeze";
				deployConf.getLogConfs().put("zeze", logConf);
			}
			return deployConf;
		}
	}

	/** 不调构造器分配实例（字段全默认值）：LogAgent 的构造器触真实初始化。 */
	@SuppressWarnings("restriction")
	private static <T> T newUninitialized(Class<T> clazz) throws Exception {
		@SuppressWarnings("unchecked")
		Constructor<T> ctor = (Constructor<T>)sun.reflect.ReflectionFactory.getReflectionFactory()
				.newConstructorForSerialization(clazz, Object.class.getDeclaredConstructor());
		return ctor.newInstance();
	}

	/** 兜底复位静态注入（测试中途失败的兜底；正常路径由 try-with-resources 恢复）。 */
	@AfterAll
	public static void resetManager() throws Exception {
		var staticField = LogAgentManager.class.getDeclaredField("logAgentManager");
		staticField.setAccessible(true);
		staticField.set(null, null);
	}
}
