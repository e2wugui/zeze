package Benchmark;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import harness.Bench;
import harness.MacroBench;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Assertions;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.TaskCompletionSource;

// 网络回环场景：纯 Zeze.Net 层的 Rpc 往返吞吐（encode → TCP loopback → decode → 处理 →
// 应答 → 回程），不依赖 Zezex/App 栈——net 路径的核心基准化（BenchSocket 是编解码微基准，
// 不含真实收发；BenchRoleTimer 是 Zezex 全栈，负载混入服务层）。形态对齐 TestRpc 的最小
// 回环骨架：临时端口 Acceptor + 同 JVM Connector；64 深度流水线窗口（环形槽位等待最老
// Future）饱和收发两侧。payload=256B 定长（32 long），典型游戏消息档位。
// 服务端处理器只做 echo（Result.assign(Argument)），测的是纯网络栈成本；事务/存储开销为 0。
@SuppressWarnings("NewClassNamingConvention")
@Bench
@Tag("core")
public class JSocketRpcLoopback {
	public static final int TxnCount = 200_000;
	public static final int Window = 64;   // 流水线深度
	public static final int Warmups = 2;
	public static final int Rounds = 5;

	// 256B 定长 payload（32 long）：游戏消息典型档位，encode/decode 成本稳定可复现
	public static class Payload extends Bean {
		public final long[] values = new long[32];

		@Override
		public void encode(ByteBuffer bb) {
			for (var v : values)
				bb.WriteLong(v);
		}

		@Override
		public void decode(IByteBuffer bb) {
			for (int i = 0; i < values.length; ++i)
				values[i] = bb.ReadLong();
		}
	}

	public static class BenchRpc extends Rpc<Payload, Payload> {
		public BenchRpc() {
			Argument = new Payload();
			Result = new Payload();
		}

		@Override
		public int getModuleId() {
			return 0x7f3a; // 手工基准协议，避开 demo 模块号段（本场景独立 Service，无注册交集）
		}

		@Override
		public int getProtocolId() {
			return -3;
		}
	}

	public static long ProcessEcho(Protocol<?> p) {
		var rpc = (BenchRpc)p;
		System.arraycopy(rpc.Argument.values, 0, rpc.Result.values, 0, rpc.Result.values.length);
		rpc.SendResult();
		return Procedure.Success;
	}

	private static @Nullable Service server;
	private static @Nullable Service client;
	private static @Nullable AsyncSocket clientSocket;

	@Test
	public void testBenchmark() throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		setupLoopback();
		try {
			MacroBench.run("J_SocketRpcLoopback", Warmups, Rounds, TxnCount, () -> {
				@SuppressWarnings("unchecked")
				var futures = new Future<?>[Window];
				for (int i = 0; i < TxnCount; ++i) {
					int slot = i % Window;
					if (futures[slot] != null)
						futures[slot].get(); // 等最老请求出窗（流水线饱和）
					var rpc = new BenchRpc();
					for (int k = 0; k < rpc.Argument.values.length; ++k)
						rpc.Argument.values[k] = i + k;
					futures[slot] = rpc.SendForWait(clientSocket, 30_000);
				}
				for (var f : futures)
					if (f != null)
						f.get();
			});
			// 末笔校验（测量轮外）：echo 必须原样带回
			var probe = new BenchRpc();
			probe.Argument.values[0] = 0x5a5a5a5aL;
			probe.SendForWait(clientSocket, 30_000).get();
			Assertions.assertEquals(0x5a5a5a5aL, probe.Result.values[0]);
		} finally {
			teardownLoopback();
		}
	}

	private static void setupLoopback() throws Exception {
		var connected = new TaskCompletionSource<AsyncSocket>();
		server = new Service("JSocketRpc.Server");
		var rpc = new BenchRpc();
		server.AddFactoryHandle(rpc.getTypeId(), new Service.ProtocolFactoryHandle<>(BenchRpc::new, JSocketRpcLoopback::ProcessEcho));
		var listener = (Zeze.Net.TcpSocket)server.newServerSocket("127.0.0.1", 0, null);
		int port = Objects.requireNonNull(listener.getLocalInet()).getPort();

		client = new Service("JSocketRpc.Client") {
			@Override
			public void OnSocketConnected(AsyncSocket so) throws Exception {
				super.OnSocketConnected(so);
				connected.setResult(so);
			}
		};
		client.AddFactoryHandle(rpc.getTypeId(), new Service.ProtocolFactoryHandle<>(BenchRpc::new));
		clientSocket = client.newClientSocket("127.0.0.1", port, null, null);
		Assertions.assertNotNull(connected.get(10, java.util.concurrent.TimeUnit.SECONDS), "10s内连接必须建立");
	}

	private static void teardownLoopback() throws Exception {
		if (client != null)
			client.stop();
		if (server != null)
			server.stop();
		client = null;
		server = null;
		clientSocket = null;
	}
}
