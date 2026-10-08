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
 * /api/search 与 /api/browse 的显式未知 logName 入口分诊直测：同模块已建立缺省
 * logName（missing logName 列名引导）与未注册 serverName（unknown log server）两条
 * 防线，显式但<b>不存在</b>的 logName（拼写错误/跨部署拷贝的请求模板）是两者之间的
 * 对称缺口——透传时单服视图 Session 构造抛裸 RuntimeException 坍缩 system error、
 * 全服视图逐台跳过成 0 成员被误报 no reachable log server（错误归因到"服务器不可达"
 * 而非"日志名不存在"）。修复后显式未知名入口即拒 errorResult("unknown logName: X")，
 * 不建/复用会话。
 *
 * <p>驱动方式对齐 TestSearchBrowseUnknownServer：真回环 HttpServer 挂生产同款处理器；
 * 注入经反射构造的 LogAgent 子类（getLogConf 返回单份 zeze.log 的部署配置，ServiceManager
 * disable、注册表为空）——未知 logName 的全服视图请求漏过校验时进 newSessionAll 构造
 * 0 成员会话抛 IllegalStateException 落 catch，desc 为 "system error"，恰构成红/绿判别；
 * 已知 logName 的请求穿过 logName 防线落到 serverName 防线（unknown log server），
 * 证明正负两方向的判别次序。</p>
 *
 * <p>@Isolated：写 LogAgentManager JVM 级静态状态，独占运行。</p>
 */
@Fast
@Isolated
@Extra
public class TestSearchBrowseUnknownLogName {

	/** search：显式未知 logName 必须入口即拒（含空白包裹形态的 trim 归一）；已知
	 * logName 穿过 logName 防线落到 serverName 防线（分诊次序判别）。 */
	@Test
	public void testSearchRejectsUnknownLogName() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/search", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new SearchLogHandle());
		var client = HttpClient.newHttpClient();
		try (var unused = installSingleLogConfAgent()) {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			var uri = URI.create("http://127.0.0.1:" + port + "/api/search");

			assertUnknown(client, uri, "ghost.log", "search 显式未知 logName");
			assertUnknown(client, uri, " zeze.log.typo ", "search 带空白未知名（trim 后比对）");

			// 已知 logName 穿过 logName 防线：落到 serverName 防线（未知服务器名）
			assertEquals("unknown log server: game1",
					post(client, uri, body("game1", "zeze.log")).getString("desc"),
					"已知 logName 不得被误拒——分诊次序先 logName 后 serverName");
		} finally {
			server.close();
			netty.close();
		}
	}

	/** browse：同款显式 logName 存在性校验。 */
	@Test
	public void testBrowseRejectsUnknownLogName() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/browse", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new BrowseLogHandle());
		var client = HttpClient.newHttpClient();
		try (var unused = installSingleLogConfAgent()) {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			var uri = URI.create("http://127.0.0.1:" + port + "/api/browse");

			assertUnknown(client, uri, "ghost.log", "browse 显式未知 logName");
			assertEquals("unknown log server: game1",
					post(client, uri, body("game1", "zeze.log")).getString("desc"),
					"browse 已知 logName 穿过 logName 防线");
		} finally {
			server.close();
			netty.close();
		}
	}

	/** 断言 errorResult("unknown logName: X")（HTTP 200 承载，既有约定）。 */
	private static void assertUnknown(HttpClient client, URI uri, String logName, String what)
			throws Exception {
		var json = post(client, uri, body(null, logName));
		assertEquals(500, json.getIntValue("status"), what + "：必须是 errorResult 而非坍缩 system error");
		assertEquals("unknown logName: " + logName.trim(), json.getString("desc"),
				what + "：desc 必须明确指向日志名不存在");
	}

	private static String body(String serverName, String logName) {
		// pattern 必须显式给值：BCondition Bean 的 setPattern(null) 抛 IAE（生成代码不接受
		// null 字符串），会在到达 logName 校验前落入 system error——真实前端恒带该字段。
		var b = new StringBuilder("{");
		if (serverName != null)
			b.append("\"serverName\":\"").append(serverName).append("\",");
		if (logName != null)
			b.append("\"logName\":\"").append(logName).append("\",");
		b.append("\"words\":\"error\",\"pattern\":\"\",\"containsType\":1,\"limit\":10,\"offsetFactor\":0.5}");
		return b.toString();
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
	 * 注入持有单份 LogConf（zeze.log）配置的 LogAgent 子类（SM disable，注册表恒空）：
	 * 返回的 AutoCloseable 恢复原静态值，不污染同 JVM 其他用例。
	 */
	private static AutoCloseable installSingleLogConfAgent() throws Exception {
		var conf = new Config().loadAndParse();
		conf.setServiceManager("disable");
		var agent = new SingleLogConfAgent(conf);
		assertEquals(0, agent.getLogServers().size(), "SM disable 下注册表为空——任意服务器名都未注册");

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

	/** 部署配置恒为单份 zeze.log（getLogConf 覆写，摆脱对环境 xml 的依赖），
	 * 注册表与真实 Client 同源（空）。 */
	private static final class SingleLogConfAgent extends LogAgent {
		private final LogServiceConf deployConf = new LogServiceConf();

		SingleLogConfAgent(Config config) throws Exception {
			super(config);
			var logConf = new LogServiceConf.LogConf();
			logConf.logActive = "zeze.log";
			deployConf.getLogConfs().put("zeze.log", logConf);
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
