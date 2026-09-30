package Zeze.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import Zeze.log.handle.SearchLogHandle;

import harness.Fast;

/**
 * 会话回执比对的查询条件维度直测：随发前端首次搜索成功后 changeSession/reset
 * 恒置 false，"搜索"按钮原样提交 searchParam——改关键词再搜时若复用判定只比
 * (视图, serverName, logName) 三元组，旧游标会话被复用、reset=false 续扫；
 * 服务端仅 beginTime 有去重哨兵（Log4jSession.trySetBeginTime），words 变更
 * 不触发重定位——新条件在游标之前（更早时段）的匹配被静默跳过且 200 success。
 * 修复后绑定回执纳入条件指纹：条件变即视同 changeSession 关旧建新，新条件从
 * 查询窗口头完整求值。
 *
 * <p>驱动形态对齐 TestSearchBrowseMissingLogName：真回环 HttpServer 挂生产同款
 * 处理器，真 LogService/LogAgent（ServiceManager disable），applyOnChanged 模拟
 * SM 推送注册真实服务端（包内可见，故落位本包），前端形态请求体驱动。</p>
 *
 * <p>@Isolated：写 LogAgentManager/FileSessionManager 的 JVM 级静态状态，独占运行。</p>
 */
@Fast
@Isolated
public class TestSearchConditionChangeRebuildsSession {

	private static final String LOG_ACTIVE = "zeze.log";
	// 夹具时间基线固定（内容时间与墙钟无关），行距 1s 对齐真实日志形态。
	private static final int FILLER_LINES = 20;

	/**
	 * 核心：同会话（三元组不变、前端不置 changeSession/reset）改关键词再搜，
	 * 新条件的早段匹配（首行，位于旧游标之前）必须出现——修复前旧游标续扫
	 * 静默跳过，只剩晚段匹配且 200 success。
	 */
	@Test
	public void testChangedWordsSearchDeliversEarlyMatches() throws Exception {
		Task.tryInitThreadPool();
		var fixture = new Fixture();
		try (var unused = fixture.setup()) {
			fixture.registerRealServer();
			var client = HttpClient.newHttpClient();

			// 首次搜索（前端形态：changeSession=true 首搜置位）：关键词 filler 翻一页，
			// 服务端游标推进到首屏 filler 之后（首行 early-marker 已在游标之前）。
			var json = post(client, fixture.searchUri, searchBody("filler-line", true, true));
			assertEquals(200, json.getIntValue("status"), "首搜必须成功: " + json);
			assertTrue(json.toString().contains("filler-line"), "首搜返回 filler 页: " + json);
			assertTrue(!json.toString().contains("alpha-early-marker"), "夹具形态自检：首屏在 early 之后");

			// 改关键词再点"搜索"（前端成功回调后 changeSession/reset 恒 false，原样提交）：
			// 新条件的完整结果（早段+晚段）都必须出现——修复前只从旧游标续扫出晚段。
			json = post(client, fixture.searchUri, searchBody("alpha-", false, false));
			assertEquals(200, json.getIntValue("status"), "改条件再搜必须成功: " + json);
			assertTrue(json.toString().contains("alpha-early-marker"),
					"新条件的早段匹配（旧游标之前）不得静默缺失: " + json);
			assertTrue(json.toString().contains("alpha-late-marker"),
					"新条件的晚段匹配照常可达: " + json);
		} finally {
			fixture.close();
		}
	}

	/**
	 * 条件不变续页（翻页路径）：同条件再搜必须复用会话游标续扫（不重放首页）——
	 * 条件指纹稳定不动，防修复把正常翻页误伤成每页重建。
	 */
	@Test
	public void testUnchangedConditionContinuesFromCursor() throws Exception {
		Task.tryInitThreadPool();
		var fixture = new Fixture();
		try (var unused = fixture.setup()) {
			fixture.registerRealServer();
			var client = HttpClient.newHttpClient();

			// 首屏：filler 前 5 行（limit=5）。
			var json = post(client, fixture.searchUri, searchBody("filler-line", true, true));
			assertTrue(json.toString().contains("filler-line-0001"), "首屏从首条 filler 起: " + json);
			assertTrue(!json.toString().contains("filler-line-0006"), "limit=5 只含前 5 条");

			// 同条件续页（reset=false/changeSession=false，前端翻页形态）：必须从游标
			// 续扫第 6 条起，而不是重放首页（每页重建=结果重复抖动）。
			json = post(client, fixture.searchUri, searchBody("filler-line", false, false));
			assertTrue(json.toString().contains("filler-line-0006"), "同条件续页从游标继续: " + json);
			assertTrue(!json.toString().contains("filler-line-0001"), "续页不得重放已投递首页");
		} finally {
			fixture.close();
		}
	}

