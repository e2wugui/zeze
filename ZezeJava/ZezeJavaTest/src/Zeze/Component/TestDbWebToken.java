package Zeze.Component;

import harness.FastServerIds;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Component.DbWeb;
import Zeze.Config;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * FND14 comp-02回归（用户2026-09-25裁决：FND8-66范式的可选token）：DbWeb七个servlet暴露
 * 全库读/写/删/清表，挂载在共享HttpServer上无法按端点绑地址（官方样例不传host即绑0.0.0.0），
 * 全链路零鉴权——可达即越权。修复：六个数据端点（ListTable/WalkTable/GetValue/PutRecord/
 * DeleteRecord/ClearTable）强制token（X-Zeze-Token头或token查询参数，常量时间比较，失配403）；
 * Index保持开放（静态外壳无数据）；无参构造自动生成token打日志（零配置可用），显式构造传入。
 * 断言：无token/错token必403（修复前红：200裸通），正确token（query与header两形态）通，Index开放。
 */
@Fast
public class TestDbWebToken {
	private static final String TOKEN = "dbweb-test-token";
	private static final String LIST = "/Zeze/Builtin/DbWeb/ListTable";
	private static final String INDEX = "/Zeze/Builtin/DbWeb/Index";

	private static final int ServerId = FastServerIds.TEST_DB_WEB_TOKEN;

	private static Application app;
	private static HttpServer httpServer;
	private static Netty netty;
	private static int port;

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(ServerId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("dbweb_token_" + ServerId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("TestDbWebToken", conf);
		app.start();
		var dbWeb = new DbWeb(TOKEN); // 显式token（无参构造的自动生成+日志形态由启动日志断言难做，走显式）
		dbWeb.Initialize(new AppBase() {
			@Override
			public Application getZeze() {
				return app;
			}
		});
		netty = new Netty(1);
		httpServer = new HttpServer(app);
		dbWeb.RegisterHttpServlet(httpServer);
		port = ((InetSocketAddress)httpServer.start(netty, 0).sync().channel().localAddress()).getPort();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (httpServer != null)
			httpServer.close();
		if (netty != null)
			netty.close();
		if (app != null)
			app.stop();
	}

	private static int get(String path, String tokenQuery, String tokenHeader) throws Exception {
		var builder = HttpRequest.newBuilder()
				.uri(URI.create("http://127.0.0.1:" + port + path + (tokenQuery != null ? "?token=" + tokenQuery : "")))
				.timeout(Duration.ofSeconds(10))
				.GET();
		if (tokenHeader != null)
			builder.header("X-Zeze-Token", tokenHeader);
		return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
	}

	@Test
	public void testDataEndpointsRequireToken() throws Exception {
		// 无token：必403（修复前红：200裸通，任何可达者可清表）
		Assertions.assertEquals(403, get(LIST, null, null), "无token的DbWeb数据请求必须403");

		// 错token：必403
		Assertions.assertEquals(403, get(LIST, "wrong-token", null), "错token必须403");

		// 正确token：query形态通
		Assertions.assertEquals(200, get(LIST, TOKEN, null), "正确token（query参数）必须放行");

		// 正确token：header形态通
		Assertions.assertEquals(200, get(LIST, null, TOKEN), "正确token（X-Zeze-Token头）必须放行");
	}

	@Test
	public void testIndexStaysOpen() throws Exception {
		// Index是静态外壳（无数据面），保持开放：浏览器直接打开不需token，JS层带token访问数据端点
		Assertions.assertEquals(200, get(INDEX, null, null), "Index页保持开放");
	}
}
