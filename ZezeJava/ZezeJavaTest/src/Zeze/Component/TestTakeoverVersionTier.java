package Zeze.Component;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Services.ServiceManagerServer;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Takeover(offlinenotify) version 梯队通知专项：SM 在会话断开时按 version 梯队广播 Suspect——
 * ①首档只发 ==死者version；②档间超时（conf.takeoverNotifyStepMs，本测试反射调短）无回执升到
 * 更高存活version的最小值；③低于死者version的永不通知；④TakeoverDone回执即停。
 * 进程内SM（@Fast固定端口独占契约：本类26111，避开26110=TestTakeoverIdentifySuspect、
 * 26100-26102=TestMQ系）；Agent的version经Config.setAppVersion(v&lt;&lt;48)设置
 * （getAppMainVersion=appVersion&gt;&gt;&gt;48）。
 */
@Fast
public class TestTakeoverVersionTier {

	private static final int PORT = 26111;
	/** 档间超时(毫秒)：压测墙钟断言教训——太小会被负载尖峰穿透，800ms给首档派发留余量。 */
	private static final long STEP_MS = 800;

	// 版本档位：死者与同档=50，更高=70，更低=30（appVersion高16位）。
	private static final long V_LOW = 30, V_SAME = 50, V_HIGH = 70;
	// serverId：独立SM实例内可见（本类独占21-24，与TestTakeoverIdentifySuspect的11/12无交叉）。
	private static final int ID_DEAD = 21, ID_SAME = 22, ID_HIGH = 23, ID_LOW = 24;

	/** 用例1：首档只发==死者version；超时升档到更高version（收到时刻须在升档之后）；低version永不收到。 */
	@Test
	public void testTierOrderAndVersionFilter() throws Exception {
		Task.tryInitThreadPool();
		Files.createDirectories(Path.of("autokeys"));
		var sm = newServerWithStep();
		Agent same = null, higher = null, lower = null, dead = null;
		try {
			dead = newAgent(ID_DEAD, V_SAME);
			same = newAgent(ID_SAME, V_SAME);
			higher = newAgent(ID_HIGH, V_HIGH);
			lower = newAgent(ID_LOW, V_LOW);
			for (var a : new Agent[]{dead, same, higher, lower}) {
				a.start();
				waitReadyWithRetry(a);
			}
			waitIdentified(sm, ID_DEAD, V_SAME, ID_SAME, V_SAME, ID_HIGH, V_HIGH, ID_LOW, V_LOW);

			var gotSame = new LinkedBlockingQueue<Long>(); // 收到时刻(ms)
			var gotHigh = new LinkedBlockingQueue<Long>();
			same.setOnSuspect(__ -> gotSame.add(System.currentTimeMillis()));
			higher.setOnSuspect(__ -> gotHigh.add(System.currentTimeMillis()));
			lower.setOnSuspect(__ -> Assertions.fail("低于死者version的永不通知（需求③）"));

			var onCloseAt = System.currentTimeMillis();
			dead.stop();
			dead = null;

			// 首档：same收到（无时刻下界断言——满负载下慢派发合法）；higher/lower此刻未收
			Assertions.assertNotNull(gotSame.poll(30, TimeUnit.SECONDS), "==死者version的应收到首档Suspect");
			// 升档：higher收到且时刻在升档之后（首档没发它的带宽证据；预留20%派发余量防墙钟过敏）
			var t = gotHigh.poll(30, TimeUnit.SECONDS);
			Assertions.assertNotNull(t, "更高version应在档间超时后收到升档Suspect");
			Assertions.assertTrue(t - onCloseAt >= STEP_MS * 4 / 5,
					"更高version的首次收到应晚于档间超时（实际onClose后" + (t - onCloseAt) + "ms）");
			// 永不：lower无收（fail回调兜底）+ same不再收第二次（升档只发更高档）+ higher不收第二次（无第三档）
			Thread.sleep(STEP_MS * 3);
			Assertions.assertNull(gotSame.poll(), "同档不得因升档重复收到");
			Assertions.assertNull(gotHigh.poll(), "无更高档后不得继续");
		} finally {
			stopAll(dead, same, higher, lower);
			sm.close();
		}
	}

	/** 用例2：TakeoverDone回执即停——首档收到方回执后，更高version的永不收到（不升档）。 */
	@Test
	public void testTakeoverDoneStopsTier() throws Exception {
		Task.tryInitThreadPool();
		Files.createDirectories(Path.of("autokeys"));
		var sm = newServerWithStep();
		Agent same = null, higher = null, dead = null;
		try {
			dead = newAgent(ID_DEAD, V_SAME);
			same = newAgent(ID_SAME, V_SAME);
			higher = newAgent(ID_HIGH, V_HIGH);
			for (var a : new Agent[]{dead, same, higher}) {
				a.start();
				waitReadyWithRetry(a);
			}
			waitIdentified(sm, ID_DEAD, V_SAME, ID_SAME, V_SAME, ID_HIGH, V_HIGH);

			var gotSame = new LinkedBlockingQueue<Integer>();
			same.setOnSuspect(gotSame::add);
			higher.setOnSuspect(__ -> Assertions.fail("回执后不得升档（需求④）"));

			dead.stop();
			dead = null;
			Assertions.assertNotNull(gotSame.poll(30, TimeUnit.SECONDS), "首档应送达同version");
			same.sendTakeoverDone(ID_DEAD); // 模拟接管完成回执（真实链路=Takeover立墓碑后发送）

			Thread.sleep(STEP_MS * 3); // 足够经历≥2个档间超时点
			Assertions.assertNull(gotSame.poll(), "回执后不再有任何广播");
		} finally {
			stopAll(dead, same, higher);
			sm.close();
		}
	}

