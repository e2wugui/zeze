package UnitTest.Zeze.Services;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAKey;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
 * FND8-47回归：KeyExchange对非空clientPubKey从不校验（ErrorUnknownClientPubKey死码），
 * 任何自造公钥都能以"认证客户端"身份建立会话——文档声称的双向认证实为无认证。
 * 修复后：addHandler重载注册可信公钥acceptor，校验在任何状态变更（含codec切换）之前，
 * 失败回ErrorUnknownClientPubKey并断连（clientPubKey为空同样拒绝）；旧签名保持无认证
 * 语义（兼容）；三处误导性文档（协议注释/字段文档/错误码注释）如实化。
 */
@Fast
public class TestFnd847KeyExchangeClientAuth {

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

	private static KeyPair genKeyPair() throws Exception {
		var gen = KeyPairGenerator.getInstance("RSA");
		gen.initialize(2048);
		return gen.generateKeyPair();
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
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "timeout waiting: " + what);
			//noinspection BusyWait
			Thread.sleep(5);
		}
	}

	// 发起KeyExchange并捕获应答resultCode（不装codec，仅观察握手结果）
	private static long sendAndCaptureCode(AsyncSocket socket, KeyExchange rpc) throws Exception {
		var latch = new CountDownLatch(1);
		var code = new AtomicReference<Long>();
		Assertions.assertTrue(rpc.Send(socket, r -> {
			code.set(r.getResultCode());
			latch.countDown();
			return 0;
		}), "KeyExchange.Send必须成功提交");
		Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "timeout waiting KeyExchange response");
		return code.get();
	}

	// 自造（不在白名单内）公钥：认证模式下必须收到ErrorUnknownClientPubKey且服务端断连
	@Test
	public void testUnknownClientPubKeyRejectedAndClosed() throws Exception {
		Task.tryInitThreadPool();
		var serverKeys = genKeyPair();
		var clientKeys = genKeyPair(); // 攻击者自持密钥对
		var serverPubKey = ((RSAKey)serverKeys.getPublic()).getModulus().toByteArray();

		var server = new CloseCountService("TestFnd847.Server1");
		var knownMd5 = KeyExchange.getPubKeyMd5(((RSAKey)genKeyPair().getPublic()).getModulus().toByteArray());
		KeyExchange.addHandler(server, serverKeys.getPrivate(),
				pubKey -> Arrays.equals(KeyExchange.getPubKeyMd5(pubKey), knownMd5));
		int port = startServer(server);

		var client = new Service("TestFnd847.Client1");
		client.AddFactoryHandle(KeyExchange.TypeId, new Service.ProtocolFactoryHandle<>(KeyExchange::new,
				r -> Procedure.Success, TransactionLevel.None, DispatchMode.Direct));
		try {
			var socket = client.newClientSocket("127.0.0.1", port, null, null);
			Assertions.assertNotNull(socket);
			await("server accepted", 10_000, () -> server.getSocketCount() >= 1);

			var attackerPubKey = ((RSAKey)clientKeys.getPublic()).getModulus().toByteArray();
			var code = sendAndCaptureCode(socket, new KeyExchange(serverPubKey, attackerPubKey));
			Assertions.assertEquals(KeyExchange.Res.ErrorUnknownClientPubKey, code,
					"未知客户端公钥必须被拒（修复前照常建会话，认证形同虚设）");
			await("server closed after unknown client pub key", 10_000, () -> server.closeCount.get() >= 1);
		} finally {
			client.stop();
			server.stop();
		}
	}

	// 认证模式下clientPubKey为空（未提供身份）同样拒绝
	@Test
	public void testEmptyClientPubKeyRejectedWhenAuthEnabled() throws Exception {
		Task.tryInitThreadPool();
		var serverKeys = genKeyPair();
		var serverPubKey = ((RSAKey)serverKeys.getPublic()).getModulus().toByteArray();

		var server = new CloseCountService("TestFnd847.Server2");
		KeyExchange.addHandler(server, serverKeys.getPrivate(), pubKey -> true); // 全放行的acceptor
		int port = startServer(server);

		var client = new Service("TestFnd847.Client2");
		client.AddFactoryHandle(KeyExchange.TypeId, new Service.ProtocolFactoryHandle<>(KeyExchange::new,
				r -> Procedure.Success, TransactionLevel.None, DispatchMode.Direct));
		try {
			var socket = client.newClientSocket("127.0.0.1", port, null, null);
			Assertions.assertNotNull(socket);
			await("server accepted", 10_000, () -> server.getSocketCount() >= 1);

			var code = sendAndCaptureCode(socket, new KeyExchange(serverPubKey)); // clientPubKey为空
			Assertions.assertEquals(KeyExchange.Res.ErrorUnknownClientPubKey, code,
					"认证模式下空clientPubKey（未提供身份）必须被拒");
			await("server closed after empty client pub key", 10_000, () -> server.closeCount.get() >= 1);
		} finally {
			client.stop();
			server.stop();
		}
	}

	// 白名单公钥（带前导0的等价编码，验证归一化）通过认证，握手正常完成且连接保持
	@Test
	public void testKnownClientPubKeyNormalizedAndAccepted() throws Exception {
		Task.tryInitThreadPool();
		var serverKeys = genKeyPair();
		var clientKeys = genKeyPair();
		var serverPubKey = ((RSAKey)serverKeys.getPublic()).getModulus().toByteArray();

		var server = new CloseCountService("TestFnd847.Server3");
		var knownN = ((RSAKey)clientKeys.getPublic()).getModulus().toByteArray(); // 可能带符号位前导0
		var knownMd5 = KeyExchange.getPubKeyMd5(knownN); // 经前导零归一后的指纹
		KeyExchange.addHandler(server, serverKeys.getPrivate(),
				pubKey -> Arrays.equals(KeyExchange.getPubKeyMd5(pubKey), knownMd5));
		int port = startServer(server);

		var client = new Service("TestFnd847.Client3");
		client.AddFactoryHandle(KeyExchange.TypeId, new Service.ProtocolFactoryHandle<>(KeyExchange::new,
				r -> Procedure.Success, TransactionLevel.None, DispatchMode.Direct));
		try {
			var socket = client.newClientSocket("127.0.0.1", port, null, null);
			Assertions.assertNotNull(socket);
			await("server accepted", 10_000, () -> server.getSocketCount() >= 1);

			// 客户端公钥带符号位前导0（BigInteger.toByteArray等价编码）：归一化后必须匹配
			var pubKeyWithLeadingZero = new byte[knownN.length + 1];
			pubKeyWithLeadingZero[0] = 0;
			System.arraycopy(knownN, 0, pubKeyWithLeadingZero, 1, knownN.length);
			var code = sendAndCaptureCode(socket, new KeyExchange(serverPubKey, pubKeyWithLeadingZero));
			Assertions.assertEquals(0, code, "白名单公钥（前导0等价编码）必须通过认证并完成握手");
			//noinspection BusyWait
			Thread.sleep(500); // 观察窗：认证通过后不得误断连（拒绝路径的断连是立即的）
			Assertions.assertEquals(0, server.closeCount.get(), "认证通过后服务端不得断连");
		} finally {
			client.stop();
			server.stop();
		}
	}

	// 兼容：旧签名（无acceptor）保持无认证语义——自造公钥照常建会话（ resultCode==0 ）
	@Test
	public void testLegacyAddHandlerKeepsNoAuthSemantics() throws Exception {
		Task.tryInitThreadPool();
		var serverKeys = genKeyPair();
		var attackerKeys = genKeyPair();
		var serverPubKey = ((RSAKey)serverKeys.getPublic()).getModulus().toByteArray();

		var server = new CloseCountService("TestFnd847.Server4");
		KeyExchange.addHandler(server, serverKeys.getPrivate()); // 旧签名
		int port = startServer(server);

		var client = new Service("TestFnd847.Client4");
		client.AddFactoryHandle(KeyExchange.TypeId, new Service.ProtocolFactoryHandle<>(KeyExchange::new,
				r -> Procedure.Success, TransactionLevel.None, DispatchMode.Direct));
		try {
			var socket = client.newClientSocket("127.0.0.1", port, null, null);
			Assertions.assertNotNull(socket);
			await("server accepted", 10_000, () -> server.getSocketCount() >= 1);

			var attackerPubKey = ((RSAKey)attackerKeys.getPublic()).getModulus().toByteArray();
			var code = sendAndCaptureCode(socket, new KeyExchange(serverPubKey, attackerPubKey));
			Assertions.assertEquals(0, code, "旧签名（未启用认证）保持无认证语义，不破坏既有部署");
		} finally {
			client.stop();
			server.stop();
		}
	}
}
