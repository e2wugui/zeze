package UnitTest.Zeze.Net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.TcpSocket;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-49回归（异步建连改造后重写）：所有权随构造同步发布——pending解析期间stop立即生效、
 * 随后的start直接建立新一代连接（无需补偿）；解析失败走close→OnSocketClose链重连
 * （start不再同步抛出）；持续失败期间stop立即终止重试链。
 */
@Fast
public class TestFnd849ConnectorRestartDuringStopWindow {

	/** resolveAddress接缝设门的Service：门住DNS解析，模拟慢解析窗口（构造本身非阻塞）。 */
	private static final class GatedResolveService extends Service {
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		final AtomicInteger attempts = new AtomicInteger();
		final boolean failResolve;

		GatedResolveService(String name, boolean failResolve) {
			super(name);
			this.failResolve = failResolve;
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
			if (failResolve)
				throw new UnknownHostException("simulated resolve failure");
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

	private static int startTcpServer(Service server) throws Exception {
		var listen = (TcpSocket)server.newServerSocket("127.0.0.1", 0, null);
		var local = listen.getLocalInet();
		Assertions.assertNotNull(local, "listen socket local address");
		return local.getPort();
	}

	// FND8-49场景在新不变量下的形态：pending解析期间stop立即生效，随后start立即建立新一代连接
	@Test
	public void testStopDuringPendingResolveThenStartReconnects() throws Exception {
		Task.tryInitThreadPool();
		var server = new Service("test.fnd849.server");
		var client = new GatedResolveService("test.fnd849.client", false);
		Connector connector = null;
		try {
			int port = startTcpServer(server);
			connector = new Connector("127.0.0.1", port, true);
			connector.SetService(client);
			final Connector c = connector;

			// T1: start()立即返回，所有权已发布，解析被门住
			connector.start();
			Assertions.assertTrue(client.entered.await(10, TimeUnit.SECONDS), "resolve not entered");
			Assertions.assertNotNull(connector.getSocket(), "所有权须随构造同步发布");

			// T2: pending期间stop立即生效（句柄已发布直接close）
			connector.stop();
			Assertions.assertNull(connector.getSocket());
			Assertions.assertFalse(connector.isConnected());

			// T3: stop后的start建立新一代连接
			connector.start();
			Assertions.assertNotNull(connector.getSocket());

			// T4: 放行——新一代连接建立；旧一代见isClosed放弃
			client.release.countDown();
			await("restarted connection ready", 15_000, () -> c.TryGetReadySocket() != null);
			Assertions.assertEquals(2, client.attempts.get(),
					"恰好两条解析（新旧各一），旧一代不得重复尝试，attempts=" + client.attempts.get());
		} finally {
			if (client.release.getCount() > 0)
				client.release.countDown();
			if (connector != null)
				connector.stop();
			client.stop();
			server.stop();
		}
	}

	// 契约变化：解析失败不再从start()同步抛出——经close→OnSocketClose链按退避续排重连
	@Test
	public void testResolveFailureReconnectsViaCloseChain() throws Exception {
		Task.tryInitThreadPool();
		var server = new Service("test.fnd849.fail.server");
		var client = new GatedResolveService("test.fnd849.fail.client", true);
		Connector connector = null;
		try {
			int port = startTcpServer(server);
			connector = new Connector("127.0.0.1", port, true);
			connector.SetService(client);
			final Connector c = connector;

			connector.start(); // 契约变化点：解析失败异步回收，本调用正常返回
			Assertions.assertTrue(client.entered.await(10, TimeUnit.SECONDS), "resolve not entered");
			client.release.countDown();
			await("failure close chain ran", 15_000, () -> c.getSocket() == null);

			// 退避1s后下一次start再次进入解析（仍失败→再排程）
			await("retry chain reentered resolve", 15_000, () -> client.attempts.get() >= 2);
			// 每代失败socket由close链最终回收。等待式断言：观测点可能落在下一代在途尝试的
			// 发布窗口内（所有权随构造同步发布，见testStopDuringPendingResolveThenStartReconnects），
			// 瞬时null断言与该窗口竞态（30轮压测round4假红）；socket永久滞留则此处超时红。
			await("failed generation socket reclaimed by close chain", 15_000, () -> c.getSocket() == null);
		} finally {
			if (client.release.getCount() > 0)
				client.release.countDown();
			if (connector != null)
				connector.stop();
			client.stop();
			server.stop();
		}
	}

	// 孪生#1（持续失败期间stop立即生效）：解析持续失败的重试链在stop后必须终止
	@Test
	public void testStopEffectiveOnFailingResolve() throws Exception {
		Task.tryInitThreadPool();
		var server = new Service("test.fnd849.stop.server");
		var client = new GatedResolveService("test.fnd849.stop.client", true);
		client.release.countDown(); // 不门控：解析立即失败，仅验证重试链终止
		Connector connector = null;
		try {
			int port = startTcpServer(server);
			connector = new Connector("127.0.0.1", port, true);
			connector.SetService(client);

			connector.start();
			await("retry chain running", 15_000, () -> client.attempts.get() >= 2);

			connector.stop(); // 取消排程+回收在途socket
			int attemptsAtStop = client.attempts.get();
			//noinspection BusyWait
			Thread.sleep(2_500); // 观察窗>2个重试周期（退避1s起）
			Assertions.assertEquals(attemptsAtStop, client.attempts.get(),
					"stop后重试链必须终止（attempts不得增长），attempts=" + client.attempts.get());
		} finally {
			if (client.release.getCount() > 0)
				client.release.countDown();
			if (connector != null)
				connector.stop();
			client.stop();
			server.stop();
		}
	}
}
