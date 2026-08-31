package UnitTest.Zeze.Services;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import Zeze.Config;
import Zeze.Raft.LogSequence;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManager.BOfflineNotify;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Action1;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;

/**
 * S-1：延迟离线通知（Session.eOfflineNotifyDelay）的重连语义。
 * <p>
 * 机制：连接关闭时按 serverId 延迟 600 秒广播离线；期间同 serverId 的新 OfflineRegister
 * 会取消延迟通知（进程重启场景，接收端还有 loadSerialNo 防线）。
 * <ul>
 * <li>test1：断线重连（进程活着）后客户端重放 OfflineRegister，延迟通知必须被取消——
 *     否则 600 秒后活服务器被广播离线，且 flap 不改变 loadSerialNo，接收端防线失效，队列/定时器被误接管。</li>
 * <li>test2：关闭事件晚于重连完成（TCP 关闭检测可任意延迟）时，重放后的取消已发生、
 *     onClose 仍会重新调度延迟任务，触发前必须再检查同 serverId 的活会话。</li>
 * <li>test3：真离线（不重连）必须照常通知，防止修复过度抑制。</li>
 * </ul>
 * 测试不起定时器：直接反射调用 offlineNotify(true) 精确模拟延迟任务触发瞬间。
 */
@SuppressWarnings({"BusyWait", "SameParameterValue"})
@Fast
public class TestOfflineNotifyReconnect {
	private static final String autokeys = "autokeys_s1_offline_notify_test";
	private static ServiceManagerServer sm;
	private static int port;

	private static Field serverField;
	private static Field futuresField;
	private static Field serverIdField;
	private static Method offlineNotifyMethod;

	@BeforeAll
	public static void setUp() throws Exception {
		try (var probe = new ServerSocket(0)) {
			port = probe.getLocalPort();
		}
		LogSequence.deleteDirectory(new File(autokeys)); // Windows下启动前清理，保证全新状态
		Task.tryInitThreadPool();
		// 独立 Config（空配置）：避免读到测试目录 zeze.xml 里指向共享 SM(5001) 的 Connector。
		sm = new ServiceManagerServer(null, port, new Config(), autokeys);

		serverField = ServiceManagerServer.class.getDeclaredField("server");
		serverField.setAccessible(true);
		futuresField = ServiceManagerServer.class.getDeclaredField("offlineNotifyFutures");
		futuresField.setAccessible(true);
		serverIdField = ServiceManagerServer.Session.class.getDeclaredField("offlineRegisterServerId");
		serverIdField.setAccessible(true);
		offlineNotifyMethod = ServiceManagerServer.Session.class.getDeclaredMethod("offlineNotify", boolean.class);
		offlineNotifyMethod.setAccessible(true);
	}

	@AfterAll
	public static void tearDown() {
		if (sm != null) {
			sm.close();
			sm = null;
		}
		LogSequence.deleteDirectory(new File(autokeys));
	}

	// ---------------------------------------------------------------- helpers

	private static Agent newAgent(boolean autoReconnect) throws Exception {
		var agent = new Agent(new Config());
		agent.start();
		agent.getClient().connect("127.0.0.1", port, autoReconnect);
		return agent;
	}

	private static BOfflineNotify offlineRegister(Agent agent, int serverId, String notifyId,
												  Action1<BOfflineNotify> handle) {
		var arg = new BOfflineNotify();
		arg.serverId = serverId;
		arg.notifyId = notifyId;
		arg.notifySerialId = 1;
		agent.offlineRegister(() -> arg, handle);
		return arg;
	}

	/** 注册并等服务端登记完成（能按 serverId 找到会话），返回服务端 Session。 */
	private static Object registerOffline(Agent agent, int serverId, String notifyId,
										  Action1<BOfflineNotify> handle) throws Exception {
		offlineRegister(agent, serverId, notifyId, handle);
		var session = waitSession(serverId, 5000);
		Assertions.assertNotNull(session, "offline register not seen on server: serverId=" + serverId);
		return session;
	}

	/** 在服务端现存连接里按 offlineRegisterServerId 找 Session；deadline 内找不到返回 null。 */
	private static Object waitSession(int serverId, long timeoutMs) throws Exception {
		var netServer = (ServiceManagerServer.NetServer)serverField.get(sm);
		var found = new AtomicReference<>();
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			netServer.foreach(so -> {
				var session = so.getUserState();
				if (session instanceof ServiceManagerServer.Session
						&& serverIdField.getInt(session) == serverId)
					found.set(session);
			});
			if (found.get() != null)
				return found.get();
			Thread.sleep(50);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static boolean waitFutureScheduled(int serverId, long timeoutMs) throws Exception {
		var futures = (ConcurrentHashMap<Integer, Future<?>>)futuresField.get(sm);
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (futures.containsKey(serverId))
				return true;
			Thread.sleep(50);
		}
		return futures.containsKey(serverId);
	}

