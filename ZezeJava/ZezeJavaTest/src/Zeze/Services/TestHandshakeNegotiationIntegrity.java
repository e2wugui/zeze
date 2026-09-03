package Zeze.Services;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Net.ServiceConf;
import Zeze.Net.TcpSocket;
import Zeze.Services.Handshake.CHandshake;
import Zeze.Services.Handshake.CHandshakeDone;
import Zeze.Services.Handshake.Constant;
import Zeze.Services.Handshake.Helper;
import Zeze.Services.Handshake.SHandshake;
import Zeze.Services.Handshake.SHandshake0;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND-S3-1 回归（协商一致性部分）：Handshake 加密类型协商无完整性校验。
 * 修复落地两处无争议的一致性校验（完整防主动MITM降级需协商完整性保护，属协议升级，见 l4/FND-S3-1.md）：
 * 1. 服务器拒绝 CHandshake 上报的 encryptType 与自身配置（即SHandshake0推荐值）不一致；
 * 2. 客户端拒绝 SHandshake 回显的 encryptType 与自己请求的不一致。
 * 自包含（本机端口、无外部依赖），标 @Fast。
 */
@Fast
public class TestHandshakeNegotiationIntegrity {
	private static String chainMessage(@Nullable Throwable e) {
		var sb = new StringBuilder();
		for (var t = e; t != null; t = t.getCause()) {
			sb.append(t.getMessage()).append(';');
			if (t.getCause() == t)
				break;
		}
		return sb.toString();
	}

	private static int listenPort(Service service) throws Exception {
		var listener = (TcpSocket)service.newServerSocket(new InetSocketAddress("127.0.0.1", 0), null);
		var local = listener.getLocalInet();
		Assertions.assertNotNull(local);
		return local.getPort();
	}

	/**
	 * 用例1：模拟 finding 主路径——MITM 把 SHandshake0 的推荐从 RsaAes(3) 篡改为 AesNoSecureIp(2)，
	 * 被误导的客户端发来的 CHandshake{encryptType=2} 到达配置为 3 的服务器。
	 * 修复后服务器必须拒绝（关闭连接）；修复前会照常按 case 2 匿名DH处理（降级成功）。
	 */
	@Test
	public void testServerRejectsEncryptTypeDowngrade() throws Exception {
		Task.tryInitThreadPool();
		var conf = new Config();
		var sconf = new ServiceConf();
		sconf.getHandshakeOptions().setEncryptType(Constant.eEncryptTypeRsaAes);
		conf.getServiceConfMap().put("TestHsDowngradeServer", sconf);

		var server = new HandshakeServer("TestHsDowngradeServer", conf);
		try {
			var port = listenPort(server);

			// 攻击者视角：吞掉服务器的SHandshake0/SHandshake，直接发"被篡改推荐后客户端会发出的"CHandshake{2}
			final var closed = new TaskCompletionSource<Throwable>();
			var attacker = new Service("TestHsDowngradeAttacker", new Config()) {
				{
					AddFactoryHandle(SHandshake0.TypeId_, new ProtocolFactoryHandle<>(SHandshake0::new,
							p -> 0L, TransactionLevel.None, DispatchMode.Direct));
					AddFactoryHandle(SHandshake.TypeId_, new ProtocolFactoryHandle<>(SHandshake::new,
							p -> 0L, TransactionLevel.None, DispatchMode.Direct));
				}

				@Override
				public void OnSocketConnected(@NotNull AsyncSocket so) throws Exception {
					super.OnSocketConnected(so);
					var p = new CHandshake();
					p.Argument.encryptType = Constant.eEncryptTypeAesNoSecureIp; // 被降级的类型2
					p.Argument.encryptParam = Helper.generateDHResponse(1, Helper.makeDHRandom()).toByteArray();
					p.Send(so);
				}

				@Override
				public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
					super.OnSocketClose(so, e);
					closed.setResult(e);
				}
			};
			try {
				attacker.newClientSocket("127.0.0.1", port, null, null);
				var cause = closed.get(5, TimeUnit.SECONDS); // 修复前连接静置不会被关，5秒超时使测试失败
				Assertions.assertNotNull(cause);
				// 服务端的 encryptType mismatch ISE 只留在服务端日志/OnSocketClose，不会随线路
				// 传播——客户端可观测的拒绝行为就是连接被关闭（cause=inputClosed/EOF 等）。
				for (int i = 0; i < 50 && server.getSocketCount() > 0; ++i)
					Thread.sleep(100);
				Assertions.assertEquals(0, server.getSocketCount());
			} finally {
				attacker.stop();
			}
		} finally {
			server.stop();
		}
	}

	/**
	 * 用例2：模拟 SHandshake 回显被篡改——客户端按推荐2发出请求，服务器回显类型1。
	 * 修复后客户端必须在装配codec之前拒绝并关闭连接；修复前会按回显类型1静默完成握手。
	 */
	@Test
	public void testClientRejectsEchoTypeMismatch() throws Exception {
		Task.tryInitThreadPool();
		// 假服务器：推荐2，收到CHandshake后回显不一致的1
		var fakeServer = new Service("TestHsEchoTamperServer", new Config()) {
			{
				AddFactoryHandle(CHandshake.TypeId_, new ProtocolFactoryHandle<>(CHandshake::new,
						this::onCHandshake, TransactionLevel.None, DispatchMode.Direct));
				AddFactoryHandle(CHandshakeDone.TypeId_, new ProtocolFactoryHandle<>(CHandshakeDone::new,
						p -> 0L, TransactionLevel.None, DispatchMode.Direct));
			}

			@Override
			public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
				super.OnSocketAccept(so);
				var hand0 = new SHandshake0();
				hand0.Argument.encryptType = Constant.eEncryptTypeAesNoSecureIp; // 模拟被篡改的推荐位
				hand0.Send(so);
			}

			private long onCHandshake(@NotNull CHandshake p) throws Exception {
				var sh = new SHandshake();
				sh.Argument.encryptType = Constant.eEncryptTypeAes; // 回显与请求不一致（模拟篡改）
				sh.Argument.encryptParam = Helper.generateDHResponse(1, Helper.makeDHRandom()).toByteArray();
				sh.Send(p.getSender());
				return 0L;
			}
		};
		try {
			var port = listenPort(fakeServer);

			final var closed = new TaskCompletionSource<Throwable>();
			var victim = new HandshakeClient("TestHsEchoVictimClient", new Config()) {
				@Override
				public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
					super.OnSocketClose(so, e);
					closed.setResult(e);
				}
			};
			try {
				victim.newClientSocket("127.0.0.1", port, null, null);
				var cause = closed.get(5, TimeUnit.SECONDS);
				Assertions.assertNotNull(cause);
				Assertions.assertTrue(chainMessage(cause).contains("encryptType mismatch"),
						() -> "expect encryptType mismatch but: " + chainMessage(cause));
			} finally {
				victim.stop();
			}
		} finally {
			fakeServer.stop();
		}
	}
}
