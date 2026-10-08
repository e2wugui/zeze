package Zeze.log;

import harness.Extra;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import Zeze.log.handle.SearchLogHandle;

import harness.Fast;

/**
 * /api/* 处理器的浏览器源防御直测（Origin/Host 双防线）：默认回环+无 Token 形态下，
 * 受害者浏览器代发的请求可打穿 ApiToken 门——跨站简单请求（CSRF）触发查询副作用、
 * DNS rebinding（attacker.com 解析到 127.0.0.1，Host=攻击者域名）完整读取响应。
 * 修复后处理器入口在 Token 门后追加 Origin 校验（存在且非同源→403）与 Host 校验
 * （回环绑定下非回环主机名→403）；同源/无 Origin 的本机请求不受影响。
 *
 * <p>驱动方式：真回环 HttpServer 挂生产 SearchLogHandle（有 catch 兜底，无 LogAgent
 * 时既有形态是 HTTP 200 + "system error"——恰构成放行路径的对照）；Host/Origin 头
 * 用裸 socket 伪造（java HttpClient 禁改 Host 头）。本环境无 LogAgent：放行形态
 * 落入处理器 catch 回 system error（200），拒绝形态必须是 403。</p>
 *
 * <p>@Isolated：ApiToken/BrowserOriginGuard 为 JVM 级静态状态，独占运行。</p>
 */
@Fast
@Isolated
@Extra
public class TestAdminApiRejectsForeignOrigin {

