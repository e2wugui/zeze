package Zeze.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetAddress;
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
 * /api/search 与 /api/browse 的单服务器 serverName 注册表预校验直测：未注册/已被 SM
 * 摘除/带首尾空白的服务器名透传到 Session 构造的裸解引用（__getLogServer 未命中
 * 返回 null → NPE 无信息、不满足会话级判别不自愈），恒 system error；同模块
 * /api/query 的 LogAgent.query 对同一张表有显式 "unknown log server" 校验——三条
 * API 路径行为不对称。修复后入口对照注册表校验：未知名回
 * errorResult("unknown log server: ...")，不建会话。
 *
 * <p>驱动方式对齐 TestSearchBrowseInvalidLimit：真回环 HttpServer 挂生产同款处理器；
 * 经反射向 LogAgentManager 注入真实 LogAgent（ServiceManager disable，注册表为空——
 * 任何 serverName 都未注册），漏过校验的名字会进到 Session 构造 NPE 落入 catch，
 * desc 为 "system error"，恰构成红/绿判别。注入以 try/finally + @AfterAll 复位静态，
 * 不污染同 JVM 后续用例。</p>
 *
 * <p>@Isolated：FileSessionManager/LogAgentManager 为 JVM 级静态状态，独占运行。</p>
 */
@Fast
@Isolated
public class TestSearchBrowseUnknownServer {

	/** search：未注册名与带空白名必须入口即拒（空白名归一到 trim 后比对）。 */
	@Test
	public void testSearchRejectsUnknownServerName() throws Exception {
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

			assertUnknown(client, uri, "ghost-server", "search 未注册名");
			assertUnknown(client, uri, " game1", "search 带首空白名（归一后仍未注册）");

				assertNoSessionBinding();
				// 负对照：全服视图（serverName 空）不经单服务器名校验，走全服路径的既有
				// "no reachable log server" 失败（空注册表构造 0 成员会话被拒）——desc 透传
				// 该分诊（兜底 catch 的已知族映射），与未知名单服务器路径的明确错误同形。
				assertEquals("no reachable log server for all-servers view (registered=0, members=0)",
						post(client, uri, allViewBody()).getString("desc"),
						"全服视图不走单服务器名校验，0 成员拒绝的 desc 透传");
		} finally {
			server.close();
			netty.close();
		}
	}

	/** browse：同款注册表预校验。 */
	@Test
	public void testBrowseRejectsUnknownServerName() throws Exception {
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

			assertUnknown(client, uri, "ghost-server", "browse 未注册名");
			assertUnknown(client, uri, "game1 ", "browse 带尾空白名");

			assertNoSessionBinding();
		} finally {
			server.close();
			netty.close();
		}
	}

	/** 断言 errorResult("unknown log server: X")（HTTP 200 承载，既有约定）。 */
	private static void assertUnknown(HttpClient client, URI uri, String serverName, String what)
			throws Exception {
		var json = post(client, uri, body(serverName));
		assertEquals(500, json.getIntValue("status"), what + "：必须是 errorResult 而非透传 NPE");
		assertEquals("unknown log server: " + serverName.trim(), json.getString("desc"),
				what + "：desc 必须明确指向未注册的服务器名");
	}

	private static String body(String serverName) {
		// pattern 必须显式给值：BCondition Bean 的 setPattern(null) 抛 IAE（生成代码不接受
		// null 字符串），会在到达注册表校验前落入 system error——真实前端恒带该字段。
		return "{\"serverName\":\"" + serverName + "\",\"logName\":\"zeze\",\"words\":\"error\","
				+ "\"pattern\":\"\",\"containsType\":1,\"limit\":10,\"offsetFactor\":0.5}";
	}

	private static String allViewBody() {
		return "{\"logName\":\"zeze\",\"words\":\"error\",\"pattern\":\"\",\"containsType\":1,\"limit\":10}";
	}

	/** 未知名不得在会话表留下绑定（不创建/复用查询会话、不透传服务器）。 */
	private static void assertNoSessionBinding() throws Exception {
		var clientAddr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0);
		assertNull(FileSessionManager.get(clientAddr), "未知服务器名的请求不得创建/复用查询会话");
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
	 * 返回的 AutoCloseable 恢复原静态值，防止污染同 JVM 其他用例
	 * （TestSearchBrowseInvalidLimit 的负对照依赖 getInstance()==null 的 NPE 形态）。
	 */
	private static AutoCloseable installEmptyRegistryAgent() throws Exception {
		var conf = new Config().loadAndParse();
		conf.setServiceManager("disable");
		var agent = new ZezeLogConfAgent(conf);
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
		return () -> staticField.set(null, prev);
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
