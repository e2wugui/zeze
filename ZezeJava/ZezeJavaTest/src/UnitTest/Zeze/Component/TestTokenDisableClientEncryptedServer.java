package UnitTest.Zeze.Component;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Services.Handshake.Constant;
import Zeze.Services.Token;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * S4-F1 回归：TokenClient 全Disable快速路径按本端单边配置在连接建立即回调OnHandshakeDone
 * （重放SubTopic明文应用协议），被配置加密的TokenServer解码准入门禁确定性拒绝断连。
 * 修复为双边语义：客户端推迟到对端SHandshake0驱动的握手完成（服务端统一发SHandshake0），
 * waitReady亦推迟到握手完成后——Disable客户端可以正常对接加密服务端。
 * 附：S4-F3 契约验证（stop等待清理任务结束）——演进后等待语义内聚为DaemonTimer.stop
 * （站级白盒运行标志已删，组件级钉板见TestDaemonTimer.testStopWaitsInFlightAndRestart），
 * 本类保留站级关门与幂等断言。
 */
@Fast
@ResourceLock("token.rocksdb") // Token经全局System property定位DB目录，与同族测试并行互相覆盖路径
public class TestTokenDisableClientEncryptedServer {
	private static final int PORT = 5013;

	@Test
	public void testDisableClientHandshakesWithEncryptedServer(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		System.setProperty("token.rocksdb", tempDir.resolve("token_db").toString());
		var conf = new Config().loadAndParse();
		// 无具名ServiceConf条目时Service使用defaultServiceConf的HandshakeOptions（见Service构造）
		conf.getDefaultServiceConf().getHandshakeOptions().setEncryptType(Constant.eEncryptTypeAesNoSecureIp);
		var tokenServer = new Token().start(conf, null, PORT);
		try {
			var tokenClient = new Token.TokenClient(null).start("127.0.0.1", PORT); // 客户端保持全Disable
			try {
				// 修复前：waitReady立即返回（快速路径），newToken明文发出即被服务端门禁拒绝断连，
				// get()超时/失败；修复后：握手先行完成，RPC走加密通路。
				tokenClient.waitReady();
				var token = tokenClient.newToken(new Binary("abc"), 5000).get().getToken();
				Assertions.assertEquals(24, token.length());

				var res = tokenClient.getToken(token, 1).get();
				Assertions.assertEquals("abc", res.getContext().toString(StandardCharsets.UTF_8));
				Assertions.assertEquals(1, res.getCount());
			} finally {
				tokenClient.stop();
			}
		} finally {
			tokenServer.stop();
			tokenServer.closeDb();
		}
	}

	/** S4-F3演进：stop()后两个守护（cleanTokenMap/cleanTokenMapTable）必须关门；
	 * rocksdb已关时再次stop不挂死不抛（幂等）。 */
	@Test
	public void testStopShutsDownCleanTokenMapDaemons(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		System.setProperty("token.rocksdb", tempDir.resolve("token_db").toString());
		var tokenServer = new Token().start(null, null, PORT);
		try {
			var mapDaemon = (Zeze.Util.DaemonTimer)getAccessible(tokenServer, "cleanTokenMapDaemon");
			var tableDaemon = (Zeze.Util.DaemonTimer)getAccessible(tokenServer, "cleanTokenMapTableDaemon");
			Assertions.assertFalse(mapDaemon.isShutdown(), "start后cleanTokenMap守护必须运行");
			Assertions.assertFalse(tableDaemon.isShutdown(), "start后cleanTokenMapTable守护必须运行");
			tokenServer.stop();
			Assertions.assertTrue(mapDaemon.isShutdown(), "stop后cleanTokenMap守护必须关门");
			Assertions.assertTrue(tableDaemon.isShutdown(), "stop后cleanTokenMapTable守护必须关门");
			// 幂等：rocksdb已关，再次stop不挂死不抛
			tokenServer.stop();
		} finally {
			tokenServer.stop();
			tokenServer.closeDb();
		}
	}

	private static Object getAccessible(Token tokenServer, String fieldName) throws Exception {
		var field = Token.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(tokenServer);
	}
}
