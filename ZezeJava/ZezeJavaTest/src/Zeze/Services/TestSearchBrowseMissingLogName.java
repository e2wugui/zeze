package Zeze.Services;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import javax.xml.parsers.DocumentBuilderFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import Zeze.Config;
import Zeze.Net.ServiceConf;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Services.ServiceManager.BEditService;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import Zeze.log.FileSessionManager;
import Zeze.log.LogAgentManager;
import Zeze.log.handle.BrowseLogHandle;
import Zeze.log.handle.SearchLogHandle;

import harness.Fast;

/**
 * /api/search 与 /api/browse 的 logName 缺省解析直测：随源码发布的 web 前端请求体
 * 不携带 logName（searchParam 字面量无该字段），缺省值 null 透传到 Session 构造
 * 的 setLogName(null)（生成代码对 null 抛 IAE）——单服务器视图与全服视图（前端
 * 默认 serverName 为空串）都恒失败，desc 为无信息量的 "system error"；全服视图
 * 更被 SessionAll 逐台跳过成 0 成员，误报 "no reachable log server"。修复后处理器
 * 入口解析缺省 logName：部署配置（LogServiceConf）的主 LogConf（server.xml 配置
 * 顺序的首个，单日志部署即唯一名）为默认，查询正常成功；零 LogConf 无法确定默认
 * 时回显式 missing logName 错误，不再坍缩 system error。
 *
 * <p>语义沿革（FND34 zokermanager-01）：多份 LogConf 曾回 "missing logName:
 * multiple logs" 拒绝——但随发 server.xml 即双 LogConf（zeze.log/zeze_error.log）
 * 而随发前端无 logName 通路，核心查询在随发形态开箱恒拒。多份歧义的权威解是部署
 * 配置顺序：首个 LogConf 即主日志（文档化契约），缺省解析取之。</p>
 *
 * <p>驱动形态对齐 TestLog4jSessionAllDegraded + TestSearchBrowseUnknownServer：
 * 真回环 HttpServer 挂生产同款处理器，真 LogService/LogAgent（ServiceManager
 * disable），applyOnChanged 模拟 SM 推送注册真实服务端（applyOnChanged 为
 * Zeze.Services 包内可见，故本测试落位本包），前端形态请求体驱动。</p>
 *
 * <p>@Isolated：写 LogAgentManager/FileSessionManager 的 JVM 级静态状态，独占运行。</p>
 */
@Fast
@Isolated
@Extra
public class TestSearchBrowseMissingLogName {

	private static final String LOG_ACTIVE = "zeze.log";
	private static final String LOG_ACTIVE_2 = "zeze_error.log";

	/**
	 * search：前端形态请求（无 logName）经全服视图与单服务器视图都必须默认解析到
	 * 唯一 LogConf 名后正常查询成功——修复前两形态恒 "system error"。
	 */
	@Test
	public void testSearchMissingLogNameDefaultsToSoleLogConf() throws Exception {
		Task.tryInitThreadPool();
		var fixture = new SingleLogFixture();
		try (var unused = fixture.setup()) {
			fixture.registerRealServer();
			var client = HttpClient.newHttpClient();

			// 全服视图（前端默认 serverName 空串，searchParam 原样无 logName）。
			var json = post(client, fixture.searchUri, searchBody("", "nothing_will_match"));
			assertEquals(200, json.getIntValue("status"), "全服视图缺省 logName 必须默认解析后成功: " + json);
			assertEquals("success", json.getString("desc"), "缺省 logName 不再坍缩 system error: " + json);
			assertTrue(json.containsKey("data"), "成功应答必须携带查询结果 data");

			// 单服务器视图：显式选中的日志服务器名，仍无 logName。
			json = post(client, fixture.searchUri, searchBody(fixture.serverIdentity, "nothing_will_match"));
			assertEquals(200, json.getIntValue("status"), "单服务器视图缺省 logName 必须默认解析后成功: " + json);
			assertEquals("success", json.getString("desc"));
		} finally {
			fixture.close();
		}
	}