	/**
	 * 用例3：无更高档=放弃——死者v50、仅低version存活：首档无人可发，超时后没有更高档，
	 * 梯队状态清空（放弃路径执行而非挂起），低version永不收到。
	 */
	@Test
	public void testTierExhaustedGivesUp() throws Exception {
		Task.tryInitThreadPool();
		Files.createDirectories(Path.of("autokeys"));
		var sm = newServerWithStep();
		Agent lower = null, dead = null;
		try {
			dead = newAgent(ID_DEAD, V_SAME);
			lower = newAgent(ID_LOW, V_LOW);
			for (var a : new Agent[]{dead, lower}) {
				a.start();
				waitReadyWithRetry(a);
			}
			waitIdentified(sm, ID_DEAD, V_SAME, ID_LOW, V_LOW);

			lower.setOnSuspect(__ -> Assertions.fail("无同档/更高档候选时低version也永不通知"));
			dead.stop();
			dead = null;

			Thread.sleep(STEP_MS * 3);
			// 反射验证放弃路径执行：suspectTiers 已清空（timer已cancel，不挂900s级默认链）
			var tiersField = ServiceManagerServer.class.getDeclaredField("suspectTiers");
			tiersField.setAccessible(true);
			@SuppressWarnings("unchecked")
			var tiers = (java.util.concurrent.ConcurrentHashMap<Integer, ?>)tiersField.get(sm);
			Assertions.assertTrue(tiers.isEmpty(), "无更高档耗尽后梯队状态应清空（实际: " + tiers.keySet() + "）");
		} finally {
			stopAll(dead, lower);
			sm.close();
		}
	}

	// ===== 基建（对齐TestTakeoverIdentifySuspect先例） =====

	private static ServiceManagerServer newServerWithStep() throws Exception {
		Files.createDirectories(Path.of("autokeys"));
		var sm = new ServiceManagerServer(null, PORT, new Config(), "autokeys/takeover-tier");
		// 档间超时反射调短（Conf非构造参数）：默认900s是为了生产1.5×TakeoverTtl，测试要800ms节奏。
		var confField = ServiceManagerServer.class.getDeclaredField("conf");
		confField.setAccessible(true);
		var smConf = confField.get(sm);
		var stepField = smConf.getClass().getDeclaredField("takeoverNotifyStepMs");
		stepField.setAccessible(true);
		stepField.setLong(smConf, STEP_MS);
		return sm;
	}

	private static Agent newAgent(int serverId, long appMainVersion) throws Exception {
		var conf = new Config();
		conf.setServerId(serverId);
		conf.setAppVersion(appMainVersion << 48);
		var agent = new Agent(conf);
		agent.getClient().getConfig().addConnector(new Connector("127.0.0.1", PORT));
		return agent;
	}

	/** 确定性等Identify(serverId+version)在SM侧生效（同步点绑定被测前置状态，waitReady只等TCP）。 */
	private static void waitIdentified(ServiceManagerServer sm, Object... idVersionPairs) throws Exception {
		var serverField = ServiceManagerServer.class.getDeclaredField("server");
		serverField.setAccessible(true);
		var netServer = (Zeze.Net.Service)serverField.get(sm);
		var idField = ServiceManagerServer.Session.class.getDeclaredField("identifyServerId");
		idField.setAccessible(true);
		var verField = ServiceManagerServer.Session.class.getDeclaredField("identifyVersion");
		verField.setAccessible(true);
		var deadline = System.currentTimeMillis() + 10_000;
		while (true) {
			var seen = new boolean[idVersionPairs.length / 2];
			netServer.foreach(so -> {
				if (so.getUserState() instanceof ServiceManagerServer.Session session)
					for (int i = 0; i < idVersionPairs.length; i += 2)
						if ((int)idField.get(session) == (int)idVersionPairs[i]
								&& verField.getLong(session) == (long)idVersionPairs[i + 1])
							seen[i / 2] = true;
			});
			var all = true;
			for (var b : seen)
				all &= b;
			if (all)
				return;
			Assertions.assertTrue(System.currentTimeMillis() < deadline, "10s内Identify(serverId+version)未全部生效");
			//noinspection BusyWait
			Thread.sleep(10);
		}
	}

	// WaitReady固定5s预算在满负载下不足（同TestTakeoverIdentifySuspect判例）：有界重试。
	private static void waitReadyWithRetry(Agent agent) throws Exception {
		for (int attempt = 1; ; ++attempt) {
			try {
				agent.waitReady();
				return;
			} catch (Exception e) {
				if (!(e instanceof java.util.concurrent.TimeoutException) || attempt >= 6)
					throw e;
				//noinspection BusyWait
				Thread.sleep(200);
			}
		}
	}

	private static void stopAll(@Nullable Agent... agents) {
		for (var a : agents) {
			if (a == null)
				continue;
			try {
				a.stop();
			} catch (Exception ignored) {
				// best-effort清理
			}
		}
	}
}
