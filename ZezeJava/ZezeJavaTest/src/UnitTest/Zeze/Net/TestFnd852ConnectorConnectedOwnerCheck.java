package UnitTest.Zeze.Net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Util.Task;
import Zeze.Util.TimeThrottle;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-52回归（异步建连改造后重写）：属主校验收缩为socket==so——所有权随构造同步发布，
 * socket!=so必为stale回调（stop已置null或已被新一代取代），无力改写连接器状态。
 */
@Fast
public class TestFnd852ConnectorConnectedOwnerCheck {

	/** 最小AsyncSocket桩：OnSocketConnected只读连接器状态，不触达传输。 */
	private static final class StubSocket extends AsyncSocket {
		StubSocket(Service service) {
			super(service);
		}

		@Override
		public Type getType() {
			return Type.eClient;
		}

		@Override
		public @Nullable SocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public @Nullable TimeThrottle getTimeThrottle() {
			return null;
		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		public boolean close(@Nullable Throwable ex, boolean gracefully) {
			return true;
		}

		@Override
		public boolean Send(byte @NotNull [] bytes, int offset, int length) {
			return true;
		}
	}

	/** resolveAddress接缝设门的Service：门住DNS解析，保持连接器处于pending建连窗口。 */
	private static final class GatedResolveService extends Service {
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		final AtomicInteger attempts = new AtomicInteger();

		GatedResolveService(String name) {
			super(name);
		}

		@Override
		protected @NotNull InetAddress resolveAddress(@Nullable String hostNameOrAddress) throws IOException {
			attempts.incrementAndGet();
			entered.countDown();
			try {
				release.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return super.resolveAddress(hostNameOrAddress);
		}
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

	// stop废弃pending在途连接后，迟到的OnSocketConnected不得置isConnected（误置后无人纠正）
	@Test
	public void testStaleConnectedAfterStopRejected() throws Exception {
		Task.tryInitThreadPool();
		var server = new Service("test.fnd852.server");
		var client = new GatedResolveService("test.fnd852.client");
		Connector connector = null;
		try {
			var listen = (TcpSocket)server.newServerSocket("127.0.0.1", 0, null);
			int port = listen.getLocalInet().getPort();
			connector = new Connector("127.0.0.1", port, true);
			connector.SetService(client);

			// start立即返回，socket已发布，解析被门住
			connector.start();
			Assertions.assertTrue(client.entered.await(10, TimeUnit.SECONDS), "resolve not entered");
			AsyncSocket pending = connector.getSocket();
			Assertions.assertNotNull(pending, "所有权须随构造同步发布");
			Assertions.assertFalse(connector.isConnected());

			connector.stop(); // 句柄直接close
			// 废弃连接的迟到回调 + 任意非owner桩：均拒绝
			connector.OnSocketConnected(pending);
			Assertions.assertFalse(connector.isConnected(), "被stop废弃的stale连接不得置isConnected=true");
			connector.OnSocketConnected(new StubSocket(client));
			Assertions.assertFalse(connector.isConnected(), "非owner的stale回调不得置isConnected=true");

			client.release.countDown(); // resolver醒来见isClosed放弃
			await("stale resolve abandoned", 15_000, () -> client.attempts.get() >= 1);
			Assertions.assertFalse(connector.isConnected(), "回收后isConnected仍须为false");
		} finally {
			if (client.release.getCount() > 0)
				client.release.countDown();
			if (connector != null)
				connector.stop();
			client.Stop();
			server.Stop();
		}
	}

	// owner回调（socket==so）放行；stop置null后同一socket的回调转为拒绝
	@Test
	public void testOwnerSocketConnectedAccepted() throws Exception {
		Task.tryInitThreadPool();
		var server = new Service("test.fnd852.b.server");
		var client = new Service("test.fnd852.b.client");
		Connector connector = null;
		try {
			var listen = (TcpSocket)server.newServerSocket("127.0.0.1", 0, null);
			int port = listen.getLocalInet().getPort();
			connector = new Connector("127.0.0.1", port, true);
			connector.SetService(client);
			final Connector c = connector;

			connector.start();
			await("connection ready", 15_000, () -> c.TryGetReadySocket() != null);
			Assertions.assertTrue(connector.isConnected(), "真实连接的Connected回调须已置位");

			AsyncSocket so = connector.TryGetReadySocket();
			connector.OnSocketConnected(so); // owner重复回调：放行（幂等）
			Assertions.assertTrue(connector.isConnected(), "owner连接的回调必须放行");

			connector.stop();
			connector.OnSocketConnected(so); // 已被stop弃置：拒绝
			Assertions.assertFalse(connector.isConnected(), "stop后的stale回调不得置位");
		} finally {
			if (connector != null)
				connector.stop();
			client.Stop();
			server.Stop();
		}
	}
}
