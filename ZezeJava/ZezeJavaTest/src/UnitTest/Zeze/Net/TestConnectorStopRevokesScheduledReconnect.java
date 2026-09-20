package UnitTest.Zeze.Net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 回归（意图代数）：已派发的重连任务阻塞在Connector锁上时stop()/setAutoReconnect(false)
 * 完整返回，旧实现中该任务随后照常建连——撤销后连接器复活。修复后旧代数任务醒来即弃；
 * stop→start重启、disable→enable重新启用不受影响。
 */
@Fast
public class TestConnectorStopRevokesScheduledReconnect {

	/** 解析必失败：驱动"构造成功→解析失败→close链→TryReconnect排程"的退避循环。 */
	private static final class FailingResolveService extends Service {
		final AtomicInteger attempts = new AtomicInteger();

		FailingResolveService(String name) {
			super(name);
		}

		@Override
		protected @NotNull InetAddress resolveAddress(@Nullable String hostNameOrAddress) throws IOException {
			attempts.incrementAndGet();
			throw new UnknownHostException("simulated resolve failure");
		}
	}

	private static void await(String what, long timeoutMs, BooleanSupplier condition) {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
			try {
				//noinspection BusyWait
				Thread.sleep(5);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		Assertions.assertTrue(condition.getAsBoolean(), what);
	}

	@Test
	public void testDispatchedReconnectTaskRevokedByStop() throws Exception {
		Task.tryInitThreadPool();
		var client = new FailingResolveService("test.stoprevoke.client");
		Connector connector = null;
		try {
			connector = new Connector("127.0.0.1", 1, true);
			connector.SetService(client);

			// 退避链运行：初次构造+至少一次排程重试（间隔1s）
			connector.start();
			await("retry chain running", 15_000, () -> client.attempts.get() >= 2);

			// 持锁覆盖下一次fire：任务取出后阻塞在本锁上，stop()以重入方式完整执行后放行——
			// 任务无论何时拿到锁携带的都是旧代数。
			connector.lock();
			try {
				//noinspection BusyWait
				Thread.sleep(3_000);
				connector.stop();
			} finally {
				connector.unlock();
			}

			int attemptsAtStop = client.attempts.get();
			Assertions.assertNull(connector.getSocket(), "stop后不得有存活的发布连接");
			//noinspection BusyWait
			Thread.sleep(3_000);
			Assertions.assertEquals(attemptsAtStop, client.attempts.get(),
					"stop后已派发的重连任务必须作废（attempts不得增长），attempts=" + client.attempts.get());

			// stop→start重启是同步新意图：新纪元，退避链恢复
			connector.start();
			await("restart reconnects", 15_000, () -> client.attempts.get() > attemptsAtStop);
		} finally {
			if (connector != null)
				connector.stop();
			client.stop();
		}
	}

	@Test
	public void testDispatchedReconnectTaskRevokedByDisableAutoReconnect() throws Exception {
		Task.tryInitThreadPool();
		var client = new FailingResolveService("test.stoprevoke.client.disable");
		Connector connector = null;
		try {
			connector = new Connector("127.0.0.1", 1, true);
			connector.SetService(client);

			// 退避链运行：初次构造+至少一次排程重试（间隔1s）
			connector.start();
			await("retry chain running", 15_000, () -> client.attempts.get() >= 2);

			// 持锁覆盖下一次fire：任务取出后阻塞在本锁上，setAutoReconnect(false)以重入方式
			// 完整执行后放行——任务无论何时拿到锁携带的都是旧代数，一律作废。
			connector.lock();
			try {
				//noinspection BusyWait
				Thread.sleep(3_000);
				connector.setAutoReconnect(false);
			} finally {
				connector.unlock();
			}

			// 撤销时若仍有在途socket，其死亡链（阻塞过的OnSocketClose→stop）随后清空socket。
			// attempts的基线须在socket清空后取：撤销前最后一发在途resolve可能在此期间落地计数。
			final var conn = connector;
			await("socket released", 5_000, () -> conn.getSocket() == null);
			int attemptsAtDisable = client.attempts.get();
			//noinspection BusyWait
			Thread.sleep(3_000);
			Assertions.assertEquals(attemptsAtDisable, client.attempts.get(),
					"disable后已派发的重连任务必须作废（attempts不得增长），attempts=" + client.attempts.get());

			// disable→enable重新启用是新意图：退避链恢复
			connector.setAutoReconnect(true);
			await("re-enable reconnects", 15_000, () -> client.attempts.get() > attemptsAtDisable);
		} finally {
			if (connector != null)
				connector.stop();
			client.stop();
		}
	}
}
