package UnitTest.Zeze.Net;

import harness.Fast;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * accept 路径生命周期契约（FND3-27）：OnSocketAccept 被调用（哪怕抛出）⟹ close→OnSocketClose
 * 恰好一次。修复前：内部构造器把异常抛回 doHandle 的 OP_ACCEPT catch，只关裸 channel，
 * 已入 socketMap 的连接永不回调 OnSocketClose——条目滞留、统计不转移、子类清理不执行
 * （默认 KeepCheckPeriod=0 时永久泄漏；KeepCheckPeriod&gt;0 的服务靠心跳迟到收尸）。
 */
@Fast
public class TestSocketAcceptCloseOnce {
	static {
		Task.tryInitThreadPool();
	}

	/**
	 * throwAfterAdd=true 模拟子类在 addSocket 之后抛出（socketMap 滞留主场景）；
	 * false 模拟超限式入册前抛出（契约(I)：未入册也必达一次 OnSocketClose）。
	 */
	public static class ThrowingAcceptService extends Service {
		public final boolean throwAfterAdd;
		public final AtomicInteger closeCount = new AtomicInteger();

		public ThrowingAcceptService(boolean throwAfterAdd) {
			super("TestSocketAcceptCloseOnce");
			this.throwAfterAdd = throwAfterAdd;
		}

		@Override
		public void OnSocketAccept(@NotNull AsyncSocket so) throws Exception {
			if (throwAfterAdd)
				addSocket(so); // 入册后再抛
			throw new RuntimeException("simulated OnSocketAccept failure");
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			super.OnSocketClose(so, e); // 出册+统计转移
			closeCount.incrementAndGet();
		}
	}

	private static int startServer(ThrowingAcceptService server) throws Exception {
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
	public void testThrowAfterAddSocket() throws Exception {
		var server = new ThrowingAcceptService(true);
		try {
			var port = startServer(server);
			try (var client = new Socket("127.0.0.1", port)) {
				await("OnSocketClose after OnSocketAccept throw", 10_000, () -> server.closeCount.get() >= 1);
			}
			Assertions.assertEquals(1, server.closeCount.get(), "OnSocketClose must fire exactly once");
			await("socketMap drained", 10_000, () -> server.getSocketCount() == 0);
		} finally {
			server.stop();
		}
	}

	@Test
	public void testThrowBeforeAddSocket() throws Exception {
		var server = new ThrowingAcceptService(false);
		try {
			var port = startServer(server);
			try (var client = new Socket("127.0.0.1", port)) {
				await("OnSocketClose for rejected accept", 10_000, () -> server.closeCount.get() >= 1);
			}
			Assertions.assertEquals(1, server.closeCount.get(), "rejected accept must also get exactly one close");
			Assertions.assertEquals(0, server.getSocketCount());
		} finally {
			server.stop();
		}
	}
}
