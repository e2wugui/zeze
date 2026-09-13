package UnitTest.Zeze.Component;

import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Services.Token;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND4-69：Token.start 半途失败（端口占用等）时service字段残留，重入检查把
 * "未运行的服务"当已启动直接返回——二次start假成功，服务永不监听、清理任务未注册。
 * 修复为全有或全无：失败清空已建状态，二次start真实重启。
 */
@Fast
@ResourceLock("token.rocksdb") // Token经全局System property定位DB目录，与同族测试并行互相覆盖路径
public class TestTokenStartRollback {

	@Test
	public void testStartFailureThenRestart(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		System.setProperty("token.rocksdb", tempDir.resolve("token_db").toString());

		int port;
		var blocker = new ServerSocket(0);
		try {
			port = blocker.getLocalPort();
		} finally {
			blocker.close();
		}
		{
			var token = new Token();

			// 第一次start：端口被占，service.start()抛异常
			var blocker2 = new ServerSocket(port);
			try {
				Assertions.assertThrows(Exception.class, () -> token.start(null, "127.0.0.1", port),
						"端口占用下首次start应抛异常");
			} finally {
				blocker2.close();
			}

			// 修复前：service残留非null（假启动态）——反射验证半途状态被清空
			var serviceField = Token.class.getDeclaredField("service");
			serviceField.setAccessible(true);
			Assertions.assertNull(serviceField.get(token), "失败后service必须清空，否则二次start假成功");

			// 端口释放后二次start：必须真实启动（监听+清理任务注册）
			var tokenRef = token;
			token.start(null, "127.0.0.1", port);
			Assertions.assertNotNull(serviceField.get(tokenRef), "二次start真实启动");

			var futureField = Token.class.getDeclaredField("cleanTokenMapFuture");
			futureField.setAccessible(true);
			Assertions.assertNotNull(((Future<?>)futureField.get(tokenRef)), "清理任务必须已注册（修复前为null）");

			// 端口可连接=真实监听
			try (var sock = new java.net.Socket("127.0.0.1", port)) {
				Assertions.assertTrue(sock.isConnected());
			} finally {
				token.stop();
				token.closeDb(); // Windows下释放rocksdb文件锁，TempDir才能清理
			}
		}
		System.clearProperty("token.rocksdb");
	}
}
