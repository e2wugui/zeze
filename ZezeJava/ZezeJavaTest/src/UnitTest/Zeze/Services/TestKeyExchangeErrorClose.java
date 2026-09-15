package UnitTest.Zeze.Services;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAKey;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Services.Handshake.KeyExchange;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND6-33：KeyExchange异常路径不断连——TypeId不在handshakeProtocols中，走普通派发
 * 的callFuncCore仅回错误码、连接保持；恶意/损坏encIvKey抛GeneralSecurityException后
 * 可在单条连接上无限刷RSA私钥解密（CPU消耗），伪造clientPubKey路径更留下已切codec的
 * 半开连接。修复：processKeyExchangeRequest整体catch(Throwable)+close，与HandshakeBase
 * 「握手错误不能忽略」判例对齐。
 * 本地回环构造真实连接：正确md5+垃圾encIvKey触发解密异常，断言【服务端】socket被
 * 关闭（修复前：仅回错误码、服务端连接保持，永不关闭）。客户端必须注册KeyExchange
 * 工厂以解码错误应答——否则客户端自身因Unknown Protocol断连，污染断言对象
 * （首版测试的教训：断言客户端关闭时修复前后都绿）。
 */
@Fast
public class TestKeyExchangeErrorClose {

	/** 计数OnSocketClose的服务端Service。 */
	private static final class CloseCountService extends Service {
		final AtomicInteger closeCount = new AtomicInteger();

		CloseCountService(String name) {
			super(name);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			closeCount.incrementAndGet();
			super.OnSocketClose(so, e);
		}
	}

	private static int startServer(Service server) {
		var listen = (TcpSocket)server.newServerSocket("127.0.0.1", 0, null);
		var local = listen.getLocalInet();
		Assertions.assertNotNull(local, "listen socket local address");
		return local.getPort();
	}

	private static void await(String what, int timeoutMillis, java.util.function.BooleanSupplier cond)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (!cond.getAsBoolean()) {
			if (System.currentTimeMillis() > deadline)
				Assertions.fail("timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(1);
		}
	}

	@Test
	public void testGarbageEncIvKeyClosesServerSideConnection() throws Exception {
		Task.tryInitThreadPool();
		var keyPairGen = KeyPairGenerator.getInstance("RSA");
		keyPairGen.initialize(2048);
		var keyPair = keyPairGen.generateKeyPair();
		var serverPubKey = ((RSAKey)keyPair.getPublic()).getModulus().toByteArray();

		var server = new CloseCountService("TestKeyExchangeClose.Server");
		KeyExchange.addHandler(server, keyPair.getPrivate());
		int port = startServer(server);

		var client = new Service("TestKeyExchangeClose.Client");
		client.AddFactoryHandle(KeyExchange.TypeId, new Service.ProtocolFactoryHandle<>(KeyExchange::new,
				r -> Procedure.Success, TransactionLevel.None, DispatchMode.Direct));
		try {
			AsyncSocket socket = client.newClientSocket("127.0.0.1", port, null, null);
			Assertions.assertNotNull(socket, "客户端必须连上");
			await("server accepted", 10_000, () -> server.getSocketCount() >= 1);

			// 正确serverPubKeyMd5（跳过ErrorUnknownServerPubKey优雅路径）+垃圾encIvKey：
			// 服务端Cert.decryptRsa必然抛GeneralSecurityException（BadPadding）。
			var rpc = new KeyExchange(serverPubKey);
			rpc.Argument.encIvKey = new byte[]{1, 2, 3, 4, 5, 6, 7, 8}; // 非法RSA密文
			rpc.SendReturnVoid(client, socket, null);

			await("server OnSocketClose after decrypt failure (FND6-33)", 10_000,
					() -> server.closeCount.get() >= 1);
		} finally {
			client.stop();
			server.stop();
		}
	}
}
