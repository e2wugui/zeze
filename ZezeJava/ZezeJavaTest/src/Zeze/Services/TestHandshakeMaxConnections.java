package Zeze.Services;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import Zeze.Config;
import Zeze.Net.ServiceConf;
import Zeze.Net.TcpSocket;
import Zeze.Services.Handshake.Constant;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND-S3-2 回归：HandshakeServer/HandshakeBoth/TokenServer 覆写 OnSocketAccept 后，
 * Service.OnSocketAccept 默认实现里的 maxConnections 检查被丢掉，连接上限静默失效。
 * 修复后覆写点统一先调用 HandshakeBase.checkMaxConnections()，超限连接在 accept 时被关闭。
 * 自包含（临时目录+本机端口），标 @Fast。
 */
@Fast
public class TestHandshakeMaxConnections {
	// ServiceConf 的 maxConnections 无公开 setter，测试用反射设置。
	private static void setMaxConnections(ServiceConf conf, int value) throws Exception {
		var field = ServiceConf.class.getDeclaredField("maxConnections");
		field.setAccessible(true);
		field.setInt(conf, value);
	}

	private static int allocPort() throws Exception {
		try (var ss = new ServerSocket(0)) {
			return ss.getLocalPort();
		}
	}

	// 断言连接被服务端关闭：EOF(-1) 或 connection reset；
	// 若连接未被关闭（修复缺失时），read 会阻塞到 SO_TIMEOUT 抛 SocketTimeoutException 使测试失败。
	// SO_TIMEOUT 窗口需容忍慢 accept（30s）：全套件并行跑时全局 Selectors 可能仍是默认单线程
	// （L5 增加并行测试后实测负载下 accept 处理可远超 5s，曾报 expected:<2> but was:<1>）。
	private static void assertClosedByServer(Socket so) throws Exception {
		so.setSoTimeout(30_000);
		try {
			Assertions.assertEquals(-1, so.getInputStream().read(), "connection should be closed by server");
		} catch (SocketException e) {
			// connection reset 也是被服务端关闭的正常表现
		}
	}

	// 全局 Selectors 单例默认只有 1 个 selector 线程；JUnit classes 并行下其余测试的网络事件
	// 与本测试的 accept 排同一个队列，负载下 accept 延迟可超过等待窗（对齐
	// TestServiceManagerWithRaftCommitThenResponse.setUp 的做法补足到 CPU 数）。
	private static void ensureCpuSelectors() {
		int cpuCount = Runtime.getRuntime().availableProcessors();
		var selectors = Zeze.Net.Selectors.getInstance();
		if (selectors.getCount() < cpuCount)
			selectors.add(cpuCount - selectors.getCount());
	}

	// 等待窗 30s（原 50×100ms=5s 在并行负载下出现过 accept 未及处理，见 assertClosedByServer 注释）。
	private static void waitSocketCount(HandshakeBase server, int expected) throws InterruptedException {
		for (int i = 0; i < 150 && server.getSocketCount() < expected; ++i)
			Thread.sleep(200);
		Assertions.assertEquals(expected, server.getSocketCount());
	}

	private static int listenPort(HandshakeBase server) throws Exception {
		var listener = (TcpSocket)server.newServerSocket(new InetSocketAddress("127.0.0.1", 0), null);
		var local = listener.getLocalInet();
		Assertions.assertNotNull(local);
		return local.getPort();
	}

	@Test
	public void testHandshakeServerEnforcesMaxConnections() throws Exception {
		Task.tryInitThreadPool();
		ensureCpuSelectors();
		var conf = new Config();
		var sconf = new ServiceConf();
		sconf.getHandshakeOptions().setEncryptType(Constant.eEncryptTypeDisable);
		setMaxConnections(sconf, 2);
		conf.getServiceConfMap().put("TestHsMaxConnServer", sconf);

		var server = new HandshakeServer("TestHsMaxConnServer", conf);
		try {
			var port = listenPort(server);
			try (var s1 = new Socket("127.0.0.1", port)) {
				waitSocketCount(server, 1);
				try (var s2 = new Socket("127.0.0.1", port)) {
					waitSocketCount(server, 2);
					try (var s3 = new Socket("127.0.0.1", port)) {
						assertClosedByServer(s3); // 第 3 个连接必须被拒（修复前会被接受）
					}
				}
			}
			Assertions.assertEquals(2, server.getSocketCount());
		} finally {
			server.stop();
		}
	}

	@Test
	public void testHandshakeBothEnforcesMaxConnections() throws Exception {
		Task.tryInitThreadPool();
		ensureCpuSelectors();
		var conf = new Config();
		var sconf = new ServiceConf();
		sconf.getHandshakeOptions().setEncryptType(Constant.eEncryptTypeDisable);
		setMaxConnections(sconf, 1);
		conf.getServiceConfMap().put("TestHsMaxConnBoth", sconf);

		var server = new HandshakeBoth("TestHsMaxConnBoth", conf);
		try {
			var port = listenPort(server);
			try (var s1 = new Socket("127.0.0.1", port)) {
				waitSocketCount(server, 1);
				try (var s2 = new Socket("127.0.0.1", port)) {
					assertClosedByServer(s2); // 第 2 个连接必须被拒（修复前会被接受）
				}
			}
			Assertions.assertEquals(1, server.getSocketCount());
		} finally {
			server.stop();
		}
	}

	@Test
	public void testTokenServerEnforcesMaxConnections(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		ensureCpuSelectors();
		// Token 的 RocksDB 目录重定向到临时目录（参考 TestTokenKeepAlive）。
		System.setProperty("token.rocksdb", tempDir.resolve("token_db").toString());
		var conf = new Config();
		var sconf = new ServiceConf();
		setMaxConnections(sconf, 1);
		conf.getServiceConfMap().put("TokenServer", sconf);

		var port = allocPort();
		var tokenServer = new Token().start(conf, null, port);
		try {
			try (var s1 = new Socket("127.0.0.1", port)) {
				waitSocketCount(tokenServer.getService(), 1);
				try (var s2 = new Socket("127.0.0.1", port)) {
					assertClosedByServer(s2); // 第 2 个连接必须被拒（修复前会被接受）
				}
			}
			Assertions.assertEquals(1, tokenServer.getService().getSocketCount());
		} finally {
			tokenServer.stop();
			tokenServer.closeDb();
		}
	}
}
