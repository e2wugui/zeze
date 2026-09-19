package Zeze.Services;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.TcpSocket;
import Zeze.Services.Handshake.Constant;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 复审R2回归（FND7-S2②）：processSHandshake0对服务端"全Disable推荐"无条件走明文快速
 * 分支（CHandshakeDone+OnHandshakeDone），客户端自身的加密诉求（HandshakeOptions.
 * EncryptType!=Disable）无客户端侧校验——配置了加密的客户端连上Disable服务端即静默
 * 降级为明文会话，"客户端要求加密"这一配置永不生效（服务端侧已有镜像校验FND-S3-1：
 * 拒绝encryptType不一致的CHandshake）。
 * 修复后：客户端镜像校验——全Disable推荐且自身配置加密时拒绝并断连（连接异常关闭，
 * 不回调OnHandshakeDone）。
 * 自包含（本机随机端口），标 @Fast。
 */
@Fast
public class TestFnd7R2HandshakeClientRefusesPlaintextDowngrade {
	private static String chainMessage(@Nullable Throwable e) {
		var sb = new StringBuilder();
		for (var t = e; t != null; t = t.getCause()) {
			sb.append(t.getMessage()).append(';');
			if (t.getCause() == t)
				break;
		}
		return sb.toString();
	}

	@Test
	public void testClientRefusesDisableRecommendationWhenEncryptionConfigured() throws Exception {
		Task.tryInitThreadPool();
		// Disable服务端：真实HandshakeServer，SHandshake0推荐全Disable
		var conf = new Config();
		var server = new HandshakeServer("TestHsR2PlainServer", conf);
		try {
			int port;
			try (var s = new java.net.ServerSocket()) {
				s.bind(new InetSocketAddress("127.0.0.1", 0));
				port = s.getLocalPort();
			}
			var listener = (TcpSocket)server.newServerSocket(new InetSocketAddress("127.0.0.1", port), null);
			try {
				final var closed = new TaskCompletionSource<Throwable>();
				final var handshakeDone = new java.util.concurrent.atomic.AtomicBoolean();
				// 客户端配置了加密诉求：EncryptType=AesNoSecureIp
				var clientConf = new Config();
				var csconf = new Zeze.Net.ServiceConf();
				csconf.getHandshakeOptions().setEncryptType(Constant.eEncryptTypeAesNoSecureIp);
				clientConf.getServiceConfMap().put("TestHsR2VictimClient", csconf);
				var victim = new HandshakeClient("TestHsR2VictimClient", clientConf) {
					@Override
					public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
						super.OnHandshakeDone(so);
						handshakeDone.set(true);
					}

					@Override
					public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
						super.OnSocketClose(so, e);
						closed.setResult(e);
					}
				};
				try {
					victim.newClientSocket("127.0.0.1", port, null, null);
					var cause = closed.get(15, TimeUnit.SECONDS); // 修复前：明文会话静默建立，连接不关，超时 → 红（预算15s：OnSocketClose派发在满负载下可滞后5s+，3x范式）
					Assertions.assertNotNull(cause);
					Assertions.assertTrue(chainMessage(cause).contains("downgrade"),
							() -> "expect plaintext downgrade refusal but: " + chainMessage(cause));
					// 拒绝路径不得回调OnHandshakeDone
					Assertions.assertFalse(handshakeDone.get(), "拒绝明文降级时不得回调OnHandshakeDone");
				} finally {
					victim.stop();
				}
			} finally {
				listener.close();
			}
		} finally {
			server.stop();
		}
	}

	/**
	 * R3收窄（S2②旁路）：仅压缩推荐（服务端EncryptType=Disable+compressS2c启用）时
	 * processSHandshake0走startHandshake分支——原检查只挂在全Disable的else分支上，
	 * 客户端配置的加密诉求被无声降级为"明文+压缩"会话。修复：检查提到分支之前，
	 * 只要推荐的encryptType=Disable且客户端配置了加密就拒绝断连。
	 * 修复前红：明文+压缩会话静默建立，连接不关，5秒超时。
	 */
	@Test
	public void testClientRefusesCompressOnlyRecommendationWhenEncryptionConfigured() throws Exception {
		Task.tryInitThreadPool();
		// 服务端：EncryptType=Disable但推荐压缩——SHandshake0为encryptType=Disable+compressS2c=Mppc
		var serverConf = new Config();
		var ssconf = new Zeze.Net.ServiceConf();
		ssconf.getHandshakeOptions().setEncryptType(Constant.eEncryptTypeDisable);
		ssconf.getHandshakeOptions().setCompressS2c(Constant.eCompressTypeMppc);
		serverConf.getServiceConfMap().put("TestHsR3CompServer", ssconf);
		var server = new HandshakeServer("TestHsR3CompServer", serverConf);
		try {
			int port;
			try (var s = new java.net.ServerSocket()) {
				s.bind(new InetSocketAddress("127.0.0.1", 0));
				port = s.getLocalPort();
			}
			var listener = (TcpSocket)server.newServerSocket(new InetSocketAddress("127.0.0.1", port), null);
			try {
				final var closed = new TaskCompletionSource<Throwable>();
				final var handshakeDone = new java.util.concurrent.atomic.AtomicBoolean();
				// 客户端配置了加密诉求：EncryptType=AesNoSecureIp
				var clientConf = new Config();
				var csconf = new Zeze.Net.ServiceConf();
				csconf.getHandshakeOptions().setEncryptType(Constant.eEncryptTypeAesNoSecureIp);
				clientConf.getServiceConfMap().put("TestHsR3VictimClient", csconf);
				var victim = new HandshakeClient("TestHsR3VictimClient", clientConf) {
					@Override
					public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
						super.OnHandshakeDone(so);
						handshakeDone.set(true);
					}

					@Override
					public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
						super.OnSocketClose(so, e);
						closed.setResult(e);
					}
				};
				try {
					victim.newClientSocket("127.0.0.1", port, null, null);
					var cause = closed.get(15, TimeUnit.SECONDS); // 修复前：明文+压缩会话静默建立，连接不关，超时 → 红（预算15s同上，3x）
					Assertions.assertNotNull(cause);
					Assertions.assertTrue(chainMessage(cause).contains("downgrade"),
							() -> "expect plaintext(compress-only) downgrade refusal but: " + chainMessage(cause));
					Assertions.assertFalse(handshakeDone.get(), "拒绝降级时不得回调OnHandshakeDone");
				} finally {
					victim.stop();
				}
			} finally {
				listener.close();
			}
		} finally {
			server.stop();
		}
	}
}