	/** browse：同款缺省解析（与 search 共用 SearchLogParam，处理器各自入口）。 */
	@Test
	public void testBrowseMissingLogNameDefaultsToSoleLogConf() throws Exception {
		Task.tryInitThreadPool();
		var fixture = new SingleLogFixture();
		try (var unused = fixture.setup()) {
			fixture.registerRealServer();
			var client = HttpClient.newHttpClient();

			var json = post(client, fixture.browseUri, browseBody(""));
			assertEquals(200, json.getIntValue("status"), "browse 全服视图缺省 logName 必须默认解析后成功: " + json);
			assertEquals("success", json.getString("desc"), "缺省 logName 不再坍缩 system error: " + json);
		} finally {
			fixture.close();
		}
	}

	/**
	 * 多份 LogConf（ZokerManager 随发 server.xml 即两份：zeze.log/zeze_error.log，
	 * 主序 zeze.log 在前）下缺省 logName 必须默认取主 LogConf 后查询成功——
	 * 随发前端无 logName 通路，"多份回拒"使核心查询在随发形态开箱恒拒
	 * （FND34 zokermanager-01）。主/次日志各写独有关键词：命中主日志关键词证明
	 * 缺省取主日志；查次日志关键词返回空排除"默认取了任意一份"的偶然通过。
	 */
	@Test
	public void testSearchMissingLogNameDefaultsToPrimaryLogConf() throws Exception {
		Task.tryInitThreadPool();
		var fixture = new SingleLogFixture();
		try (var unused = fixture.setup(true)) {
			fixture.registerRealServer();
			var client = HttpClient.newHttpClient();

			// 全服视图（前端默认 serverName 空串，searchParam 原样无 logName）：
			// 主日志独有关键词必须命中——修复前该形态被 "missing logName: multiple logs" 恒拒。
			var json = post(client, fixture.searchUri, searchBody("", "alpha-in-primary-log"));
			assertEquals(200, json.getIntValue("status"), "多日志部署缺省 logName 必须默认取主 LogConf: " + json);
			assertEquals("success", json.getString("desc"), "缺省 logName 不再被多日志形态拒绝: " + json);
			assertTrue(json.toString().contains("alpha-in-primary-log"), "命中必须来自主日志（首个 LogConf）: " + json);

			// 同一缺省请求查次日志（zeze_error.log）独有关键词：结果必须为空——
			// 默认目标是主日志而非任意一份。
			json = post(client, fixture.searchUri, searchBody("", "omega-in-error-log"));
			assertEquals(200, json.getIntValue("status"), "次日志关键词在主日志上无匹配，查询本身仍成功: " + json);
			assertFalse(json.toString().contains("omega-in-error-log"),
					"缺省目标必须是主日志，不得命中次日志内容: " + json);

			// 单服务器视图同款缺省解析（显式选中的日志服务器名，仍无 logName）。
			json = post(client, fixture.searchUri, searchBody(fixture.serverIdentity, "alpha-in-primary-log"));
			assertEquals(200, json.getIntValue("status"), "单服务器视图缺省 logName 必须默认取主 LogConf: " + json);
			assertTrue(json.toString().contains("alpha-in-primary-log"));
		} finally {
			fixture.close();
		}
	}

	/**
	 * 零 LogConf（部署配置漏配整个 LogServiceConf 日志集）无法确定默认：缺省
	 * logName 必须入口即拒，desc 明确指向 missing logName——语义沿革后"多份回拒"
	 * 改为取主，零份仍是唯一无法解析的缺省形态。
	 */
	@Test
	public void testSearchMissingLogNameWithoutLogConfRejected() throws Exception {
		Task.tryInitThreadPool();
		var fixture = new SingleLogFixture();
		try (var unused = fixture.setup()) {
			// 注入零 LogConf 配置的 agent（错误在入口即拒，无需注册真实服务端）。
			try (var unused2 = fixture.installAgent(new LogAgent(newConfigWithoutLogConf()))) {
				var client = HttpClient.newHttpClient();
				var json = post(client, fixture.searchUri, searchBody("", "nothing_will_match"));
				assertEquals(500, json.getIntValue("status"), "零 LogConf 缺省 logName 必须 errorResult: " + json);
				var desc = json.getString("desc");
				assertTrue(desc.startsWith("missing logName"), "desc 必须明确指向缺参: " + desc);
			}
		} finally {
			fixture.close();
		}
	}