	// ---------------------------------------------------------------- fixture

	/**
	 * 真回环 HttpServer（生产同款处理器注册形态）+ 真 LogService/LogAgent（单日志）。
	 * 日志内容：首行 alpha-early-marker（新条件早段匹配锚点）、其后 20 行 filler、
	 * 末行 alpha-late-marker——"改条件再搜"的早段锚点在首屏 filler 的游标之前。
	 */
	private static final class Fixture implements AutoCloseable {
		final Path logDir;
		final int logServicePort;
		final String serverIdentity;
		final Netty netty = new Netty(1);
		final HttpServer server = new HttpServer();
		URI searchUri;
		LogService logService;
		LogAgent logAgent;

		Fixture() throws Exception {
			logDir = Files.createTempDirectory("zeze-zoker-condition-change");
			try (var ss = new ServerSocket(0)) {
				logServicePort = ss.getLocalPort();
			}
			serverIdentity = "LogService_test_127.0.0.1_" + logServicePort;
			var sb = new StringBuilder();
			sb.append("26-03-01 10:00:00.000 alpha-early-marker\n");
			for (var i = 1; i <= FILLER_LINES; ++i)
				sb.append(String.format("26-03-01 10:00:%02d.000 filler-line-%04d%n", i, i));
			sb.append("26-03-01 10:01:00.000 alpha-late-marker\n");
			Files.write(logDir.resolve(LOG_ACTIVE), sb.toString().getBytes(StandardCharsets.UTF_8));
		}

		AutoCloseable setup() throws Exception {
			logService = new LogService(newConfig(logServicePort, logDir));
			logService.start();
			server.addHandler("/api/search", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
					new SearchLogHandle());
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			searchUri = URI.create("http://127.0.0.1:" + port + "/api/search");
			return installAgent(new LogAgent(newConfig(logServicePort, logDir)));
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

	/** 前端 search 请求体形态（searchParam 字面量；changeSession/reset 按用例摆态）。 */
	private static String searchBody(String words, boolean changeSession, boolean reset) {
		return "{\"serverName\":\"\",\"reset\":" + reset + ",\"offsetFactor\":0,\"limit\":5,"
				+ "\"beginTime\":\"\",\"endTime\":\"\",\"words\":\"" + words + "\","
				+ "\"containsType\":1,\"pattern\":\"\",\"changeSession\":" + changeSession + "}";
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

	/** DOM 构造配置：LogServiceConf（单 LogConf）+ LogService.Server Acceptor。 */
	private static Config newConfig(int logServicePort, Path logDir) throws Exception {
		var config = new Config();
		var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		var customize = doc.createElement("CustomizeConf");
		var logConfElem = doc.createElement("LogConf");
		logConfElem.setAttribute("LogActive", LOG_ACTIVE);
		logConfElem.setAttribute("LogDir", logDir.toString());
		customize.appendChild(logConfElem);
		config.getCustomizes().put("LogServiceConf", customize);

		var serviceConfElem = doc.createElement("ServiceConf");
		serviceConfElem.setAttribute("Name", "Zeze.LogService.Server");
		var acceptorElem = doc.createElement("Acceptor");
		acceptorElem.setAttribute("Ip", "127.0.0.1");
		acceptorElem.setAttribute("Port", String.valueOf(logServicePort));
		serviceConfElem.appendChild(acceptorElem);
		new ServiceConf(config, serviceConfElem);
		return config;
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
