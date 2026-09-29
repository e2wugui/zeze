package Zeze.log;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import Zeze.Config;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Services.LogAgent;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import Zeze.log.handle.QueryHandle;

import harness.Fast;

/**
 * /api/query 的 serverName 入口预校验直测：缺字段（JSON 反序列化 null）透传到
 * LogAgent.query 的 ConcurrentHashMap.get(null) 抛 NPE、空白名与未注册名（显式
 * IAE "unknown log server"）同被兜底 catch 坍缩 "system error"——与 search/browse
 * 的入口分诊纪律（26186fc3e）不对称。修复后入口即拒：缺/空白回
 * "missing serverName"，未注册（trim 后比对注册表）回 "unknown log server: X"。
 *
 * <p>驱动方式对齐 TestSearchBrowseUnknownServer：真回环 HttpServer 挂生产同款
 * 处理器；经反射注入真实 LogAgent（ServiceManager disable，注册表为空——任意
 * 名都未注册），漏过校验的名字进入代理层以 NPE/IAE 落 catch，desc 为
 * "system error"，恰构成红/绿判别。</p>
 *
 * <p>@Isolated：写 LogAgentManager JVM 级静态状态，独占运行。</p>
 */
@Fast
@Isolated
public class TestQueryRejectsMalformedServerName {

	/** 三种畸形形态都必须明确分诊，不坍缩 system error。 */
	@Test
	public void testQueryRejectsMissingBlankAndUnknownServerName() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/query", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new QueryHandle());
		var client = HttpClient.newHttpClient();
		try (var unused = installEmptyRegistryAgent()) {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			var uri = URI.create("http://127.0.0.1:" + port + "/api/query");

			// JSON 缺 serverName 字段（null）：不得进入代理透传（NPE 坍缩）。
			assertError(client, uri, "{\"json\":\"{}\"}", "missing serverName", "缺字段（null）");
			// 纯空白名：与缺字段同形态分诊。
			assertError(client, uri, "{\"serverName\":\"   \",\"json\":\"{}\"}",
					"missing serverName", "纯空白名");
			// 未注册名：对齐 search/browse 的 "unknown log server: X"。
			assertError(client, uri, "{\"serverName\":\"ghost-server\",\"json\":\"{}\"}",
					"unknown log server: ghost-server", "未注册名");
			// 带首尾空白的未注册名：trim 后比对。
			assertError(client, uri, "{\"serverName\":\" game1 \",\"json\":\"{}\"}",
					"unknown log server: game1", "带首尾空白的未注册名");
		} finally {
			server.close();
			netty.close();
		}
	}

	/** 断言 errorResult（HTTP 200 承载，既有约定）与明确 desc。 */
	private static void assertError(HttpClient client, URI uri, String body, String expectedDesc, String what)
			throws Exception {
		var json = post(client, uri, body);
		assertEquals(500, json.getIntValue("status"), what + "：必须是 errorResult 而非透传 NPE/IAE");
		assertEquals(expectedDesc, json.getString("desc"), what + "：desc 必须明确指向参数错误");
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

	/**
	 * 注入持有真实 LogAgent（SM disable，注册表恒空）的 LogAgentManager：
	 * 返回的 AutoCloseable 恢复原静态值，不污染同 JVM 其他用例。
	 */
	private static AutoCloseable installEmptyRegistryAgent() throws Exception {
		var conf = new Config().loadAndParse();
		conf.setServiceManager("disable");
		var agent = new LogAgent(conf);
		assertEquals(0, agent.getLogServers().size(), "SM disable 下注册表为空——任意名都未注册");

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
		return () -> {
			staticField.set(null, prev);
			agent.stop();
		};
	}

	/** 兜底复位静态注入（测试中途失败的兜底；正常路径由 try-with-resources 恢复）。 */
	@AfterAll
	public static void resetManager() throws Exception {
		var staticField = LogAgentManager.class.getDeclaredField("logAgentManager");
		staticField.setAccessible(true);
		staticField.set(null, null);
	}
}