	// ---------------------------------------------------------------- fixture

	/**
	 * 真回环 HttpServer（生产同款处理器注册形态）+ 真 LogService/LogAgent。
	 * setup 返回的 AutoCloseable 恢复 LogAgentManager 静态注入；close 停服务并
	 * 清 FileSessionManager 静态绑定（成功请求对 127.0.0.1 留下的绑定不得泄漏给
	 * 同 JVM 后续用例）。
	 */
	private static final class SingleLogFixture implements AutoCloseable {
		final Path logDir;
		final int logServicePort;
		final String serverIdentity;
		final Netty netty = new Netty(1);
		final HttpServer server = new HttpServer();
		URI searchUri;
		URI browseUri;
		LogService logService;
		LogAgent logAgent;

		SingleLogFixture() throws Exception {
			logDir = Files.createTempDirectory("zeze-zoker-missing-logname");
			try (var ss = new ServerSocket(0)) {
				logServicePort = ss.getLocalPort();
			}
			serverIdentity = "LogService_test_127.0.0.1_" + logServicePort;
		}

		/** 单日志部署形态（既有缺省用例）：唯一 LogConf 名即默认。 */
		AutoCloseable setup() throws Exception {
			return setup(false);
		}

		/**
		 * 指定日志集形态启动：LogService 与 LogAgent 同源同款配置（真部署形态）。
		 * twoLogs 时先落盘两份日志内容（主日志 zeze.log 在前，含各自独有关键词，
		 * 行首时间串对齐默认 LogTimeFormat "yy-MM-dd HH:mm:ss.SSS"）再构造
		 * LogService——索引由构造期 loadIndex 建好。
		 */
		AutoCloseable setup(boolean twoLogs) throws Exception {
			if (twoLogs) {
				Files.write(logDir.resolve(LOG_ACTIVE),
						("26-03-01 10:00:00.000 alpha-in-primary-log\n").getBytes(StandardCharsets.UTF_8));
				Files.write(logDir.resolve(LOG_ACTIVE_2),
						("26-03-01 10:00:00.000 omega-in-error-log\n").getBytes(StandardCharsets.UTF_8));
			}
			logService = new LogService(newConfig(logServicePort, logDir, twoLogs));
			logService.start();
			server.addHandler("/api/search", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
					new SearchLogHandle());
			server.addHandler("/api/browse", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
					new BrowseLogHandle());
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			searchUri = URI.create("http://127.0.0.1:" + port + "/api/search");
			browseUri = URI.create("http://127.0.0.1:" + port + "/api/browse");
			return installAgent(new LogAgent(newConfig(logServicePort, logDir, twoLogs)));
		}

