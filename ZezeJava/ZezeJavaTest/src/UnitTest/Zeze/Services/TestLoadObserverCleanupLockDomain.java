package UnitTest.Zeze.Services;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Config;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BServiceInfo;
import Zeze.Services.ServiceManager.BSubscribeInfo;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-30回归：观察者行回收（onClose→removeLoadObservers）必须与登记
 * （addLoadObserver，全部在editLock内的调用点）同处一个锁域——原先清理在锁外，
 * "removeObserver判空→it.remove()整行删除"与并发登记构成TOCTOU，后到的活观察者
 * 随地址行被误删。
 * 确定性交错：测试线程持有editLock期间关闭订阅者会话——摘除（socketMap.remove）
 * 前置于清理发生，而清理必须阻塞在editLock上；解锁后观察者行才被回收。
 * 若清理移回锁外（回归），持锁期间行即消失，断言失败。
 */
@Fast
public class TestLoadObserverCleanupLockDomain {

	@Test
	public void testObserverRowSurvivesWhileEditLockHeld() throws Exception {
		Task.tryInitThreadPool();
		// 固定端口契约与选段说明见TestTakeoverIdentifySuspect；26112避开26110/26111。
		final int port = 26112;
		final int passivePort = 26222;
		final String rowKey = "127.0.0.1_" + passivePort;
		Files.createDirectories(Path.of("autokeys"));

		var sm = new ServiceManagerServer(null, port, new Config(), "autokeys/fnd5-30");
		Agent subscriber = null;
		Agent provider = null;
		try {
			// 先连订阅者（此刻唯一连接，可确定其服务端会话），再连提供者。
			subscriber = newAgent(port);
			subscriber.start();
			subscriber.waitReady();
			var server = (Service)field(sm, "server");
			var subscriberSide = server.GetSocket();
			Assertions.assertNotNull(subscriberSide, "订阅者应已建立服务端会话");
			var subscriberSid = subscriberSide.getSessionId();

			provider = newAgent(port);
			provider.start();
			provider.waitReady();

			// 提供者注册（passive地址非空）→ 订阅者订阅 → 服务端把订阅者登记为该地址的观察者。
			provider.registerService(new BServiceInfo("Fnd5LoadProvider", "1", 0, "127.0.0.1", passivePort));
			subscriber.subscribeService(new BSubscribeInfo("Fnd5LoadProvider"));

			@SuppressWarnings("unchecked")
			var loads = (ConcurrentHashMap<String, ?>)field(sm, "loads");
			Assertions.assertTrue(loads.containsKey(rowKey), "订阅应已登记负载观察者地址行");

			// 持editLock期间关闭订阅者会话：摘除前置于清理（FND5-29顺序），清理阻塞在editLock上。
			// close必须在独立线程：AsyncSocket.close可能在调用线程同步执行OnSocketClose，
			// 而editLock是可重入锁——测试线程调用会重入执行清理，测不出阻塞。
			var editLock = (ReentrantLock)field(sm, "editLock");
			editLock.lock();
			try {
				var closer = new Thread(subscriberSide::close, "test-close-subscriber");
				closer.start();
				Assertions.assertTrue(waitUntil(() -> server.GetSocket(subscriberSid) == null),
						"socketMap应已摘除该会话（摘除前置于清理）");
				// 给“锁外清理”的回归形态留足时间：持锁期间地址行必须仍在。
				Thread.sleep(300);
				Assertions.assertTrue(loads.containsKey(rowKey),
						"editLock持有期间观察者行必须不被清理（清理与登记同锁域，FND5-30）");
			} finally {
				editLock.unlock();
			}

			// 解锁后清理完成：观察者行随之回收（行内唯一观察者已死）。
			Assertions.assertTrue(waitUntil(() -> !loads.containsKey(rowKey)),
					"解锁后会话清理应回收观察者地址行");
		} finally {
			if (subscriber != null)
				subscriber.stop();
			if (provider != null)
				provider.stop();
			sm.close();
		}
	}

	private static Agent newAgent(int port) throws Exception {
		var agent = new Agent(new Config());
		agent.getClient().getConfig().addConnector(new Connector("127.0.0.1", port));
		return agent;
	}

	private static Object field(Object obj, String name) throws Exception {
		Field f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static boolean waitUntil(java.util.function.BooleanSupplier cond) throws InterruptedException {
		for (var i = 0; i < 250 && !cond.getAsBoolean(); i++)
			Thread.sleep(20);
		return cond.getAsBoolean();
	}
}
