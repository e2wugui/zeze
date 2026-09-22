package UnitTest.Zeze.Services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Services.GlobalCacheManagerAsyncServer;
import Zeze.Services.GlobalCacheManager.Acquire;
import Zeze.Services.GlobalCacheManager.KeepAlive;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import harness.Fast;

/**
 * 异步GCM的stop停机钉板（对齐同步版TestFnd718GcmStopWaitsDaemon的构造手法）：
 * stop()必须在拆依赖前限时等待在飞守护扫描结束——扫描被阻塞（测试持有会话锁）期间
 * stop不得完成（缺等待形态毫秒级完成，断言即红）；放行后扫描完整跑完本轮
 * （A、B都被kick清零sessionId），stop才返回。
 * <p>
 * 注意基线演变：本类不再以"kick踩null server的NPE"为红色信号——server引用已墓碑化
 * （stop只停止对象不杀引用），晚到扫描读到的已停止Service上GetSocket安全返回null。
 * 红色信号是"stop未等在飞扫描"本身。
 */
@Fast
public class TestGlobalCacheManagerAsyncStopWaitsDaemon {
	private static final int PORT = 19719; // @Fast固定端口独占
	private static final int SERVER_ID_A = 9501;
	private static final int SERVER_ID_B = 9502;

	private static RawClient clientA;
	private static RawClient clientB;
	private static GlobalCacheManagerAsyncServer gcm;
	private static ExecutorService stopExecutor;

	/** 裸协议客户端：只需Login（kick由服务端单向发起）。 */
	private static final class RawClient extends Service {
		RawClient(String name) {
			super(name, new Zeze.Config());
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(KeepAlive.TypeId_, new ProtocolFactoryHandle<>(
					KeepAlive::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(Acquire.TypeId_, new ProtocolFactoryHandle<>(
					Acquire::new, null, TransactionLevel.None, DispatchMode.Direct));
		}

		AsyncSocket connect() throws Exception {
			var connector = new Connector("127.0.0.1", PORT, false);
			getConfig().addConnector(connector);
			start();
			return connector.WaitReady();
		}
	}

	@BeforeAll
	public static void setUp() throws Exception {
		Task.tryInitThreadPool();
		// 自建实例自起自停（对齐TestGlobalCacheManagerAsyncAcquireKick）：固定端口独占，
		// 不依赖TestEnvLauncherListener的环境（其在test任务被关闭）。
		gcm = new GlobalCacheManagerAsyncServer();
		gcm.start(null, PORT, null);
		stopExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "UnitTest.AsyncGcmStop.stop"));
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (stopExecutor != null)
			stopExecutor.shutdownNow();
		for (var c : new RawClient[]{clientA, clientB})
			if (c != null)
				c.stop();
		if (gcm != null)
			gcm.stop();
	}

	private static ReentrantLock sessionHolder(int serverId) throws Exception {
		var sessionsField = GlobalCacheManagerAsyncServer.class.getDeclaredField("sessions");
		sessionsField.setAccessible(true);
		@SuppressWarnings("unchecked")
		var sessions = (LongConcurrentHashMap<ReentrantLock>)sessionsField.get(gcm);
		var holder = sessions.get(serverId);
		Assertions.assertNotNull(holder, "session must exist, serverId=" + serverId);
		return holder;
	}

	private static void staleSession(ReentrantLock holder) throws Exception {
		var setActiveTime = holder.getClass().getDeclaredMethod("setActiveTime", long.class);
		setActiveTime.setAccessible(true);
		// 远早于globalDaemonTimeout（默认约75s），守护tick必然判定超时
		setActiveTime.invoke(holder, System.currentTimeMillis() - 10_000_000L);
	}

	private static long holderSessionId(ReentrantLock holder) throws Exception {
		var field = holder.getClass().getDeclaredField("sessionId");
		field.setAccessible(true);
		return field.getLong(holder);
	}

	private static Service serverService() throws Exception {
		var field = GlobalCacheManagerAsyncServer.class.getDeclaredField("server");
		field.setAccessible(true);
		return (Service)field.get(gcm);
	}

