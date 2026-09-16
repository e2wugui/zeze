package UnitTest.Zeze.Services;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Services.GlobalCacheManagerServer;
import Zeze.Services.GlobalCacheManager.KeepAlive;
import Zeze.Services.GlobalCacheManager.Login;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.LongConcurrentHashMap;
import Zeze.Util.Task;
import harness.Fast;

/**
 * FND7-17：同步GCM守护任务与Rpc超时定时器共用共享调度池。扫描体（kick+同步release，
 * noWait=false）在pending上可无限期await，解锁依赖的Rpc超时定时器又排在同一个
 * scheduledPool上——单线程/饱和调度池下形成同池循环等待，锁协调者停摆。
 * <p>
 * 修复后定时器tick只做派发，扫描体在默认worker池上执行。验证（会话锁汇合点，时序全由
 * 测试控制，不依赖线程身份枚举）：
 * <ol>
 * <li>真实定时器tick仍能端到端驱动扫描（守住周期与kick语义不变）；</li>
 * <li>tick入口（scheduleAchillesHeelDaemon）在扫描被阻塞（测试持有会话锁）时必须立刻
 * 返回——内联执行（缺陷形态）会在被阻塞的扫描上无限期等待。修复前扫描直接内联在定时器
 * lambda里（方法不存在，反射调用以NoSuchMethodException失败），同样是红。</li>
 * </ol>
 */
@Fast
public class TestFnd717GcmDaemonOffScheduler {
	private static final int PORT = 19717; // @Fast固定端口独占
	private static final int SERVER_ID_A = 9301;

	private static RawClient clientA;
	private static GlobalCacheManagerServer gcm;

	private interface Cond {
		boolean get() throws Exception;
	}

	/** 裸协议客户端：只需Login（守护对超时会话的kick由服务端单向发起，无需客户端应答）。 */
	private static final class RawClient extends Service {
		RawClient(String name) {
			super(name, new Zeze.Config());
			AddFactoryHandle(Login.TypeId_, new ProtocolFactoryHandle<>(
					Login::new, null, TransactionLevel.None, DispatchMode.Direct));
			AddFactoryHandle(KeepAlive.TypeId_, new ProtocolFactoryHandle<>(
					KeepAlive::new, null, TransactionLevel.None, DispatchMode.Direct));
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
		// 同步版是进程内单例；fast套件无其他使用者（TestEnvLauncherListener在test任务被关闭，
		// 且只启动异步版），自起自停保持端口独占。
		gcm = GlobalCacheManagerServer.getInstance();
		gcm.start(null, PORT, null);
	}

	@AfterAll
	public static void tearDown() throws Exception {
		if (clientA != null)
			clientA.stop();
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

	private static boolean scanRunning() throws Exception {
		var field = GlobalCacheManagerServer.class.getDeclaredField("achillesHeelRunning");
		field.setAccessible(true);
		return ((AtomicBoolean)field.get(gcm)).get();
	}

	private static long holderSessionId(ReentrantLock holder) throws Exception {
		var field = holder.getClass().getDeclaredField("sessionId");
		field.setAccessible(true);
		return field.getLong(holder);
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

	@Test
	@Timeout(90)
	public void testDaemonScanRunsOffSchedulerThread() throws Exception {
		clientA = new RawClient("UnitTest.FND7_17.A");
		var socketA = clientA.connect();
		var login = new Login();
		login.Argument.serverId = SERVER_ID_A;
		login.Argument.globalCacheManagerHashIndex = 0;
		Assertions.assertTrue(login.SendForWait(socketA, 10_000).await(10_000), "login await");
		Assertions.assertFalse(login.isTimeout(), "login timeout");
		Assertions.assertEquals(0, login.getResultCode(), "login resultCode");

		var holder = sessionHolder(SERVER_ID_A);
		Assertions.assertNotEquals(0L, holderSessionId(holder), "登录后会话必须绑定socket");

		// 1. 守住真实定时器接线：置为超时会话并占住会话锁，tick驱动的扫描必须到来排队
		staleSession(holder);
		holder.lock();
		try {
			Assertions.assertTrue(waitUntil(holder::hasQueuedThreads, 12_000),
					"真实tick必须在12s内派发扫描（周期5s）");
			Assertions.assertTrue(scanRunning(), "扫描在飞标志必须置位");
		} finally {
			holder.unlock();
		}

		// 2. 放行扫描，等本轮结束（kick会断开A的连接并清零sessionId）
		Assertions.assertTrue(waitUntil(() -> !scanRunning(), 10_000), "扫描必须结束");
		Assertions.assertTrue(waitUntil(() -> holderSessionId(holder) == 0L, 10_000), "kick必须清零sessionId");

		// 3. 核心断言：tick入口只派发不内联——扫描被阻塞时调用必须立刻返回。
		// 修复前扫描内联在定时器lambda里（无scheduleAchillesHeelDaemon，反射即红）；
		// 内联形态下本调用会在被阻塞的扫描上无限期等待，2s限时必然超红。
		staleSession(holder);
		holder.lock();
		try {
			var scheduleTick = GlobalCacheManagerServer.class.getDeclaredMethod("scheduleAchillesHeelDaemon");
			scheduleTick.setAccessible(true);
			var begin = System.nanoTime();
			scheduleTick.invoke(gcm);
			var elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
			Assertions.assertTrue(elapsedMs < 2_000, "tick入口必须立即返回（只派发），实际耗时=" + elapsedMs + "ms");

			// 派发出去的扫描必须真实运行并到达会话锁（证明不是被丢弃）
			Assertions.assertTrue(waitUntil(holder::hasQueuedThreads, 5_000), "派发的扫描必须到达会话锁");
		} finally {
			holder.unlock();
		}

		// 4. 放行后扫描完成kick，会话状态被清理
		Assertions.assertTrue(waitUntil(() -> !scanRunning(), 10_000), "派发的扫描必须结束");
		Assertions.assertEquals(0L, holderSessionId(holder), "kick必须清零sessionId");
	}
}
