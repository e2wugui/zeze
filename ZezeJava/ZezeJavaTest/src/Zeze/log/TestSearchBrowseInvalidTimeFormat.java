package Zeze.log;

import harness.Extra;
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
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Services.LogAgent;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import Zeze.log.handle.BrowseLogHandle;
import Zeze.log.handle.SearchLogHandle;

import harness.Fast;

/**
 * /api/search 与 /api/browse 的 beginTime/endTime 时间串格式预校验直测：手输
 * 笔误（非 yyyy-MM-dd HH:mm:ss）的 DateTimeParseException 原先发生在会话操作
 * 之前（无害会话），却落入兜底 catch 坍缩 "system error"——参数笔误被引导成
 * 服务端故障。修复后 validateError 入口预检，回显式 desc（invalid beginTime/
 * endTime + 期望格式），不坍缩 system error。
 *
 * <p>驱动方式对齐 TestSearchBrowseUnknownServer：真回环 HttpServer 挂生产同款
 * 处理器；注入真实 LogAgent（SM disable，注册表为空），请求走全服视图
 * （serverName 空串，不经单服务器注册表比对）——修复前畸形时间串在 BCondition
 * 构造处抛 DateTimeParseException 落入 catch，desc 为 "system error"，恰构成
 * 红/绿判别。</p>
 *
 * <p>@Isolated：写 LogAgentManager JVM 级静态状态，独占运行。</p>
 */
@Fast
@Isolated
@Extra
public class TestSearchBrowseInvalidTimeFormat {

	/** search：beginTime/endTime 格式非法必须入口即拒，desc 指明字段与期望格式。 */
	@Test
	public void testSearchRejectsMalformedTimeStrings() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/search", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new SearchLogHandle());
		var client = HttpClient.newHttpClient();
		try (var unused = installEmptyRegistryAgent()) {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			var uri = URI.create("http://127.0.0.1:" + port + "/api/search");

			assertInvalidTime(client, uri, "2026/09/01 00:00:00", null,
					"invalid beginTime, expect yyyy-MM-dd HH:mm:ss", "search beginTime 日期分隔符笔误");
			assertInvalidTime(client, uri, null, "2026-13-99 99:99:99",
					"invalid endTime, expect yyyy-MM-dd HH:mm:ss", "search endTime 越界值笔误");
			assertInvalidTime(client, uri, "not-a-time", null,
					"invalid beginTime, expect yyyy-MM-dd HH:mm:ss", "search beginTime 非时间串");

			// 负对照：合法格式不被拒绝——走到代理层以既有失败形态承载（空注册表
			// 全服视图 0 成员拒绝，desc 透传该分诊），证明预检只收敛格式错误、不扩大化误伤。
			assertEquals("no reachable log server for all-servers view (registered=0, members=0)",
					post(client, uri, body("2026-09-01 00:00:00", null)).getString("desc"),
					"合法时间格式不得被 invalid 拒绝");
		} finally {
			server.close();
			netty.close();
		}
	}

	/** browse：同款时间格式预检（与 search 共用 SearchLogParam，处理器各自入口）。 */
	@Test
	public void testBrowseRejectsMalformedTimeStrings() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/browse", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new BrowseLogHandle());
		var client = HttpClient.newHttpClient();
		try (var unused = installEmptyRegistryAgent()) {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			var uri = URI.create("http://127.0.0.1:" + port + "/api/browse");

			assertInvalidTime(client, uri, "2026/09/01 00:00:00", null,
					"invalid beginTime, expect yyyy-MM-dd HH:mm:ss", "browse beginTime 日期分隔符笔误");
		} finally {
			server.close();
			netty.close();
		}
	}

	private static String body(String beginTime, String endTime) {
		return "{\"serverName\":\"\",\"logName\":\"zeze\",\"words\":\"error\",\"pattern\":\"\","
				+ "\"containsType\":1,\"limit\":10,\"offsetFactor\":0.5,"
				+ "\"beginTime\":\"" + (beginTime == null ? "" : beginTime) + "\","
				+ "\"endTime\":\"" + (endTime == null ? "" : endTime) + "\"}";
	}

	private static void assertInvalidTime(HttpClient client, URI uri, String beginTime, String endTime,
										  String expectedDesc, String what) throws Exception {
		var json = post(client, uri, body(beginTime, endTime));
		assertEquals(500, json.getIntValue("status"), what + "：必须是 errorResult 而非 system error");
		assertEquals(expectedDesc, json.getString("desc"), what + "：desc 必须指明字段与期望格式");
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
	 * 返回的 AutoCloseable 恢复原静态值并停 agent，不污染同 JVM 其他用例。
	 */
	private static AutoCloseable installEmptyRegistryAgent() throws Exception {
		var conf = new Config().loadAndParse();
		conf.setServiceManager("disable");
		var agent = new ZezeLogConfAgent(conf);

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


	/** 部署配置恒含请求所用的 logName "zeze"（getLogConf 覆写，摆脱对环境 xml 的依赖）：
	 * 显式 logName 存在性入口校验（TestSearchBrowseUnknownLogName）之后，空 LogConf 的
	 * 裸真实 agent 会在到达本测试的目标防线（serverName/时间格式/代理层）前先拒
	 * "unknown logName: zeze"——测试环境无 LogServiceConf 节点，覆写配置使校验门可过。 */
	private static final class ZezeLogConfAgent extends LogAgent {
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
	/** 兜底复位静态注入（测试中途失败的兜底；正常路径由 try-with-resources 恢复）。 */
	@AfterAll
	public static void resetManager() throws Exception {
		var staticField = LogAgentManager.class.getDeclaredField("logAgentManager");
		staticField.setAccessible(true);
		staticField.set(null, null);
	}
}
