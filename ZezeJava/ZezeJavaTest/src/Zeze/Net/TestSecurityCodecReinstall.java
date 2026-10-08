package Zeze.Net;

import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Services.Handshake.Constant;
import Zeze.Util.Task;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * FND15 net-01 回归：安全codec链同方向只允许装配一次。
 * 重复握手/KeyExchange重放驱动二次装配时，旧链（zstd为NoFinalizer变体，native上下文仅close()释放、
 * 无GC兜底）被覆盖后无人close——native内存永久泄漏。修复：装配前绊线以re-install错误断连。
 */
@Fast
public class TestSecurityCodecReinstall {
	static {
		Task.tryInitThreadPool();
	}

	public static class Server extends Service {
		public final CountDownLatch closed = new CountDownLatch(1);
		public volatile @Nullable Throwable closeEx;

		public Server(String name) {
			super(name);
		}

		@Override
		public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
			super.OnHandshakeDone(so);
			if (so instanceof TcpSocket tcp)
				tcp.setInputSecurityCodec(Constant.eEncryptTypeDisable, null, Constant.eCompressTypeZstd);
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			super.OnSocketClose(so, e);
			closeEx = e;
			closed.countDown();
		}
	}

	// 二次装配（重复握手/重放的传输层形态）必须断连而不是静默覆盖旧链。
	// 修复前红：第二次装配静默成功、旧zstd链失引用泄漏、连接不关闭（await超时失败）。
	@Test
	public final void testReinstallClosesConnection() throws Exception {
		var server = new Server("TestSecurityCodecReinstall");
		var listen = (TcpSocket)server.newServerSocket("127.0.0.1", 0, null);
		var local = listen.getLocalInet();
		Assertions.assertNotNull(local, "listen socket local address");
		int port = local.getPort();
		try (Socket client = new Socket("127.0.0.1", port)) {
			// 确定性等待首次装配完成（反射轮询对齐TestTcpSocketInputLimit判据）
			var field = TcpSocket.class.getDeclaredField("inputCodecChain");
			field.setAccessible(true);
			long deadline = System.currentTimeMillis() + 10_000;
			TcpSocket serverSide;
			while (true) {
				if (server.GetSocket() instanceof TcpSocket tcp && field.get(tcp) != null) {
					serverSide = tcp;
					break;
				}
				Assertions.assertTrue(System.currentTimeMillis() < deadline, "10s内codec未装配（连接未建立？）");
				//noinspection BusyWait
				Thread.sleep(10);
			}
			// 第二次装配：必须以 re-install 错误断连，旧链保留给realClose正常释放。
			serverSide.setInputSecurityCodec(Constant.eEncryptTypeDisable, null, Constant.eCompressTypeZstd);
			Assertions.assertTrue(server.closed.await(5, TimeUnit.SECONDS), "二次装配未断连：旧codec链被静默覆盖（泄漏）");
			var ex = server.closeEx;
			Assertions.assertTrue(ex instanceof IllegalStateException
							&& ex.getMessage() != null && ex.getMessage().contains("re-install"),
					() -> "期待 re-install 断连异常，实际: " + ex);
		}
		try {
			server.stop();
		} catch (Throwable ignored) {
		}
	}
}
