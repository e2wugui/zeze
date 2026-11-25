package Zeze.Raft;

import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Builtin.ServiceManagerWithRaft.Login;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.OutObject;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestProxyRequestEncodingAndGuard {
	private static final class CapturingSocket extends AsyncSocket {
		final AtomicReference<byte @Nullable []> lastSent = new AtomicReference<>();

		CapturingSocket(Service service) {
			super(service);
			setHandshakeDone(true);
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		protected void doClose(@Nullable Throwable ex, boolean gracefully) {
		}

		@Override
		public boolean Send(byte[] bytes, int offset, int length) {
			var copy = new byte[length];
			System.arraycopy(bytes, offset, copy, 0, length);
			lastSent.set(copy);
			return true;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}
	}

	private static Service.@Nullable ProtocolFactoryHandle<?> loginFactory(long typeId) {
		return typeId == Login.TypeId_
				? new Service.ProtocolFactoryHandle<>(Login::new, null, TransactionLevel.None, DispatchMode.Direct)
				: null;
	}

	@Test
	void proxyArgumentAndDirectSendIgnoreConcurrentResponseMutation() {
		Task.tryInitThreadPool();

		// 1. 普通请求（初始 resultCode=0）并发被应答/超时线程改写 isRequest=false、resultCode=Timeout。
		var rpc = new Login();
		rpc.Argument.setSessionName("session-alpha");
		rpc.getUnique().setClientId("client-a");
		rpc.getUnique().setRequestId(11);
		rpc.setCreateTime(123456L);
		((RaftRpc<?, ?>)rpc).freezeRequestState();

		rpc.setRequest(false);
		rpc.setIsTimeout(true);
		rpc.setResultCode(Procedure.Timeout);

		var proxyArg = new ProxyArgument("node-1", rpc);
		var decoded = (Login)Protocol.decode(TestProxyRequestEncodingAndGuard::loginFactory,
				ByteBuffer.Wrap(proxyArg.getRpcBinary()), new OutObject<>());
		assertNotNull(decoded);
		assertTrue(decoded.isRequest(), "代理重发必须始终编码为 RaftRequest");
		assertEquals(0L, decoded.getResultCode(), "代理重发不得携带并发写入的 Timeout 错误码");
		assertEquals("session-alpha", decoded.Argument.getSessionName());
		assertEquals(11L, decoded.getUnique().getRequestId());

		// 2. 携带初始请求码（如 AcquireFreshSource）的请求冻结后，并发改写不得覆盖原请求码。
		var freshRpc = new Login();
		freshRpc.Argument.setSessionName("session-fresh");
		freshRpc.getUnique().setClientId("client-b");
		freshRpc.getUnique().setRequestId(12);
		freshRpc.setResultCode(GlobalCacheManagerConst.AcquireFreshSource);
		((RaftRpc<?, ?>)freshRpc).freezeRequestState();

		freshRpc.setRequest(false);
		freshRpc.setResultCode(Procedure.Timeout);

		var freshArg = new ProxyArgument("node-1", freshRpc);
		var decodedFresh = (Login)Protocol.decode(TestProxyRequestEncodingAndGuard::loginFactory,
				ByteBuffer.Wrap(freshArg.getRpcBinary()), new OutObject<>());
		assertNotNull(decodedFresh);
		assertTrue(decodedFresh.isRequest());
		assertEquals(GlobalCacheManagerConst.AcquireFreshSource, decodedFresh.getResultCode(),
				"初始请求码（AcquireFreshSource）必须在冻结后完整保留");
		assertEquals("session-fresh", decodedFresh.Argument.getSessionName());

		// 3. 直连模式 RaftRpc.Send 同样不受并发 isRequest=false / resultCode=Timeout 污染。
		var dummyService = new Service("test.direct.send", (Config)null);
		var socket = new CapturingSocket(dummyService);
		assertTrue(rpc.Send(socket));
		var sentBytes = socket.lastSent.get();
		assertNotNull(sentBytes);
		var directDecoded = (Login)Protocol.decode(TestProxyRequestEncodingAndGuard::loginFactory,
				ByteBuffer.Wrap(sentBytes), new OutObject<>());
		assertNotNull(directDecoded);
		assertTrue(directDecoded.isRequest());
		assertEquals(0L, directDecoded.getResultCode());
		assertEquals("session-alpha", directDecoded.Argument.getSessionName());
	}

	@Test
	void proxyServerRejectsNonRequestInnerRaftRpc(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var conf = RaftConfig.loadFromString("""
				<raft Name="127.0.0.1:19820" DbHome="%s">
				  <node Host="127.0.0.1" Port="19820"/>
				  <node Host="127.0.0.1" Port="19821"/>
				  <node Host="127.0.0.1" Port="19822"/>
				</raft>
				""".formatted(home));
		var raft = new Raft(new StateMachine() {
			@Override
			public SnapshotResult snapshot(String path) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void loadSnapshot(String path) {
			}
		}, null, conf, new Config());
		var proxyServer = new ProxyServer(new Config(), 5000);
		try {
			var stateField = Raft.class.getDeclaredField("state");
			stateField.setAccessible(true);
			stateField.set(raft, Raft.RaftState.Leader);

			var handled = new AtomicInteger();
			raft.getServer().AddFactoryHandle(Login.TypeId_, new Service.ProtocolFactoryHandle<>(
					Login::new, p -> {
						handled.incrementAndGet();
						return 0L;
					}, TransactionLevel.None, DispatchMode.Direct));
			proxyServer.addRaft(raft);

			// 构造携带 isRequest=false 的畸形 RaftRpc 载荷，验证 ProxyServer 直接拒绝而不派发业务。
			var malformed = new Login();
			malformed.getUnique().setClientId("client-bad");
			malformed.getUnique().setRequestId(99);
			malformed.setCreateTime(System.currentTimeMillis());
			malformed.setRequest(false);
			malformed.setResultCode(Procedure.Timeout);

			var proxyReq = new ProxyRequest(new ProxyArgument(raft.getName(), new Binary(malformed.encode())));
			var method = ProxyServer.class.getDeclaredMethod("ProcessProxyRequest", ProxyRequest.class);
			method.setAccessible(true);
			assertEquals(Procedure.LogicError, method.invoke(proxyServer, proxyReq),
					"非请求态内层 RaftRpc 必须被 ProxyServer 拒绝");
			assertEquals(0, handled.get(), "畸形内层响应不得进入业务处理器");
		} finally {
			proxyServer.removeRaft(raft);
			raft.shutdown();
		}
	}

	@Test
	void proxyAgentReturnsProviderNotExistWhenClientIsNull(@TempDir Path home) throws Exception {
		Task.tryInitThreadPool();
		var conf = RaftConfig.loadFromString("""
				<raft Name="127.0.0.1:19830" DbHome="%s">
				  <node Host="127.0.0.1" Port="19830"/>
				  <node Host="127.0.0.1" Port="19831"/>
				  <node Host="127.0.0.1" Port="19832"/>
				</raft>
				""".formatted(home));
		var proxyAgent = new ProxyAgent(5000);
		var agent = new Agent("waitForLeader", conf, new Config());
		agent.stop(); // client 已置空
		try {
			// 模拟 Agent.stop() 置空 client 与 ProxyAgent.ProcessProxyRequest 并发的交错窗口。
			var agentsField = ProxyAgent.class.getDeclaredField("agents");
			agentsField.setAccessible(true);
			@SuppressWarnings("unchecked")
			var agentsMap = (ConcurrentHashMap<String, Agent>)agentsField.get(proxyAgent);
			agentsMap.put("127.0.0.1:19830", agent);

			var req = new ProxyRequest(new ProxyArgument("127.0.0.1:19830", Binary.Empty));
			var method = ProxyAgent.class.getDeclaredMethod("ProcessProxyRequest", ProxyRequest.class);
			method.setAccessible(true);
			assertEquals(Procedure.ProviderNotExist, method.invoke(proxyAgent, req),
					"Agent.client 已置空时必须返回 ProviderNotExist 而非 NPE");
		} finally {
			proxyAgent.stop();
		}
	}
}