	/** DNS rebinding 读取形态：Host 与 Origin 一致（同为攻击者域名）但非回环——Host 防线拒。 */
	@Test
	public void testReboundHostRejected() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/search", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new SearchLogHandle());
		try {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			assertEquals(403, rawPostStatus(port, "attacker.com:9980", "http://attacker.com:9980"),
					"rebinding 形态（Host=Origin=攻击者域名）必须 403");
			assertEquals(403, rawPostStatus(port, "192.168.1.2:9980", null),
					"回环绑定下非回环 Host（无 Origin 的畸形/直连形态）必须 403");
		} finally {
			server.close();
			netty.close();
		}
	}

	/** CSRF 副作用形态：Host 是本机但 Origin 是攻击者页面（跨站）——Origin 防线拒。 */
	@Test
	public void testCrossSiteOriginRejected() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/search", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new SearchLogHandle());
		try {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			assertEquals(403, rawPostStatus(port, "127.0.0.1:" + port, "https://evil.com"),
					"跨站 Origin（与 Host 不同源）必须 403");
			assertEquals(403, rawPostStatus(port, "127.0.0.1:" + port, "http://evil.com:" + port),
					"跨站 Origin（主机不同）必须 403");
		} finally {
			server.close();
			netty.close();
		}
	}

	/** 放行对照：同源浏览器请求与无 Origin 的本机客户端（curl/脚本）不受防线影响。 */
	@Test
	public void testSameOriginAndLocalClientsStillAllowed() throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		server.addHandler("/api/search", 1024, TransactionLevel.Serializable, DispatchMode.Normal,
				new SearchLogHandle());
		try {
			int port = ((InetSocketAddress)server.start(netty, "127.0.0.1", 0)
					.sync().channel().localAddress()).getPort();
			// 同源（Origin 与 Host 同 authority）：进入处理器既有路径（无 LogAgent → system error）。
			assertEquals(200, rawPostStatus(port, "127.0.0.1:" + port, "http://127.0.0.1:" + port),
					"同源浏览器请求必须放行");
			assertEquals(200, rawPostStatus(port, "localhost:" + port, "http://localhost:" + port),
					"localhost 形态的同源请求必须放行");
			// 无 Origin 的本机客户端（curl/脚本，存量使用方式）：回环 Host 放行。
			assertEquals(200, rawPostStatus(port, "127.0.0.1:" + port, null),
					"无 Origin 的本机请求必须放行");
		} finally {
			server.close();
			netty.close();
		}
	}

	/** isAllowed 纯判定矩阵：无 Origin 的本机客户端、同源浏览器、跨站 Origin、
	 * rebinding 同源-非回环 Host、非回环绑定形态（Token 门承担读取防线）。 */
	@Test
	public void testIsAllowedMatrix() {
		// 无 Origin（curl/脚本）：回环 Host 放行，非回环 Host/缺 Host 拒。
		assertTrue(BrowserOriginGuard.isAllowed(null, "127.0.0.1:9980", true));
		assertTrue(BrowserOriginGuard.isAllowed(null, "localhost:9980", true));
		assertTrue(BrowserOriginGuard.isAllowed(null, "[::1]:9980", true));
		assertTrue(BrowserOriginGuard.isAllowed(null, "127.0.0.5:9980", true), "127/8 全段回环");
		assertFalse(BrowserOriginGuard.isAllowed(null, "192.168.1.2:9980", true));
		assertFalse(BrowserOriginGuard.isAllowed(null, null, true), "缺 Host 的畸形请求拒");
		// Origin 与 Host 同 authority（大小写/括号归一）放行；不同源（scheme 内主机/端口）拒。
		assertTrue(BrowserOriginGuard.isAllowed("http://127.0.0.1:9980", "127.0.0.1:9980", true));
		assertTrue(BrowserOriginGuard.isAllowed("http://localhost:9980", "LOCALHOST:9980", true));
		assertFalse(BrowserOriginGuard.isAllowed("https://evil.com", "127.0.0.1:9980", true));
		assertFalse(BrowserOriginGuard.isAllowed("http://127.0.0.1:9981", "127.0.0.1:9980", true), "端口不同非同源");
		// rebinding：Origin 与 Host 同为攻击者域名（同源比对通过）但 Host 非回环——Host 防线拒。
		assertFalse(BrowserOriginGuard.isAllowed("http://attacker.com:9980", "attacker.com:9980", true));
		// 非回环绑定（checkDeployPolicy 必配 Token）：Host 防线让位 Token 门；跨站 Origin 仍拒。
		assertTrue(BrowserOriginGuard.isAllowed(null, "10.20.30.40:9980", false), "远程部署的本机/远端客户端放行");
		assertTrue(BrowserOriginGuard.isAllowed("http://10.20.30.40:9980", "10.20.30.40:9980", false));
		assertFalse(BrowserOriginGuard.isAllowed("https://evil.com", "10.20.30.40:9980", false));
	}

	/** 裸 socket 伪造 Host/Origin 头 POST，按头+Content-Length 读回应答状态码
	 * （服务器 keep-alive 不主动断连，不能整流读到 EOF）。 */
	private static int rawPostStatus(int port, String hostHeader, String originHeader) throws IOException {
		var request = "POST /api/search HTTP/1.1\r\n"
				+ "Host: " + hostHeader + "\r\n"
				+ (originHeader == null ? "" : "Origin: " + originHeader + "\r\n")
				+ "Content-Type: application/json\r\n"
				+ "Content-Length: 0\r\n"
				+ "Connection: close\r\n\r\n";
		try (var socket = new Socket("127.0.0.1", port)) {
			socket.setSoTimeout(10_000);
			socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
			var in = new java.io.BufferedInputStream(socket.getInputStream());
			var head = new StringBuilder();
			int b;
			while ((b = in.read()) != -1) {
				head.append((char)b);
				if (head.length() >= 4 && head.substring(head.length() - 4).equals("\r\n\r\n"))
					break;
			}
			var headers = head.toString();
			var statusLine = headers.split("\r\n", 2)[0];
			assertTrue(statusLine.startsWith("HTTP/1.1 "), "收到完整应答头: " + statusLine);
			var contentLength = 0;
			for (var line : headers.split("\r\n")) {
				if (line.toLowerCase().startsWith("content-length:"))
					contentLength = Integer.parseInt(line.substring(15).trim());
			}
			for (var i = 0; i < contentLength; i++) {
				if (in.read() == -1)
					break;
			}
			return Integer.parseInt(statusLine.split(" ")[1]);
		}
	}

	/** 复位静态防线状态到默认（回环设防），不污染同 JVM 其他用例。 */
	@AfterAll
	public static void resetGuard() {
		BrowserOriginGuard.configure(null);
	}
}
