package UnitTest.Zeze.Net;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Net.WebsocketHandle;
import Zeze.Netty.HttpServer;
import Zeze.Netty.Netty;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-49回归：stop()打断在途连接构造的窗口期内（构造内含阻塞DNS可达数秒）到达的
 * start()/TryReconnect()被静默吞没，abort丢弃尾段又无补偿——Connector永久停机无告警
 * （closedInWindow尾段有TryReconnect补偿，两条对称丢弃路径只有一条带补偿）。
 * 孪生#1：构造异常catch无条件TryReconnect，stop对"构造持续失败"的连接器不生效。
 * 修复后：窗口期启动意图记录为restartRequested（仅请求晚于stop时），abort丢弃尾段
 * 补偿start()（非TryReconnect——对autoReconnect=false静默失效）；构造异常catch加
 * abort守卫并消费abort标志。
 */
@Fast
public class TestFnd849ConnectorRestartDuringStopWindow {

	/** newWebsocketClient可重载（newClientSocket为final），在构造入口设门模拟长DNS窗口。 */
	private static final class GatedWsService extends Service {
		final CountDownLatch entered = new CountDownLatch(1);
		final CountDownLatch release = new CountDownLatch(1);
		final AtomicInteger attempts = new AtomicInteger();
		final boolean failConstruction;

		GatedWsService(String name, boolean failConstruction) {
			super(name);
			this.failConstruction = failConstruction;
		}

		@Override
		public @NotNull AsyncSocket newWebsocketClient(@NotNull String url, @Nullable Object userState,
													   @Nullable Connector connector) {
			attempts.incrementAndGet();
			entered.countDown();
			try {
				release.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (failConstruction)
				throw new RuntimeException("simulated construction failure");
			return super.newWebsocketClient(url, userState, connector);
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

	private static void stopWindowScenario(java.util.function.Consumer<Connector> swallowedRequest) throws Exception {
		Task.tryInitThreadPool();
		var netty = new Netty(1);
		var server = new HttpServer();
		try {
			var wsService = new Service("test.fnd849.server");
			var handle = new WebsocketHandle("/ws", server);
			handle.setService(wsService);
			handle.start();
			var port = ((InetSocketAddress)server.start(netty, 0).sync().channel().localAddress()).getPort();

			var client = new GatedWsService("test.fnd849.client", false);
			var connector = new Connector(true, "ws://127.0.0.1:" + port + "/ws");
			connector.SetService(client);
			try {
				// T1: start()进入构造（窗口打开）
				var t1 = new Thread(connector::start, "fnd849-start");
				t1.start();
				Assertions.assertTrue(client.entered.await(10, TimeUnit.SECONDS), "construction not entered");

				// T2: stop()打断在途构造（abortConnect=true，立即返回不等待）
				connector.stop();

				// T3: 窗口期内的重启请求（修复前被静默吞掉）
				swallowedRequest.accept(connector);

				// T4: 放行在途构造——abort丢弃尾段必须按restartRequested补偿重启
				client.release.countDown();
				t1.join(15_000);
				Assertions.assertFalse(t1.isAlive(), "in-flight start must return");

				await("compensated restart connected", 15_000, () -> connector.TryGetReadySocket() != null);
				Assertions.assertEquals(2, client.attempts.get(),
						"窗口期启动意图必须被补偿（第二次构造建立连接），attempts=" + client.attempts.get());
			} finally {
				connector.stop();
				client.Stop();
				wsService.Stop();
			}
		} finally {
			server.close();
			netty.close();
		}
	}

	// 窗口期内start()被吞：修复后abort丢弃尾段补偿start()，连接恢复
	@Test
	public void testStartDuringStopWindowCompensated() throws Exception {
		stopWindowScenario(Connector::start); // 修复前：connecting窗口内静默return，意图丢失
	}

	// 窗口期内TryReconnect()被吞（setAutoReconnect(true)内部同路径）：同补偿
	@Test
	public void testTryReconnectDuringStopWindowCompensated() throws Exception {
		stopWindowScenario(Connector::TryReconnect);
	}

	// 孪生#1：构造持续失败期间stop必须立即生效——abort后构造异常catch不得再续排重试
	@Test
	public void testStopEffectiveOnFailingConstruction() throws Exception {
		Task.tryInitThreadPool();
		var client = new GatedWsService("test.fnd849.fail", true);
		var connector = new Connector(true, "ws://127.0.0.1:1/dead");
		connector.SetService(client);
		try {
			var t1 = new Thread(() -> {
				try {
					connector.start();
				} catch (Exception expected) { // 构造失败由start抛出（含补偿链）
				}
			}, "fnd849-fail-start");
			t1.start();
			Assertions.assertTrue(client.entered.await(10, TimeUnit.SECONDS), "construction not entered");
			connector.stop(); // 构造期间stop：abortConnect=true
			client.release.countDown();
			t1.join(15_000);
			Assertions.assertFalse(t1.isAlive(), "failing start must return");

			// 修复前：catch无条件TryReconnect，约1s后（reConnectDelay初值1000ms）再次构造，
			// 重试链无视stop持续发起；修复后无重试排程。观察窗>2个重试周期。
			//noinspection BusyWait
			Thread.sleep(2_500);
			Assertions.assertEquals(1, client.attempts.get(),
					"stop后构造失败重试链必须终止（attempts不得增长），attempts=" + client.attempts.get());
		} finally {
			client.release.countDown();
			connector.stop();
			client.Stop();
		}
	}
}
