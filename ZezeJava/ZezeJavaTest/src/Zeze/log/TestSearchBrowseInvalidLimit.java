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
 * /api/search 与 /api/browse 的 limit 下界校验直测（FND30 zokermanager-04）：
 * limit 缺省（JSON 漏字段→默认 0）/0/负值被处理器原样透传，服务器把
 * {@code limit<=0} 定义为"翻页终结"——零扫描返回空成功 remain=false，与
 * "查完无匹配"应答同形；且请求已先创建查询会话。修复后入口即拒绝：
 * {@code BaseResponse.errorResult("invalid limit")}，不触会话层。
 *
 * <p>驱动方式：真回环 HttpServer 挂生产同款处理器（对齐 TestZokerManagerBindAndToken
 * 的真 HTTP 形态；注册形态对齐 LogAgentManager：Serializable+Normal）。本环境无
 * LogAgent（LogAgentManager.getInstance()==null）——畸形 limit 若漏过校验会进到
 * getLogAgent() 抛 NPE 落入 catch，desc 为 "system error" 而非 "invalid limit"，
 * 恰构成红/绿判别；"空成功"应答形态需真 LogAgent/LogService（不在直测面，
 * 见服务器侧 Log4jSession.searchContains/browseContains 的 limit<=0 卫语句）。</p>
 *
 * <p>@Isolated：TestZokerManagerBindAndToken 会临时 configure ApiToken（JVM 级静态），
 * TestD06/D07 会写 FileSessionManager 静态表——独占运行避免与它们的摆盘互踩。</p>
 */
@Fast
@Isolated
public class TestSearchBrowseInvalidLimit {

	/** search：limit=0/负/漏传（默认0）都必须 errorResult("invalid limit")，且不建会话。 */
	@Test
	public void testSearchRejectsNonPositiveLimit() throws Exception {
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

			assertInvalidLimit(client, uri, body(0), "search limit=0");
			assertInvalidLimit(client, uri, body(-5), "search limit=-5");
			// 漏传 limit：JSON 缺字段反序列化为字段默认值 0，同 0 形态。
			assertInvalidLimit(client, uri,
					"{\"serverName\":\"game1\",\"logName\":\"zeze\",\"words\":\"error\",\"containsType\":1}",
					"search 漏传 limit（默认0）");

			assertNoSessionBinding();
			// 负对照：合法 limit 不在校验拒绝面——本环境无 LogAgent，进到代理层即 system error
			// （证明拒绝只收敛 limit<=0，不扩大化误伤正常请求）。
			assertEquals("system error", post(client, uri, body(10)).getString("desc"),
					"合法 limit 不得被 invalid limit 拒绝");
			assertNoSessionBinding();
		} finally {
			server.close();
			netty.close();
		}
	}

	/** browse：同款下界校验（与 search 共用 SearchLogParam，处理器各自入口）。 */
	@Test
	public void testBrowseRejectsNonPositiveLimit() throws Exception {
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

			assertInvalidLimit(client, uri, body(0), "browse limit=0");
			assertInvalidLimit(client, uri, body(-1), "browse limit=-1");
			assertInvalidLimit(client, uri,
					"{\"serverName\":\"game1\",\"logName\":\"zeze\",\"words\":\"error\",\"containsType\":1}",
					"browse 漏传 limit（默认0）");

			assertNoSessionBinding();
		} finally {
			server.close();
			netty.close();
		}
	}

	private static String body(int limit) {
		return "{\"serverName\":\"game1\",\"logName\":\"zeze\",\"words\":\"error\",\"containsType\":1,\"limit\":"
				+ limit + "}";
	}

	/** 断言 errorResult("invalid limit")：BaseResponse status=500 + 明确 desc（HTTP 200 承载，既有约定）。 */
	private static void assertInvalidLimit(HttpClient client, URI uri, String body, String what) throws Exception {
		var json = post(client, uri, body);
		assertEquals(500, json.getIntValue("status"), what + "：必须是 errorResult 而非空成功应答");
		assertEquals("invalid limit", json.getString("desc"), what + "：desc 必须明确指向参数错误");
	}

	/** 畸形请求不得在会话表留下绑定（不创建/复用查询会话、不透传服务器）。 */
	private static void assertNoSessionBinding() throws Exception {
		var clientAddr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0);
		assertNull(FileSessionManager.get(clientAddr), "limit<=0 的请求不得创建/复用查询会话");
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
