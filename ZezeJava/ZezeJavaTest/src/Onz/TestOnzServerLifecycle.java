package Onz;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;

import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Onz.OnzServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * FND3-53/FND3-54：OnzServer连接器注册表与生命周期所有权。
 * 53：getZezeInstance的"查缓存→建连接→顶掉stop旧"全程无同步——并发冷路径互相
 * stop对方的connector（GetReadySocket等待者收到异常，事务假性失败），重连窗口
 * 每个新请求都杀死上一个正在握手的尝试（churn）。
 * 54：stop()不停instances里的自动重连connector（停止后无限重连）、不停各SM代理
 * （线程泄漏）、cancel(false)不等在途redo轮次就关库；且不幂等、无stopped拒绝。
 */
public class TestOnzServerLifecycle {
	private final demo.App zeze2 = new demo.App();
	private OnzServer onzServer;

	@BeforeEach
	public void before() throws Exception {
		// 第二对服务 SM(5011)/Global(5012) 由 TestEnvLauncherListener 在进程内自动启动
		Assumptions.assumeTrue(harness.TestEnv.portReachable("127.0.0.1", 5011)
						&& harness.TestEnv.portReachable("127.0.0.1", 5012),
				"第二对服务(5011/5012)不可用：zeze.test.env=off 时 TestEnvLauncherListener 不在进程内自动启动");

		demo.App.Instance.Start();
		var config2 = Config.load("./zeze_cluster_2.xml");
		zeze2.Start(config2);

		var myConfig = Config.load("zeze.xml");
		onzServer = new OnzServer("zeze1=zeze.xml;zeze2=zeze_cluster_2.xml", myConfig);
		onzServer.start();
	}

	@AfterEach
	public void after() throws Exception {
		if (onzServer != null)
			onzServer.stop(); // 幂等：testStop已经stop过，这里验证重复stop不抛
		zeze2.Stop();
	}

	// 同 TestOnz.waitOnzReady：等 OnzServer 订阅发现两侧集群并建连。
	private void waitOnzReady() throws InterruptedException {
		var deadline = System.currentTimeMillis() + 60_000;
		for (;;) {
			try {
				onzServer.getZezeInstance("zeze1");
				onzServer.getZezeInstance("zeze2");
				return;
			} catch (RuntimeException e) {
				if (System.currentTimeMillis() > deadline)
					throw e;
				Thread.sleep(100);
			}
		}
	}

	// instances 是 OnzServer 的 private 字段，测试包不同，反射读取。
	@SuppressWarnings("unchecked")
	private static java.util.concurrent.ConcurrentHashMap<String, Connector> instances(OnzServer server)
			throws Exception {
		var field = OnzServer.class.getDeclaredField("instances");
		field.setAccessible(true);
		return (java.util.concurrent.ConcurrentHashMap<String, Connector>)field.get(server);
	}

	// ---------------------------------------------------------------
	// FND3-53: 并发冷路径必须只建一个connector，且互相不误杀等待者
	// ---------------------------------------------------------------

	@Test
	@Timeout(120)
	public void testConcurrentColdStartSingleConnector() throws Exception {
		// 先用zeze2确认订阅就绪；zeze1保持冷缓存（waitOnzReady会预热两者，不能用）。
		var deadline = System.currentTimeMillis() + 60_000;
		while (true) {
			try {
				onzServer.getZezeInstance("zeze2");
				break;
			} catch (RuntimeException e) {
				if (System.currentTimeMillis() > deadline)
					throw e;
				Thread.sleep(100);
			}
		}
		Assertions.assertFalse(instances(onzServer).containsKey("zeze1"), "zeze1必须保持冷缓存");

		final var threads = 8;
		var barrier = new CyclicBarrier(threads);
		var errors = new CopyOnWriteArrayList<Throwable>();
		var sockets = new CopyOnWriteArrayList<AsyncSocket>();
		var workers = new java.util.ArrayList<Thread>();
		for (var i = 0; i < threads; i++) {
			var t = new Thread(() -> {
				try {
					barrier.await();
					sockets.add(onzServer.getZezeInstance("zeze1"));
				} catch (Throwable e) {
					errors.add(e);
				}
			});
			workers.add(t);
			t.start();
		}
		for (var t : workers)
			t.join();

		Assertions.assertEquals(List.of(), errors, "并发获取连接不得有假性失败（被并发stop误杀）");
		Assertions.assertEquals(threads, sockets.size());
		// 单连接器证据：全部线程拿到同一个socket实例。
		Assertions.assertEquals(1, sockets.stream().distinct().count(),
				"并发冷路径必须只创建一个connector（FND3-53）");
		Assertions.assertNotNull(sockets.stream().findFirst().orElse(null), "必须拿到就绪socket");
	}

	// ---------------------------------------------------------------
	// FND3-54: stop幂等、清理connector、拒绝停机后的新请求
	// ---------------------------------------------------------------

	@Test
	@Timeout(60)
	public void testStopLifecycle() throws Exception {
		waitOnzReady();
		Assertions.assertFalse(instances(onzServer).isEmpty(), "预热后instances应有缓存connector");

		onzServer.stop();

		// 幂等：重复stop不抛（@AfterEach还会再stop一次）。
		Assertions.assertDoesNotThrow(onzServer::stop);

		// 停机后拒绝新请求。
		Assertions.assertThrows(RuntimeException.class, () -> onzServer.getZezeInstance("zeze1"),
				"stop后getZezeInstance必须拒绝");

		// 缓存connector已停止并清空（否则自动重连在停机后无限进行）。
		Assertions.assertTrue(instances(onzServer).isEmpty(), "stop必须清空instances");
	}
}
