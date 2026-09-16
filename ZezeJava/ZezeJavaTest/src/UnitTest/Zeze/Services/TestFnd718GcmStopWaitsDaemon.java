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
import org.junit.jupiter.api.parallel.ResourceLock;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Services.GlobalCacheManagerServer;
import Zeze.Services.GlobalCacheManager.Acquire;
import Zeze.Services.GlobalCacheManager.KeepAlive;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND7-18：stop()对守护定时器cancel(false)后立刻拆依赖（置server=null）——若cancel不等
 * 在飞扫描，扫描的kick()读到null的server踩NPE，异常逃出sessions.forEach中断本轮检查
 * （剩余会话不再被kick/回收）。
 * <p>
 * 注意基线：原版（扫描内联在tick任务体内）不受此害——TimerFuture.cancel与周期任务体
 * 持同一把锁（Task.schedulePeriodCore在body执行期间持future锁），cancel天然join在飞
 * 扫描。FND7-17把扫描派发到worker池后，TimerFuture锁只包住派发动作，cancel不再覆盖
 * 在飞扫描，窗口重新打开——本测试钉死stop必须显式等在飞扫描结束再拆依赖。
 * <p>
 * 构造性场景（同步GCM单例，两个超时会话；时序由测试持有的会话锁控制）：
 * 1. A、B登录并置为超时会话；测试先占住两会话锁，等守护扫描到来阻塞在第一把锁上；
 * 2. 清空两个服务端socket的userState并关闭之——stop()的server.stop()关闭会话socket时
 *    会同步回调OnSocketClose→tryUnBindSocket抢会话锁（那会让stop自己阻塞、掩盖缺陷），
 *    清空userState后stop路径不再触碰会话锁，可自由跑到server=null；
 * 3. 后台stop()：缺等待的形态毫秒级完成（cancel只挡住了派发）——断言1红；
 * 4. 放行会话锁：修复后扫描在拆依赖前完整跑完（A、B的sessionId都被kick清零）；
 *    缺等待的形态下扫描恢复后kick踩null server的NPE，forEach中断，sessionId保持非零。
 */
@ResourceLock("GlobalCacheManagerServer.instance")
@Fast
public class TestFnd718GcmStopWaitsDaemon {
	private static final int PORT = 19718; // @Fast固定端口独占（与TestFnd717经ResourceLock串行）
	private static final int SERVER_ID_A = 9401;
	private static final int SERVER_ID_B = 9402;

	private static RawClient clientA;
	private static RawClient clientB;
	private static GlobalCacheManagerServer gcm;
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
		// 同步版是进程内单例；与TestFnd717GcmDaemonOffScheduler经@ResourceLock串行使用，
		// fast套件无其他使用者（TestEnvLauncherListener在test任务被关闭且只启动异步版）。
		gcm = GlobalCacheManagerServer.getInstance();
		gcm.start(null, PORT, null);
		stopExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "UnitTest.FND7_18.stop"));
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
		var sessionsField = GlobalCacheManagerServer.class.getDeclaredField("sessions");
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
		var field = GlobalCacheManagerServer.class.getDeclaredField("server");
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
		clientA = new RawClient("UnitTest.FND7_18.A");
		clientB = new RawClient("UnitTest.FND7_18.B");
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

			// 3. stop()不得在扫描被阻塞期间完成：缺等待的形态下cancel(false)只挡住派发动作，
			//    随后立即拆依赖（server=null）
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
			stopFuture.get(30_000, TimeUnit.MILLISECONDS); // stop完成（扫描结束后拆依赖）

			Assertions.assertTrue(waitUntil(() -> holderSessionId(holderA) == 0L, 10_000),
					"扫描必须在拆依赖前kick A（缺等待形态下kick踩null server的NPE，sessionId不清零）");
			Assertions.assertTrue(waitUntil(() -> holderSessionId(holderB) == 0L, 10_000),
					"扫描必须完整跑完本轮（缺等待形态下A的NPE中断forEach，B不再检查）");
		} finally {
			if (holderA.isHeldByCurrentThread())
				holderA.unlock();
			if (holderB.isHeldByCurrentThread())
				holderB.unlock();
		}
	}
}