		/** 注入持有指定 agent 的 LogAgentManager，返回恢复原静态值的 AutoCloseable。 */
		AutoCloseable installAgent(LogAgent agent) throws Exception {
			logAgent = agent;
			agent.start();

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

		/** 模拟 SM 推送注册真实 LogService，并等连接就绪。 */
		void registerRealServer() throws Exception {
			var edit = new BEditService();
			edit.getAdd().add(new BServiceInfo("Zeze.LogService", serverIdentity, 0, "127.0.0.1", logServicePort));
			logAgent.applyOnChanged(edit);
			var deadline = System.currentTimeMillis() + 10_000;
			while (logAgent.__getLogServer(serverIdentity).TryGetReadySocket() == null) {
				if (System.currentTimeMillis() > deadline)
					throw new IllegalStateException("等待真实 LogService 连接就绪超时");
				Thread.sleep(50);
			}
		}

		@Override
		public void close() throws Exception {
			// 先关查询会话（服务端仍存活，CloseSession 快速成功），再停 agent/服务，
			// 最后摘除静态绑定（私有 map 反射，防泄漏给同 JVM 后续用例）。
			try {
				var mapField = FileSessionManager.class.getDeclaredField("map");
				mapField.setAccessible(true);
				@SuppressWarnings("unchecked")
				var map = (java.util.Map<String, Object>)mapField.get(null);
				for (var it = map.entrySet().iterator(); it.hasNext(); ) {
					var entry = it.next();
					var session = entry.getValue().getClass().getMethod("session").invoke(entry.getValue());
					if (session instanceof AutoCloseable c)
						c.close();
					it.remove();
				}
			} catch (Exception e) {
				// 清理尽力而为，不掩盖测试结果。
			}
			if (logAgent != null)
				logAgent.stop();
			if (logService != null)
				logService.stop();
			server.close();
			netty.close();
			deleteBestEffort(logDir);
		}
	}

	/** 前端 search 请求体形态（searchParam 字面量，无 logName 字段；words 可变）。 */
	private static String searchBody(String serverName, String words) {
		return "{\"serverName\":\"" + serverName + "\",\"reset\":true,\"offsetFactor\":0,\"limit\":10,"
				+ "\"beginTime\":\"\",\"endTime\":\"\",\"words\":\"" + words + "\","
				+ "\"containsType\":1,\"pattern\":\"\",\"changeSession\":true}";
	}

	/** 前端 browse 请求体形态（同上，browse 使用 offsetFactor）。 */
	private static String browseBody(String serverName) {
		return "{\"serverName\":\"" + serverName + "\",\"reset\":true,\"offsetFactor\":0.5,\"limit\":10,"
				+ "\"beginTime\":\"\",\"endTime\":\"\",\"words\":\"nothing_will_match\","
				+ "\"containsType\":1,\"pattern\":\"\",\"changeSession\":true}";
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

	/** DOM 构造配置：LogServiceConf（单/双 LogConf）+ LogService.Server Acceptor。 */
	private static Config newConfig(int logServicePort, Path logDir, boolean twoLogs) throws Exception {
		var config = new Config();
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		var customize = doc.createElement("CustomizeConf");
		customize.appendChild(newLogConf(doc, LOG_ACTIVE, logDir));
		if (twoLogs)
			customize.appendChild(newLogConf(doc, LOG_ACTIVE_2, logDir));
		config.getCustomizes().put("LogServiceConf", customize);

		if (logServicePort > 0) {
			var serviceConfElem = doc.createElement("ServiceConf");
			serviceConfElem.setAttribute("Name", "Zeze.LogService.Server");
			var acceptorElem = doc.createElement("Acceptor");
			acceptorElem.setAttribute("Ip", "127.0.0.1");
			acceptorElem.setAttribute("Port", String.valueOf(logServicePort));
			serviceConfElem.appendChild(acceptorElem);
			new ServiceConf(config, serviceConfElem);
		}
		return config;
	}

	/** DOM 构造零 LogConf 配置：LogServiceConf 无日志集（部署漏配形态）。 */
	private static Config newConfigWithoutLogConf() throws Exception {
		var config = new Config();
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		config.getCustomizes().put("LogServiceConf", doc.createElement("CustomizeConf"));
		return config;
	}

	private static Element newLogConf(Document doc, String logActive, Path logDir) {
		var logConfElem = doc.createElement("LogConf");
		logConfElem.setAttribute("LogActive", logActive);
		logConfElem.setAttribute("LogDir", logDir.toString());
		return logConfElem;
	}

	// 尽力删除：留给系统临时目录清理，失败不干扰测试结果。
	private static void deleteBestEffort(Path dir) {
		try (var walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (Exception e) {
					// ignore
				}
			});
		} catch (Exception e) {
			// ignore
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
