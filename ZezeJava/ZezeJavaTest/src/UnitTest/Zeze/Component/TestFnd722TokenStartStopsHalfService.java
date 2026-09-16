package UnitTest.Zeze.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.ServiceConf;
import Zeze.Services.Token;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND7-22 回归：Token.start 半途失败时catch块只取消定时任务/置null/关rocksdb，
 * 不stop半启动的TokenServer——多acceptor配置下前一个bind成功、后一个失败时，
 * 已bind的监听socket与keepAlive定时器残留运行（引用已置null无人能停），
 * 同端口重试start永远bind冲突。
 * 修复后：catch中先service.stop()释放监听再清引用。
 * 两个acceptor的启动顺序按ServiceConf实际迭代序决定（forEachAcceptor与start用同一
 * ConcurrentHashMap.values()序），据此选定被占端口，保证"先成功后失败"确定性复现。
 */
@Fast
@ResourceLock("token.rocksdb") // Token经全局System property定位DB目录，与同族测试并行互相覆盖路径
public class TestFnd722TokenStartStopsHalfService {

	private static int freePort() throws Exception {
		try (var s = new ServerSocket()) {
			s.bind(new InetSocketAddress("127.0.0.1", 0));
			return s.getLocalPort();
		}
	}

	@Test
	public void testHalfStartedListenersReleasedOnStartFailure(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		System.setProperty("token.rocksdb", tempDir.resolve("token_db").toString());
		var token = new Token();
		int port1 = freePort();
		int port2 = freePort();
		try {
			var conf = new Config();
			var sconf = new ServiceConf();
			sconf.addAcceptor(new Acceptor(port1, "127.0.0.1"));
			sconf.addAcceptor(new Acceptor(port2, "127.0.0.1"));
			conf.getServiceConfMap().put("TokenServer", sconf);

			// 读出实际迭代顺序：排在前面的acceptor先Start，先bind成功；
			// 占用排在后面的端口，制造"前一个成功、后一个失败"的半启动场景。
			var iterPorts = new int[2];
			var idx = new int[1];
			sconf.forEachAcceptor2(a -> {
				iterPorts[idx[0]++] = a.getPort();
				return true;
			});
			int openPort = iterPorts[0];
			int blockedPort = iterPorts[1];

			var blocker = new ServerSocket();
			blocker.bind(new InetSocketAddress("127.0.0.1", blockedPort));
			try {
				Assertions.assertThrows(Exception.class, () -> token.start(conf, null, 0),
						"第二个acceptor端口被占，start必须失败");
			} finally {
				blocker.close();
			}
			Assertions.assertNull(token.getService(), "失败后service必须清空（FND4-69语义保持）");

			// FND7-22核心断言：半启动的监听必须已被stop释放——释放占用后两个端口都必须可重新bind。
			// 修复前：openPort被无人引用的TokenServer监听占用，此处BindException。
			try (var s1 = new ServerSocket(); var s2 = new ServerSocket()) {
				s1.bind(new InetSocketAddress("127.0.0.1", openPort));
				s2.bind(new InetSocketAddress("127.0.0.1", blockedPort));
			}

			// 半启动状态清干净后，二次start必须真实启动（在先前被残留占用的端口上监听）。
			token.start(null, "127.0.0.1", openPort);
			Assertions.assertNotNull(token.getService(), "复位后二次start真实启动");
			try (var sock = new Socket(InetAddress.getLoopbackAddress(), openPort)) {
				Assertions.assertTrue(sock.isConnected(), "端口可连接=真实监听");
			}
		} finally {
			try {
				token.stop();
			} catch (Exception ignored) {
			}
			token.closeDb(); // Windows下释放rocksdb文件锁，TempDir才能清理
			System.clearProperty("token.rocksdb");
		}
	}
}