	private interface Cond {
		boolean get() throws Exception;
	}

	private static boolean waitUntil(Cond condition, long timeoutMs) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (condition.get())
				return true;
			//noinspection BusyWait
			Thread.sleep(20);
		}
		return condition.get();
	}

	private static AsyncSocket login(RawClient client, int serverId) throws Exception {
		var socket = client.connect();
		var login = new Login();
		login.Argument.serverId = serverId;
		login.Argument.globalCacheManagerHashIndex = 0;
		Assertions.assertTrue(login.SendForWait(socket, 10_000).await(10_000), "login await, serverId=" + serverId);
		Assertions.assertFalse(login.isTimeout(), "login timeout");
		Assertions.assertEquals(0, login.getResultCode(), "login resultCode");
		return socket;
	}

	@Test
	@Timeout(90)
	public void testStopWaitsInFlightDaemonAndKicksAllStaleSessions() throws Exception {
		clientA = new RawClient("UnitTest.AsyncGcmStop.A");
		clientB = new RawClient("UnitTest.AsyncGcmStop.B");
		login(clientA, SERVER_ID_A);
		login(clientB, SERVER_ID_B);

		var holderA = sessionHolder(SERVER_ID_A);
		var holderB = sessionHolder(SERVER_ID_B);
		Assertions.assertNotEquals(0L, holderSessionId(holderA), "A登录后必须绑定socket");
		Assertions.assertNotEquals(0L, holderSessionId(holderB), "B登录后必须绑定socket");

		// 1. 两个超时会话；测试占住两把会话锁，守护扫描到来后阻塞在第一把上（本轮检查未完成）
		staleSession(holderA);
		staleSession(holderB);
		holderA.lock();
		holderB.lock();
		try {
			var scanArrived = waitUntil(holderA::hasQueuedThreads, 12_000)
					|| waitUntil(holderB::hasQueuedThreads, 1_000);
			Assertions.assertTrue(scanArrived, "守护tick必须在12s内驱动扫描（周期5s）");

			// 2. 清空userState并关闭两个服务端socket：stop()的server.stop()关socket时同步回调
			//    OnSocketClose→tryUnBindSocket会抢会话锁（掩盖缺陷），清掉后stop路径不再触碰
			//    会话锁；holder.sessionId保持非零（未解绑），kick路径仍会查询server.GetSocket。
			var server = serverService();
			for (var holder : new ReentrantLock[]{holderA, holderB}) {
				var peer = server.GetSocket(holderSessionId(holder));
				Assertions.assertNotNull(peer, "会话socket必须仍在服务端socket表");
				peer.setUserState(null);
				peer.close();
			}

			// 3. stop()不得在扫描被阻塞期间完成：缺等待形态下stop立即返回并拆除依赖
			Future<?> stopFuture = stopExecutor.submit(() -> {
				gcm.stop();
				return null;
			});
			try {
				stopFuture.get(1_500, TimeUnit.MILLISECONDS);
				Assertions.fail("stop()必须在在飞守护扫描结束前等待（缺等待形态立即完成并拆依赖）");
			} catch (java.util.concurrent.TimeoutException expected) {
				// 期望：stop仍在等待在飞扫描
			}

			// 4. 放行会话锁：扫描在拆依赖前完整跑完本轮，A、B都被kick（sessionId清零）
			holderA.unlock();
			holderB.unlock();
			stopFuture.get(30_000, TimeUnit.MILLISECONDS); // stop完成（扫描结束后才拆依赖）

			Assertions.assertTrue(waitUntil(() -> holderSessionId(holderA) == 0L, 10_000),
					"扫描必须在拆依赖前kick A（sessionId清零）");
			Assertions.assertTrue(waitUntil(() -> holderSessionId(holderB) == 0L, 10_000),
					"扫描必须完整跑完本轮（A之后B也被检查）");
		} finally {
			if (holderA.isHeldByCurrentThread())
				holderA.unlock();
			if (holderB.isHeldByCurrentThread())
				holderB.unlock();
		}
	}
}
