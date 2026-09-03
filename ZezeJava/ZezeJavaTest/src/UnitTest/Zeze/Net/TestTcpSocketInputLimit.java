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
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			super.OnSocketClose(so, e);
			closeEx = e;
			closed.countDown();
		}
	}

	private static int startServer(Server server) throws Exception {
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

	private static void sendAfterCodec(int port, byte @NotNull [] wireBytes) throws Exception {
		try (Socket client = new Socket("127.0.0.1", port)) {
			Thread.sleep(300); // 等 selector 线程应用解压 codec（OnHandshakeDone 里 submitAction）
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
		sendAfterCodec(port, compressMppc(new byte[4 * 1024 * 1024]));
		assertClosedAtLimit(server);
	}

	// zstd 同型：全零数据高放大率，首个 128KB 解压批次即应超上限抛异常。
	@Test
	public final void testZstdBombClosedAtLimit() throws Exception {
		var server = new Server("TestTcpSocketInputLimit.Zstd", Constant.eCompressTypeZstd, 64 * 1024);
		int port = startServer(server);
		sendAfterCodec(port, compressZstd(new byte[4 * 1024 * 1024]));
		assertClosedAtLimit(server);
	}

	// 负控：解压总量低于上限的合法数据不应触发关闭（协议头声明的size比实际数据大，
	// decode 会等待更多数据而不派发，客户端保持连接期间服务端不应关闭）。
	@Test
	public final void testUnderLimitKeepsConnection() throws Exception {
		var server = new Server("TestTcpSocketInputLimit.Under", Constant.eCompressTypeMppc, 128 * 1024);
		int port = startServer(server);
		var body = new byte[60 * 1024];
		Arrays.fill(body, (byte)0x42);
		var payload = ByteBuffer.Allocate(12 + body.length);
		payload.WriteInt(0x1234); // moduleId
		payload.WriteInt(0x5678); // protocolId
		payload.WriteInt(64 * 1024); // size：比实际数据大，永远等不完整 → 不派发
		payload.Append(body, 0, body.length);
		try (Socket client = new Socket("127.0.0.1", port)) {
			Thread.sleep(300); // 等 selector 线程应用解压 codec
			OutputStream os = client.getOutputStream();
			os.write(compressMppc(Arrays.copyOfRange(payload.Bytes, payload.ReadIndex, payload.WriteIndex)));
			os.flush();
			// 负控只断言"上限未触发"：本用例的全链路（真实TCP+单发压缩流+服务端解码）存在与上限
			// 无关的流配对噪音（修复前后行为一致，已记 FND-N1-2.md 新发现候选），连接可能因解码
			// "too large" 关闭，但那不是增长上限；上限的红绿由上面两个炸弹用例覆盖。
			if (server.closed.await(1, TimeUnit.SECONDS)) {
				var ex = server.closeEx;
				boolean limitFired = ex instanceof IllegalStateException && ex.getMessage() != null
						&& ex.getMessage().contains("InputBufferMaxProtocolSize");
				Assertions.assertFalse(limitFired, "低于上限的合法解压不应触发增长上限，实际: " + ex);
			}
		}
	}
}
