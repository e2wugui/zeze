package Zeze.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import Zeze.log.handle.BrowseLogHandle;
import Zeze.log.handle.SearchLogHandle;

import harness.Fast;

/**
 * /api/search 与 /api/browse 的参数级入口校验直测（containsType 枚举、words/pattern
 * 双空、browse 的 offsetFactor∈[0,1)）：透传时服务端对这三者回非零结果码，与死会话的
 * LogicError 同族——operateRecovering 会误判会话级死亡，把健康查询会话整组关旧建新后
 * 同参数重试再失败，最终恒 system error 且错误不可区分；offsetFactor 负值更在服务端
 * 静默退化为无上下文的过滤搜索。修复后入口即拒：明确 errorResult（invalid containsType
 * / empty condition / invalid offsetFactor），不建/复用会话。
 *
 * <p>驱动方式对齐 TestSearchBrowseInvalidLimit：真回环 HttpServer 挂生产同款处理器；
 * 本环境无 LogAgent（LogAgentManager.getInstance()==null）——漏过校验的参数会进到
 * getLogAgent() 抛 NPE 落入 catch，desc 为 "system error" 而非明确参数错误，恰构成
 * 红/绿判别；服务端的码级分诊（参数级不拆会话）由 TestParamErrorNoSessionRebuild
 * 与 TestSessionResultCodeCheck 覆盖。</p>
 *
 * <p>@Isolated：FileSessionManager/ApiToken 为 JVM 级静态状态，独占运行避免与其他
 * 摆盘互踩（对齐 TestSearchBrowseInvalidLimit）。</p>
 */
@Fast
@Isolated
public class TestSearchBrowseInvalidCondition {

	/** search：非法 containsType 与 words/pattern 双空（时间-only）必须入口即拒。 */
	@Test
	public void testSearchRejectsInvalidCondition() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/search", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new SearchLogHandle());
		var client = HttpClient.newHttpClient();
		try {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			var uri = URI.create("http://127.0.0.1:" + port + "/api/search");

			assertRejected(client, uri,
					"{\"serverName\":\"game1\",\"logName\":\"zeze\",\"words\":\"error\",\"containsType\":7,\"limit\":10}",
					"invalid containsType", "search containsType=7");
			assertRejected(client, uri,
					"{\"serverName\":\"game1\",\"logName\":\"zeze\",\"words\":\"error\",\"containsType\":-1,\"limit\":10}",
					"invalid containsType", "search containsType=-1");
			// words 与 pattern 双空（时间范围-only）：服务端空条件以非零码应答，与死会话同族。
			assertRejected(client, uri,
					"{\"serverName\":\"game1\",\"logName\":\"zeze\",\"beginTime\":\"2026-01-01 00:00:00\","
							+ "\"endTime\":\"2026-01-02 00:00:00\",\"limit\":10}",
					"empty condition", "search 双空条件（漏传 words/pattern）");
			assertRejected(client, uri,
					"{\"serverName\":\"game1\",\"logName\":\"zeze\",\"words\":\" , \",\"pattern\":\"  \",\"limit\":10}",
					"empty condition", "search 空白段 words + 空白 pattern 归一后仍双空");

			assertNoSessionBinding();
			// 负对照：合法参数不在拒绝面（search 不使用 offsetFactor，坏 offsetFactor 也不拒）——
			// 进到代理层即 system error，证明校验只收敛参数级错误，不扩大化误伤正常请求。
			assertEquals("system error", post(client, uri,
					"{\"serverName\":\"game1\",\"logName\":\"zeze\",\"words\":\"error\",\"containsType\":1,"
							+ "\"offsetFactor\":2.0,\"limit\":10}").getString("desc"),
					"合法 search 参数不得被参数校验拒绝");
			assertNoSessionBinding();
		} finally {
			server.close();
			netty.close();
		}
	}

	/** browse：同款条件校验 + offsetFactor∈[0,1)（服务端负值静默退化、≥1 抛非零码）。 */
	@Test
	public void testBrowseRejectsInvalidConditionAndOffset() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/browse", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new BrowseLogHandle());
		var client = HttpClient.newHttpClient();
		try {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			var uri = URI.create("http://127.0.0.1:" + port + "/api/browse");

			assertRejected(client, uri,
					"{\"serverName\":\"game1\",\"logName\":\"zeze\",\"words\":\"error\",\"containsType\":9,\"limit\":10}",
					"invalid containsType", "browse containsType=9");
			assertRejected(client, uri,
					"{\"serverName\":\"game1\",\"logName\":\"zeze\",\"limit\":10,\"offsetFactor\":0.5}",
					"empty condition", "browse 双空条件（漏传 words/pattern）");
			assertRejected(client, uri,
					"{\"serverName\":\"game1\",\"logName\":\"zeze\",\"words\":\"error\",\"limit\":10,\"offsetFactor\":2.0}",
					"invalid offsetFactor", "browse offsetFactor=2.0");
			assertRejected(client, uri,
					"{\"serverName\":\"game1\",\"logName\":\"zeze\",\"words\":\"error\",\"limit\":10,\"offsetFactor\":-0.5}",
					"invalid offsetFactor", "browse offsetFactor=-0.5（服务端静默退化形态）");

			assertNoSessionBinding();
			// 负对照：合法 browse 参数（words + containsType + offsetFactor∈[0,1)）不被拒。
			assertEquals("system error", post(client, uri,
					"{\"serverName\":\"game1\",\"logName\":\"zeze\",\"words\":\"error\",\"containsType\":1,"
							+ "\"limit\":10,\"offsetFactor\":0.5}").getString("desc"),
					"合法 browse 参数不得被参数校验拒绝");
			assertNoSessionBinding();
		} finally {
			server.close();
			netty.close();
		}
	}

	/** 断言 errorResult(desc)：BaseResponse status=500 + 明确 desc（HTTP 200 承载，既有约定）。 */
	private static void assertRejected(HttpClient client, URI uri, String body, String desc, String what)
			throws Exception {
		var json = post(client, uri, body);
		assertEquals(500, json.getIntValue("status"), what + "：必须是 errorResult 而非透传服务器");
		assertEquals(desc, json.getString("desc"), what + "：desc 必须明确指向参数错误");
	}

	/** 畸形请求不得在会话表留下绑定（不创建/复用查询会话、不透传服务器）。 */
	private static void assertNoSessionBinding() throws Exception {
		var clientAddr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0);
		assertNull(FileSessionManager.get(clientAddr), "参数级拒绝的请求不得创建/复用查询会话");
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
}
