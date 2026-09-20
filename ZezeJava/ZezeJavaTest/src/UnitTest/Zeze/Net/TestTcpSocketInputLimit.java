package UnitTest.Zeze.Net;

import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Net.AsyncSocket;
import Zeze.Net.BufferCodec;
import Zeze.Net.Compress;
import Zeze.Net.CompressZstd;
import Zeze.Net.Protocol;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.Handshake.Constant;
import Zeze.Util.Task;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * FND-N1-2 回归：解压输入路径的流式增长上限。
 * 恶意高放大率压缩数据在解压 sink 侧达到 InputBufferMaxProtocolSize 即抛异常关闭连接，
 * 而不是先无上限解压进 codecBuf 再检查（压缩放大 DoS，单包可放大百倍内存）。
 */
@Fast
public class TestTcpSocketInputLimit {
	static {
		Task.tryInitThreadPool();
	}

	public static class Server extends Service {
		public final int compressType;
		public final CountDownLatch closed = new CountDownLatch(1);
		public volatile @Nullable Throwable closeEx;
		// 边界用例观察点：完整协议帧送达（走 dispatchUnknownProtocol，moduleId/protocolId 未注册）。
		public final CountDownLatch received = new CountDownLatch(1);
		public volatile int receivedBodySize = -1; // 最近一次完整收到的协议body大小（帧内声明size）

		public Server(String name, int compressType, int maxInputProtocolSize) {
			super(name);
			this.compressType = compressType;
			getSocketOptions().setInputBufferMaxProtocolSize(maxInputProtocolSize);
		}

		@Override
		public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
			super.OnHandshakeDone(so);
			if (so instanceof TcpSocket tcp)
				tcp.setInputSecurityCodec(Constant.eEncryptTypeDisable, null, compressType);
		}

