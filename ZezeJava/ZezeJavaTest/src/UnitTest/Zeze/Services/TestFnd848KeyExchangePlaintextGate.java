package UnitTest.Zeze.Services;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAKey;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Handshake.KeyExchange;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-48回归：KeyExchange.addHandler挂在普通Service上无任何明文门禁——安全codec经
 * submitAction延迟生效，同批输入在生效前被明文解码派发（MITM注入窗口）；恶意客户端
 * 干脆不握手、全程明文同样无拒绝。修复后：addHandler装配Service级逐帧门禁（判据
 * TcpSocket.isSecurity()，豁免KeyExchange自身），未完成密钥交换的连接收到任何其他
 * 明文帧立即断连；正常完成握手后加密流量不受影响。
 */
@Fast
public class TestFnd848KeyExchangePlaintextGate {

	/** 服务端已注册handle的普通应用协议（修复前：不握手也照常派发执行）。 */
	public static class PlainProtocol extends Protocol<EmptyBean> {
		public static final int ModuleId = 92;
		public static final int ProtocolId = 48;

		public PlainProtocol() {
			Argument = new EmptyBean();
		}

		@Override
		public int getModuleId() {
			return ModuleId;
		}

		@Override
		public int getProtocolId() {
			return ProtocolId;
		}
	}

	private static final class CloseCountService extends Service {
		final AtomicInteger closeCount = new AtomicInteger();
		final AtomicInteger plainDispatched = new AtomicInteger();

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

	// 不握手直发明文应用协议（服务端已注册handle）：门禁必须断连且协议不得被派发
	@Test
	public void testPlaintextWithoutHandshakeRejected() throws Exception {
		Task.tryInitThreadPool();
		var serverKeys = genKeyPair();
		var server = new CloseCountService("TestFnd848.Server1");
		KeyExchange.addHandler(server, serverKeys.getPrivate());
		server.AddFactoryHandle(Protocol.makeTypeId(PlainProtocol.ModuleId, PlainProtocol.ProtocolId),
				new Service.ProtocolFactoryHandle<>(PlainProtocol::new,
						p -> {
							server.plainDispatched.incrementAndGet();
							return Procedure.Success;
						}, TransactionLevel.None, DispatchMode.Direct));
		int port = startServer(server);

		var client = new Service("TestFnd848.Client1");
		try {
			var socket = client.newClientSocket("127.0.0.1", port, null, null);
			Assertions.assertNotNull(socket);
			await("server accepted", 10_000, () -> server.getSocketCount() >= 1);

			socket.Send(new PlainProtocol().encode()); // 不握手，直发明文应用协议
			await("server closed plaintext connection", 10_000, () -> server.closeCount.get() >= 1);
			Assertions.assertEquals(0, server.plainDispatched.get(),
					"未握手的明文协议帧必须被门禁拒绝，不得派发执行");
		} finally {
			client.stop();
			server.stop();
		}
	}

	// 注入窗口：KeyExchange明文请求帧后同批拼接明文应用协议帧（模拟MITM注入/恶意pipelining），
	// 服务端codec生效前不得解码派发该帧——门禁断连、应用handle计数为0
	@Test
	public void testPlaintextInjectedAfterKeyExchangeFrameRejected() throws Exception {
		Task.tryInitThreadPool();
		var serverKeys = genKeyPair();
		var serverPubKey = ((RSAKey)serverKeys.getPublic()).getModulus().toByteArray();
		var server = new CloseCountService("TestFnd848.Server2");
		KeyExchange.addHandler(server, serverKeys.getPrivate());
		server.AddFactoryHandle(Protocol.makeTypeId(PlainProtocol.ModuleId, PlainProtocol.ProtocolId),
				new Service.ProtocolFactoryHandle<>(PlainProtocol::new,
						p -> {
							server.plainDispatched.incrementAndGet();
							return Procedure.Success;
						}, TransactionLevel.None, DispatchMode.Direct));
		int port = startServer(server);

		var client = new Service("TestFnd848.Client2");
		client.AddFactoryHandle(KeyExchange.TypeId, new Service.ProtocolFactoryHandle<>(KeyExchange::new,
				r -> Procedure.Success, TransactionLevel.None, DispatchMode.Direct));
		try {
			var socket = client.newClientSocket("127.0.0.1", port, null, null);
			Assertions.assertNotNull(socket);
			await("server accepted", 10_000, () -> server.getSocketCount() >= 1);

			// 同一批写入：KeyExchange请求 + 明文应用协议（合规客户端收到响应前不可能发送任何帧）
			var keBytes = new KeyExchange(serverPubKey).encode();
			var plainBytes = new PlainProtocol().encode();
			var both = ByteBuffer.Allocate(keBytes.size() + plainBytes.size());
			both.Append(keBytes.Bytes, keBytes.ReadIndex, keBytes.size());
			both.Append(plainBytes.Bytes, plainBytes.ReadIndex, plainBytes.size());
			socket.Send(both);

			await("server closed injected plaintext connection", 10_000, () -> server.closeCount.get() >= 1);
			Assertions.assertEquals(0, server.plainDispatched.get(),
					"握手帧后同批注入的明文帧在codec生效前必须被门禁拒绝，不得派发");
		} finally {
			client.stop();
			server.stop();
		}
	}

	// 正常路径不受影响：完成KeyExchange后（双向codec就绪）加密发送应用协议，服务端正常派发
	@Test
	public void testEncryptedTrafficAfterHandshakePassesGate() throws Exception {
		Task.tryInitThreadPool();
		var serverKeys = genKeyPair();
		var clientKeys = genKeyPair();
		var serverPubKey = ((RSAKey)serverKeys.getPublic()).getModulus().toByteArray();

		var server = new CloseCountService("TestFnd848.Server3");
		KeyExchange.addHandler(server, serverKeys.getPrivate());
		server.AddFactoryHandle(Protocol.makeTypeId(PlainProtocol.ModuleId, PlainProtocol.ProtocolId),
				new Service.ProtocolFactoryHandle<>(PlainProtocol::new,
						p -> {
							server.plainDispatched.incrementAndGet();
							return Procedure.Success;
						}, TransactionLevel.None, DispatchMode.Direct));
		int port = startServer(server);

		var client = new Service("TestFnd848.Client3");
		client.AddFactoryHandle(KeyExchange.TypeId, new Service.ProtocolFactoryHandle<>(KeyExchange::new,
				r -> Procedure.Success, TransactionLevel.None, DispatchMode.Direct));
		try {
			var socket = (TcpSocket)client.newClientSocket("127.0.0.1", port, null, null);
			Assertions.assertNotNull(socket);
			await("server accepted", 10_000, () -> server.getSocketCount() >= 1);

			var clientPubKeyN = ((RSAKey)clientKeys.getPublic()).getModulus().toByteArray();
			var ke = new KeyExchange(serverPubKey, clientPubKeyN);
			Assertions.assertTrue(ke.send(socket, clientKeys.getPrivate()), "KeyExchange.send提交成功");
			await("client codecs installed", 10_000, socket::isSecurity); // 客户端双向codec就绪

			new PlainProtocol().Send(socket); // 加密发送（输出codec已生效）
			await("server dispatched encrypted protocol", 10_000, () -> server.plainDispatched.get() >= 1);
			Assertions.assertEquals(0, server.closeCount.get(), "正常握手的加密流量不得被门禁误杀");
		} finally {
			client.stop();
			server.stop();
		}
	}
}
