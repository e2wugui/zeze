package UnitTest.Zeze.Netty;

import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-39 回归：HttpServer.start先注册scheduler后bind。
 * bind同步失败（host不可解析，SocketUtils.createSocketAddress同步抛
 * UnknownHostException）时scheduler已非null残留——再次start抛
 * IllegalStateException("already started")（服务器无法重试启动），
 * 且每5秒的checkTimeout任务对空channels永久空转。修复：scheduler注册
 * 移到bind之后，bind同步失败不产生半启动态。
 * host用语法非法串（含空格）保证全平台确定性同步失败。
 */
@Fast
public class TestHttpServerStartRollback {
	@Test
	public void testBindSyncFailNotHalfStarted() throws Exception {
		try (var netty = new Netty(); var http = new HttpServer(null)) {
			// 非法端口：AbstractBootstrap.bind(int)构造new InetSocketAddress(65536)时
			// 同步抛IllegalArgumentException（不依赖host解析——本Netty版本的
			// bind(String,int)不做同步DNS解析）。
			Assertions.assertThrows(Exception.class, () -> http.start(netty, null, 65536),
					"非法端口必须同步失败");

			// 半启动态判定：第二次start必须重新到达bind（仍是bind的异常），
			// 而非scheduler残留导致的already started。
			var ex = Assertions.assertThrows(Exception.class, () -> http.start(netty, null, 65536));
			Assertions.assertNotEquals("already started", ex.getMessage(),
					"bind同步失败不得进入already started半启动态（FND4-39）");
		}
	}
}
