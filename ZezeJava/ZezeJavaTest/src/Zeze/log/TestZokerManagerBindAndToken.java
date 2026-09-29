package Zeze.log;

import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import Zeze.Config;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZokerManager HTTP 管理口部署契约直测（FND29 zokermanager-02，直构形态对齐
 * TestD06LogSessionBinding 的纯逻辑直测 + 真回环 HttpServer 起 0 端口驱动 ApiToken 门）：
 * <ol>
 * <li>ZokerManagerConf 默认值与 CustomizeConf 解析——默认回环、空属性不回退旧全网卡语义；</li>
 * <li>checkDeployPolicy——非回环无 Token fail-fast（报错含风险与配置方法）、其余组合放行；</li>
 * <li>ApiToken——常量时间比较矩阵（对/错/缺失/未启用）；</li>
 * <li>真 HTTP 驱动：处理器同款入口门在缺头/错头/对头下分别回 401/401/200。</li>
 * </ol>
 * 绑定行为本身（HttpServer.start(netty, host, port) 按 host 绑定、host 空才绑全网卡）
 * 属 HttpServer 既有代码路径，不在直测面（论证收口）。
 */
@Fast
public class TestZokerManagerBindAndToken {

	/** 默认值：无任何 CustomizeConf 节时回环 + 不启用 token；空属性同样不改变默认。 */
	@Test
	public void testDefaultsLoopbackAndNoToken() throws Exception {
		var conf = parseConf("<zeze/>");
		assertEquals("127.0.0.1", conf.bind, "默认绑回环（迁移项：旧版本绑 0.0.0.0）");
		assertEquals("", conf.token);
		assertDoesNotThrow(() -> ZokerManagerConf.checkDeployPolicy(conf.bind, conf.token));

		// 属性空白（漏配/显式置空）必须保持默认回环——不允许借空值回到旧全网卡语义。
		var blank = parseConf("<zeze><CustomizeConf Name=\"ZokerManagerConf\" Bind=\"  \" Token=\" \"/></zeze>");
		assertEquals("127.0.0.1", blank.bind, "空白 Bind 不回退全网卡");
		assertEquals("", blank.token);
	}

	/** CustomizeConf 解析：Bind/Token 显式配置生效。 */
	@Test
	public void testParseExplicitBindAndToken() throws Exception {
		var conf = parseConf("<zeze><CustomizeConf Name=\"ZokerManagerConf\" "
				+ "Bind=\"0.0.0.0\" Token=\"secret-token\"/></zeze>");
		assertEquals("0.0.0.0", conf.bind);
		assertEquals("secret-token", conf.token);
	}

	/** fail-fast 矩阵：非回环（含全网卡与具体网卡IP）且无 Token 必须启动期拒绝；其余组合放行。 */
	@Test
	public void testNonLoopbackWithoutTokenFailsFast() {
		for (var bind : new String[] {"0.0.0.0", "10.20.30.40", "192.168.1.2"}) {
			var ex = assertThrows(IllegalStateException.class,
					() -> ZokerManagerConf.checkDeployPolicy(bind, ""),
					"bind=" + bind + " 无 token 必须拒绝启动");
			assertTrue(ex.getMessage().contains("ZokerManagerConf"), "报错指明配置节：\n" + ex.getMessage());
			assertTrue(ex.getMessage().contains("Token"), "报错写明配置方法（Token）：\n" + ex.getMessage());
		}
		assertThrows(IllegalStateException.class,
				() -> ZokerManagerConf.checkDeployPolicy("0.0.0.0", "   "), "纯空白 token 视为未配置");

		// 回环形态（127/8、localhost、::1 全写变体）无需 token。
		for (var bind : new String[] {"127.0.0.1", "127.0.0.5", "localhost", "::1", "0:0:0:0:0:0:0:1"})
			assertDoesNotThrow(() -> ZokerManagerConf.checkDeployPolicy(bind, ""),
					"bind=" + bind + " 是回环，无需 token");

		// 非回环配了 token：放行。
		assertDoesNotThrow(() -> ZokerManagerConf.checkDeployPolicy("0.0.0.0", "secret"));
	}

	/** token 比较矩阵：对/错/缺失/空串；未配置时isEnabled=false（check 放行语义由 HTTP 用例验证）。 */
	@Test
	public void testTokenCompareMatrix() {
		try {
			ApiToken.configure("secret");
			assertTrue(ApiToken.isEnabled());
			assertTrue(ApiToken.matches("secret"), "正确 token");
			assertFalse(ApiToken.matches("wrong"), "错误 token");
			assertFalse(ApiToken.matches("secret2"), "前缀延长不算匹配");
			assertFalse(ApiToken.matches("secre"), "前缀截短不算匹配");
			assertFalse(ApiToken.matches(null), "缺头");
			assertFalse(ApiToken.matches(""), "空串（web 前端未配置时的默认值）");
		} finally {
			ApiToken.configure(null);
		}
		assertFalse(ApiToken.isEnabled(), "未配置 token 时门禁关闭");
	}

	/** 真 HTTP 驱动 ApiToken.check（处理器同款入口门）：缺头/错头 401，对头 200，未启用全放行。 */
	@Test
	public void testTokenGateOverRealHttp() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/probe", 1024, TransactionLevel.None, DispatchMode.Normal, x -> {
			// 与 ZokerManager 四个处理器完全同款的入口门形态。
			if (!ApiToken.check(x))
				return;
			x.sendJson(io.netty.handler.codec.http.HttpResponseStatus.OK, "{\"status\":200}");
		});
		var client = HttpClient.newHttpClient();
		try {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			var base = URI.create("http://127.0.0.1:" + port + "/api/probe");

			ApiToken.configure("secret");
			try {
				assertEquals(401, status(client, HttpRequest.newBuilder(base).build()), "缺 Authorization 头：401");
				assertEquals(401, status(client, HttpRequest.newBuilder(base)
						.header("Authorization", "wrong").build()), "错 token：401");
				assertEquals(401, status(client, HttpRequest.newBuilder(base)
						.header("Authorization", "").build()), "空 token（前端未配置形态）：401");
				assertEquals(200, status(client, HttpRequest.newBuilder(base)
						.header("Authorization", "secret").build()), "对 token：200");
			} finally {
				ApiToken.configure(null);
			}

			// 未配置 token（默认回环部署形态）：无头也放行——存量本机使用方式不变。
			assertEquals(200, status(client, HttpRequest.newBuilder(base).build()), "未启用门禁：放行");
		} finally {
			server.close();
			netty.close();
			ApiToken.configure(null);
		}
	}

	private static int status(HttpClient client, HttpRequest request) throws Exception {
		return client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode();
	}

	private static ZokerManagerConf parseConf(String xml) throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		var config = new Config();
		config.parse(doc.getDocumentElement());
		var conf = new ZokerManagerConf();
		config.parseCustomize(conf);
		return conf;
	}

	/** 兜底清理静态 token 状态，防止其他用例（或重跑）受本类 configure 残留影响。 */
	@AfterAll
	public static void resetToken() {
		ApiToken.configure(null);
	}
}
