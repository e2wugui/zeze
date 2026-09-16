package Zeze.Services;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import Zeze.Config;
import Zeze.Net.ServiceConf;
import Zeze.Net.TcpSocket;
import Zeze.Services.Handshake.Constant;
import Zeze.Services.Token.TokenClient;
import Zeze.Util.Task;
import harness.Fast;

/**
 * 复审R2回归（FND7-S2①）：TokenServer覆写OnSocketAccept不发送SHandshake0即直呼
 * OnHandshakeDone，TokenClient覆写OnSocketConnected同样立即回调——握手机制虽完整装配
 * （TokenServer extends HandshakeServer、TokenClient extends HandshakeClient）却从未被
 * 发起，EncryptType/Compress配置永远不生效：配置加密的Token服务此前静默全明文运行；
 * FND7-23输入门禁落地后更是直接拒绝所有未握手应用协议，服务完全不可用。
 * 修复后：TokenServer配置了加密或压缩时对齐HandshakeServer发送SHandshake0发起握手，
 * OnHandshakeDone推迟到CHandshakeDone（codec装配完成）后；TokenClient按自身配置对称
 * 推迟。全Disable保持原快速路径（零额外往返）。
 * 验证：AesNoSecureIp配置下握手真实完成（两侧codec装配、连接isSecurity），加密NewToken
 * 往返成功。修复前红：握手永不发生，isSecurity恒false。
 * 自包含（本机随机端口+独立DB目录），标 @Fast。
 */
@Fast
@ResourceLock("token.rocksdb") // Token经全局System property定位DB目录，与同族测试并行互相覆盖路径
public class TestFnd7R2TokenHandshakeInitiation {
	private static boolean waitUntil(java.util.function.BooleanSupplier cond, long timeoutMs) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (cond.getAsBoolean())
				return true;
			//noinspection BusyWait
			Thread.sleep(20);
		}
		return cond.getAsBoolean();
	}

	@Test
	@Timeout(90)
	public void testConfiguredEncryptionActuallyHandshakes(@TempDir java.nio.file.Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		System.setProperty("token.rocksdb", tempDir.resolve("token_db").toString());

		var conf = new Config();
		var sconf = new ServiceConf();
		sconf.getHandshakeOptions().setEncryptType(Constant.eEncryptTypeAesNoSecureIp);
		conf.getServiceConfMap().put("TokenServer", sconf);

		var token = new Zeze.Services.Token();
		var client = new TokenClient(new Config());
		// 两侧对称配置（部署契约）：客户端同样按自身配置推迟OnHandshakeDone到握手完成
		client.getConfig().getHandshakeOptions().setEncryptType(Constant.eEncryptTypeAesNoSecureIp);
		try {
			int port;
			try (var s = new java.net.ServerSocket()) {
				s.bind(new java.net.InetSocketAddress("127.0.0.1", 0));
				port = s.getLocalPort();
			}
			token.start(conf, "127.0.0.1", port);
			client.start("127.0.0.1", port);

			var socket = client.getSocket();
			Assertions.assertNotNull(socket, "客户端必须建立连接");
			Assertions.assertTrue(waitUntil(() -> socket instanceof TcpSocket tcp && tcp.isSecurity(), 10_000),
					"配置了EncryptType=AesNoSecureIp的Token连接必须完成握手装配codec"
							+ "（修复前：握手永不发起，isSecurity恒false）");

			var future = client.newToken(null, 60_000);
			var result = future.get(10, TimeUnit.SECONDS); // 加密NewToken往返
			Assertions.assertNotNull(result);
		} finally {
			try {
				client.stop();
			} catch (Exception ignored) {
			}
			try {
				token.stop();
				token.closeDb(); // Windows下释放rocksdb文件锁，TempDir才能清理
			} catch (Exception ignored) {
			}
		}
	}
}