		@Override
		public void dispatchUnknownProtocol(@NotNull AsyncSocket so, int moduleId, int protocolId,
		                                    @NotNull ByteBuffer data) {
			// 默认实现抛UnsupportedOperationException会关连接；这里记录大小证明帧被完整接收。
			receivedBodySize = data.size(); // decode窗口内：ReadIndex跳过帧头、WriteIndex=帧尾，恰为body大小
			received.countDown();
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			super.OnSocketClose(so, e);
			closeEx = e;
			closed.countDown();
		}
	}

	// 2026-09-20审核：5个用例原先无任何stop——监听channel与socketMap登记跨用例泄漏至JVM退出。
	// startServer登记，@AfterEach统一收口。
	private static final java.util.ArrayList<Server> started = new java.util.ArrayList<>();

	@org.junit.jupiter.api.AfterEach
	public void stopServers() throws Exception {
		for (var s : started) {
			try {
				s.stop();
			} catch (Throwable ignored) {
			}
		}
		started.clear();
	}

	private static int startServer(Server server) throws Exception {
		started.add(server);
		var listen = (TcpSocket)server.newServerSocket("127.0.0.1", 0, null);
		var local = listen.getLocalInet();
		Assertions.assertNotNull(local, "listen socket local address");
		return local.getPort();
	}

	private static byte[] compressMppc(byte @NotNull [] payload) {
		var sink = new BufferCodec();
		var cp = new Compress(sink);
		cp.update(payload, 0, payload.length);
		cp.flush();
		return Arrays.copyOfRange(sink.Bytes, sink.ReadIndex, sink.WriteIndex);
	}

	private static byte[] compressZstd(byte @NotNull [] payload) {
		var sink = new BufferCodec();
		var cp = new CompressZstd(sink);
		cp.update(payload, 0, payload.length);
		cp.flush();
		return Arrays.copyOfRange(sink.Bytes, sink.ReadIndex, sink.WriteIndex);
	}

	// 构造完整协议帧：12字节帧头(moduleId/protocolId/size，小端定长4字节——须用WriteInt4，
	// WriteInt是变长编码) + body。moduleId/protocolId 未注册，完整收到时走
	// dispatchUnknownProtocol（Server 里记录body大小作为接收成功的观察点）。
	private static byte[] makeFrame(int bodySize) {
		var body = new byte[bodySize];
		Arrays.fill(body, (byte)0x42);
		var payload = ByteBuffer.Allocate(Protocol.HEADER_SIZE + bodySize);
		payload.WriteInt4(0x1234); // moduleId
		payload.WriteInt4(0x5678); // protocolId
		payload.WriteInt4(bodySize); // size：body大小
		payload.Append(body, 0, bodySize);
		return Arrays.copyOfRange(payload.Bytes, payload.ReadIndex, payload.WriteIndex);
	}

	private static void sendAfterCodec(Server server, int port, byte @NotNull [] wireBytes) throws Exception {
		try (Socket client = new Socket("127.0.0.1", port)) {
			// 原先裸sleep(300)等codec装配（2026-09-20审核：假同步点，满载下codec未装即发送
			// →压缩帧被当裸帧解码→假红）。确定性轮询服务端socket的inputCodecChain非空。
			var field = TcpSocket.class.getDeclaredField("inputCodecChain");
			field.setAccessible(true);
			long deadline = System.currentTimeMillis() + 10_000;
			while (true) {
				var so = server.GetSocket();
				if (so instanceof TcpSocket tcp && field.get(tcp) != null)
					break;
				Assertions.assertTrue(System.currentTimeMillis() < deadline, "10s内解压codec未装配（握手未完成？）");
				//noinspection BusyWait
				Thread.sleep(10);
			}
			OutputStream os = client.getOutputStream();
			os.write(wireBytes);
			os.flush();
		}
	}

	private static void assertClosedAtLimit(Server server) throws InterruptedException {
		Assertions.assertTrue(server.closed.await(5, TimeUnit.SECONDS), "等不到连接关闭：解压增长上限未生效？");
		var ex = server.closeEx;
		Assertions.assertTrue(ex instanceof IllegalStateException
						&& ex.getMessage() != null && ex.getMessage().contains("InputBufferMaxProtocolSize"),
				() -> "期待 InputBufferMaxProtocolSize 异常，实际: " + ex);
	}

	// 全零数据经 MPPC 压缩后放大率极高：解压 sink 应在增长到上限时抛 InputBufferMaxProtocolSize
	// 关闭连接，而不是无上限解压完再检查。
	@Test
	public final void testMppcBombClosedAtLimit() throws Exception {
		var server = new Server("TestTcpSocketInputLimit.Mppc", Constant.eCompressTypeMppc, 64 * 1024);
		int port = startServer(server);
		sendAfterCodec(server, port, compressMppc(new byte[4 * 1024 * 1024]));
		assertClosedAtLimit(server);
	}

	// zstd 同型：全零数据高放大率，首个 128KB 解压批次即应超上限抛异常。
	@Test
	public final void testZstdBombClosedAtLimit() throws Exception {
		var server = new Server("TestTcpSocketInputLimit.Zstd", Constant.eCompressTypeZstd, 64 * 1024);
		int port = startServer(server);
		sendAfterCodec(server, port, compressZstd(new byte[4 * 1024 * 1024]));
		assertClosedAtLimit(server);
	}

	// 边界回归（FND6-12）：body恰为max的完整协议（压缩开启）解压总量=HEADER_SIZE+max恰达限值，
	// InputLimitCodec 与 processReceive 残留检查均用 > 比较，必须允许（接收成功）而非误杀。
	@Test
	public final void testBodyExactlyMaxCompressedReceived() throws Exception {
		int max = 64 * 1024;
		var server = new Server("TestTcpSocketInputLimit.MaxCompressed", Constant.eCompressTypeMppc, max);
		int port = startServer(server);
		sendAfterCodec(server, port, compressMppc(makeFrame(max)));
		Assertions.assertTrue(server.received.await(5, TimeUnit.SECONDS), "body==max的压缩协议应被完整接收");
		Assertions.assertEquals(max, server.receivedBodySize, "收到的body大小应恰为max");
	}

	// 边界回归：直通路径（compressType=disable，codec链不扩展仅透传进 inputBuffer）body恰为max。
	// max取64KB==默认readBufferSize，帧(HEADER_SIZE+max)必然跨多次read，恰经 processReceive 的
	// 残留检查路径：首轮decode等剩余数据后 remain==max，须 <= HEADER_SIZE+max 放行（用 > 允许恰达限值）。
	@Test
	public final void testBodyExactlyMaxPassthroughReceived() throws Exception {
		int max = 64 * 1024; // ==默认readBufferSize(64KB)，帧跨多次read，残留检查路径必经
		var server = new Server("TestTcpSocketInputLimit.MaxPassthrough", Constant.eCompressTypeDisable, max);
		int port = startServer(server);
		sendAfterCodec(server, port, makeFrame(max)); // 不压缩直发
		Assertions.assertTrue(server.received.await(5, TimeUnit.SECONDS), "body==max的直通协议应被完整接收");
		Assertions.assertEquals(max, server.receivedBodySize, "收到的body大小应恰为max");
	}

	// 边界回归：body=max+1（压缩开启）解压总量=HEADER_SIZE+max+1超限，InputLimitCodec 流式拒绝，
	// 按既有断言方式验证被拒。注：直通路径完整帧一次到达时帧级检查不拦截（数据够则优先处理），
	// 需分片到达才触发帧级"too large"，属另一检查点，故被拒用例以压缩路径的量纲检查为准。
	@Test
	public final void testBodyOverMaxRejected() throws Exception {
		int max = 64 * 1024;
		var server = new Server("TestTcpSocketInputLimit.OverMax", Constant.eCompressTypeMppc, max);
		int port = startServer(server);
		sendAfterCodec(server, port, compressMppc(makeFrame(max + 1)));
		assertClosedAtLimit(server);
	}

	// 负控：解压总量低于上限的合法数据不应触发关闭（协议头声明的size比实际数据大，
	// decode 会等待更多数据而不派发，客户端保持连接期间服务端不应关闭）。
	// 注：本用例曾误用变长WriteInt写帧头导致解码乱序关闭，被误记为"流配对噪音"
	// （FND-N1-2新发现候选）——实为测试自身bug，改用WriteInt4后噪音消失。
	@Test
	public final void testUnderLimitKeepsConnection() throws Exception {
		var server = new Server("TestTcpSocketInputLimit.Under", Constant.eCompressTypeMppc, 128 * 1024);
		int port = startServer(server);
		var body = new byte[60 * 1024];
		Arrays.fill(body, (byte)0x42);
		var payload = ByteBuffer.Allocate(12 + body.length);
		payload.WriteInt4(0x1234); // moduleId
		payload.WriteInt4(0x5678); // protocolId
		payload.WriteInt4(64 * 1024); // size：比实际数据大，永远等不完整 → 不派发
		payload.Append(body, 0, body.length);
		try (Socket client = new Socket("127.0.0.1", port)) {
			Thread.sleep(300); // 等 selector 线程应用解压 codec
			OutputStream os = client.getOutputStream();
			os.write(compressMppc(Arrays.copyOfRange(payload.Bytes, payload.ReadIndex, payload.WriteIndex)));
			os.flush();
			// 负控只断言"上限未触发"：帧头声明的size大于实际数据，decode永远等待不派发；
			// 若连接因任何原因关闭，不得是增长上限（上限的红绿由上面两个炸弹用例覆盖）。
			if (server.closed.await(1, TimeUnit.SECONDS)) {
				var ex = server.closeEx;
				boolean limitFired = ex instanceof IllegalStateException && ex.getMessage() != null
						&& ex.getMessage().contains("InputBufferMaxProtocolSize");
				Assertions.assertFalse(limitFired, "低于上限的合法解压不应触发增长上限，实际: " + ex);
			}
		}
	}
}
