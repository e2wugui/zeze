package Zeze.log;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
 * /api/query 的 json 字段入口预校验直测：本端点已对 serverName 建立缺/空白/未注册
 * 三段分诊，透传查询体 json 字段未做同款校验——缺失（JSON缺字段→null）透传到
 * LogAgent.query 的 BJson 编码对 null 抛 NPE、空白串按空序列化省略使服务端
 * Json.parse("") 越界回错误码，两者同被兜底 catch 坍缩 "system error"（空串形态
 * 还放大成两端日志噪音）。修复后入口即拒：缺/空白回 "missing json"，合法 json
 * 照常透传代理。
 *
 * <p>驱动方式对齐 TestQueryRejectsMalformedServerName：真回环 HttpServer 挂生产同款
 * 处理器；注入覆写注册表（含 game1）与 query（记账不透传）的 LogAgent 子类
 * （ServiceManager disable）——畸形 json 漏过校验时进代理覆写点，红/绿判别在 desc
 * 与 query 计数两处。</p>
 *
 * <p>@Isolated：写 LogAgentManager JVM 级静态状态，独占运行。</p>
 */
@Fast
@Isolated
@Extra
public class TestQueryRejectsMissingJson {

	/** 缺字段/空白/纯空白 json 必须明确分诊，不坍缩 system error、不进代理。 */
	@Test
	public void testQueryRejectsMissingAndBlankJson() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/query", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new QueryHandle());
		var client = HttpClient.newHttpClient();
		try (var unused = installRecordingAgent()) {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			var uri = URI.create("http://127.0.0.1:" + port + "/api/query");

			assertError(client, uri, "{\"serverName\":\"game1\"}", "missing json",
					"缺 json 字段（null 形态 NPE 坍缩）");
			assertError(client, uri, "{\"serverName\":\"game1\",\"json\":\"\"}", "missing json",
					"空串形态（服务端解析越界坍缩）");
			assertError(client, uri, "{\"serverName\":\"game1\",\"json\":\"  \"}", "missing json",
					"纯空白形态");
			assertEquals(0, recordingAgent.queryCalls.get(), "畸形 json 不得进代理透传");

			// 合法 json 照常透传代理（校验不误伤正常路径）
			var ok = post(client, uri, "{\"serverName\":\"game1\",\"json\":\"{}\"}");
			assertEquals(200, ok.getIntValue("status"), "合法 json 照常成功");
			assertEquals("stub-query-result", ok.getString("data"), "代理返回值透传");
			assertEquals(1, recordingAgent.queryCalls.get(), "合法 json 恰一次代理调用");
			assertEquals("{}", recordingAgent.lastJson.get(), "json 原样透传");
		} finally {
			server.close();
			netty.close();
		}
	}

	private static void assertError(HttpClient client, URI uri, String body, String expectedDesc, String what)
			throws Exception {
		var json = post(client, uri, body);
		assertEquals(500, json.getIntValue("status"), what + "：必须是 errorResult 而非坍缩 system error");
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

	private static RecordingAgent recordingAgent;

	/**
	 * 注入覆写注册表（含 game1，处理器 serverName 三段校验可过）与 query（记账返回
	 * 固定串，不进网络）的 LogAgent 子类：返回的 AutoCloseable 恢复原静态值。
	 */
	private static AutoCloseable installRecordingAgent() throws Exception {
		var conf = new Config().loadAndParse();
		conf.setServiceManager("disable");
		recordingAgent = new RecordingAgent(conf);

		@SuppressWarnings("unchecked")
		Constructor<LogAgentManager> ctor = (Constructor<LogAgentManager>)sun.reflect.ReflectionFactory
				.getReflectionFactory()
				.newConstructorForSerialization(LogAgentManager.class, Object.class.getDeclaredConstructor());
		var manager = ctor.newInstance();
		var agentField = LogAgentManager.class.getDeclaredField("logAgent");
		agentField.setAccessible(true);
		agentField.set(manager, recordingAgent);

		var staticField = LogAgentManager.class.getDeclaredField("logAgentManager");
		staticField.setAccessible(true);
		var prev = staticField.get(null);
		staticField.set(null, manager);
		return () -> {
			staticField.set(null, prev);
			recordingAgent.stop();
		};
	}

	/** 注册表恒含 game1（serverName 校验可过），query 记账不透传。 */
	private static final class RecordingAgent extends LogAgent {
		final AtomicInteger queryCalls = new AtomicInteger();
		final AtomicReference<String> lastJson = new AtomicReference<>();

		RecordingAgent(Config config) throws Exception {
			super(config);
		}

		@Override
		public Set<String> getLogServers() {
			return Set.of("game1");
		}

		@Override
		public String query(String serverName, String jsonArgument) {
			queryCalls.incrementAndGet();
			lastJson.set(jsonArgument);
			return "stub-query-result";
		}
	}

	/** 兜底复位静态注入（测试中途失败的兜底；正常路径由 try-with-resources 恢复）。 */
	@AfterAll
	public static void resetManager() throws Exception {
		var staticField = LogAgentManager.class.getDeclaredField("logAgentManager");
		staticField.setAccessible(true);
		staticField.set(null, null);
	}
}