	private static boolean waitReconnected(Agent agent, long timeoutMs) throws Exception {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			if (agent.getClient().getSocket() != null)
				return true;
			Thread.sleep(50);
		}
		return agent.getClient().getSocket() != null;
	}

	/** 模拟延迟任务触发（跳过 600 秒等待）。 */
	@Test
	@Timeout(60)
	@Disabled
	public void test4_reconnectReplaysWithNewGenerationSerial() throws Exception {
		try (var receiver = newAgent(false)) {
			registerOffline(receiver, 8804, "UnitTest.S1.notify4", n -> {
			});

			try (var flapped = newAgent(true)) {
				// 工厂模拟应用的代际号：启动与每次重连各递增一次（真实实现见CsQueue/Timer的工厂闭包）
				var generation = new java.util.concurrent.atomic.AtomicLong();
				flapped.offlineRegister(() -> {
					var arg = new BOfflineNotify();
					arg.serverId = 7704;
					arg.notifyId = "UnitTest.S1.notify4";
					arg.notifySerialId = generation.incrementAndGet();
					return arg;
				}, n -> {
				});
				Assertions.assertEquals(1, generation.get());
				Assertions.assertNotNull(waitSession(7704, 5000), "offline register not seen on server");

				flapped.getClient().getSocket().close();
				Assertions.assertTrue(waitFutureScheduled(7704, 5000), "delayed notify not scheduled after close");
				Assertions.assertTrue(waitReconnected(flapped, 10_000), "no reconnect after flap");

				// 重连重放必须再次执行工厂（bump出新代际）并以新serial注册——
				// 在途的旧代际通知据此被接收端拒绝（残余窗口的关闭机制）。
				Assertions.assertTrue(waitSessionSerial(7704, "UnitTest.S1.notify4", 2, 5000),
						"reconnect replay must re-invoke factory and register bumped serial");
				Assertions.assertEquals(2, generation.get());
			}
		}
	}

	/** 等待服务端存在指定 serverId 的活会话，且其登记的指定 notifyId 的 serial 达到期望值。 */
	private static boolean waitSessionSerial(int serverId, String notifyId, long expectedSerial, long timeoutMs)
			throws Exception {
		var notifiesField = ServiceManagerServer.Session.class.getDeclaredField("offlineRegisterNotifies");
		notifiesField.setAccessible(true);
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			var session = waitSession(serverId, 200);
			if (session != null) {
				@SuppressWarnings("unchecked")
				var notifies = (java.util.HashMap<String, BOfflineNotify>)notifiesField.get(session);
				var arg = notifies.get(notifyId);
				if (arg != null && arg.notifySerialId == expectedSerial)
					return true;
			}
			Thread.sleep(50);
		}
		return false;
	}

	private static void fireDelayedNotify(Object session) throws Exception {
		offlineNotifyMethod.invoke(session, true);
	}

	private static void invokeOnClose(Object session) throws Exception {
		session.getClass().getMethod("onClose").invoke(session);
	}

	// ---------------------------------------------------------------- tests

	@Test
	@Timeout(60)
	public void test1_flapReconnectCancelsDelayedNotify() throws Exception {
		try (var receiver = newAgent(false)) {
			var notified = new TaskCompletionSource<BOfflineNotify>();
			registerOffline(receiver, 8801, "UnitTest.S1.notify1", notified::setResult);

			try (var flapped = newAgent(true)) {
				Object oldSession = registerOffline(flapped, 7701, "UnitTest.S1.notify1", n -> {
				});

				flapped.getClient().getSocket().close();
				Assertions.assertTrue(waitFutureScheduled(7701, 5000), "delayed notify not scheduled after close");
				Assertions.assertTrue(waitReconnected(flapped, 10_000), "no reconnect after flap");
				waitSession(7701, 5000); // 等重放登记（修复后出现；未修复时超时忽略，仍继续触发）

				fireDelayedNotify(oldSession);
				Assertions.assertFalse(notified.await(2000),
						"reconnected live server (serverId=7701) must not be notified offline");
			}
		}
	}

	@Test
	@Timeout(60)
	public void test2_lateCloseAfterReconnectSkipsNotify() throws Exception {
		try (var receiver = newAgent(false)) {
			var notified = new TaskCompletionSource<BOfflineNotify>();
			registerOffline(receiver, 8802, "UnitTest.S1.notify2", notified::setResult);

			try (var flapped = newAgent(true)) {
				Object oldSession = registerOffline(flapped, 7702, "UnitTest.S1.notify2", n -> {
				});

				flapped.getClient().getSocket().close();
				Assertions.assertTrue(waitReconnected(flapped, 10_000), "no reconnect after flap");
				waitSession(7702, 5000); // 等重放登记（修复后出现；未修复时超时忽略）

				// 模拟"关闭事件晚于重连完成"：真实 onClose 可能因 keepalive 超时才触发，
				// 此时重放的取消已过去，onClose 仍会重新调度延迟任务。
				invokeOnClose(oldSession);
				Assertions.assertTrue(waitFutureScheduled(7702, 5000), "late onClose must schedule delayed notify");
				fireDelayedNotify(oldSession);
				Assertions.assertFalse(notified.await(2000),
						"live session exists for serverId=7702; delayed notify must be suppressed");
			}
		}
	}

	@Test
	@Timeout(60)
	public void test3_trueOfflineStillNotifies() throws Exception {
		try (var receiver = newAgent(false)) {
			var notified = new TaskCompletionSource<BOfflineNotify>();
			registerOffline(receiver, 8803, "UnitTest.S1.notify3", notified::setResult);

			// 不自动重连：关闭即真离线
			try (var dead = newAgent(false)) {
				Object deadSession = registerOffline(dead, 7703, "UnitTest.S1.notify3", n -> {
				});

				dead.getClient().getSocket().close();
				Assertions.assertTrue(waitFutureScheduled(7703, 5000), "delayed notify not scheduled after close");
				fireDelayedNotify(deadSession);
				Assertions.assertTrue(notified.await(5000), "true offline (serverId=7703) must be notified");
				Assertions.assertEquals(7703, Objects.requireNonNull(notified.getNow()).serverId);
			}
		}
	}
}
