package UnitTest.Zeze.Component;

import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Builtin.Timer.BIndex;
import Zeze.Builtin.Timer.BNode;
import Zeze.Builtin.Timer.BNodeRoot;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Builtin.Timer.BTimer;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Config;
import Zeze.Net.Connector;
import Zeze.Services.ServiceManager.Agent;
import Zeze.Transaction.TableX;
import Zeze.Util.TaskSpec;
import demo.App;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Timer接管端到端回归（demo.App真实环境）：伪造死者serverId=777的定时器链+过期租约，
 * tryTransfer（Suspect提示同款入口）单事务内【搬运+立墓碑】，afterTransfer对搬来的链
 * 重调度本地——终极验证：被接管的定时器在本进程真正触发。
 * <p>
 * 环境直接使用 demo.App.Start()（serverId=0，Memory库，Takeover默认on，
 * Timer由ProviderApp.startLast启动），不使用TakeoverTestEnv编程式App。
 * 无@Fast：依赖SM/Global环境（integrationTest由TestEnvLauncherListener自动提供）。
 */
public class TestTakeoverTimerDemoApp {

	// 死者serverId沿用777（避开真实id与TakeoverTestEnv分配的100+段）。
	private static final int deadServerId = 777;
	private static final long nodeId = 777_777L;
	private static final String timerId = "@UnitTest.Zeze.Component.TestTakeoverTimerDemoApp.deadSimple";
	private static final AtomicInteger FIRED = new AtomicInteger();

	// Suspect广播端到端用例的死者（独立id/链/计数，避免与直调用例相互干扰）。
	private static final int suspectDeadId = 778;
	private static final long suspectNodeId = 778_778L;
	private static final String suspectTimerId = "@UnitTest.Zeze.Component.TestTakeoverTimerDemoApp.suspectDeadSimple";
	private static final AtomicInteger SUSPECT_FIRED = new AtomicInteger();

	public static class DeadTimerHandle implements TimerHandle {
		public DeadTimerHandle() {
		}

		@Override
		public void onTimer(@NotNull TimerContext context) {
			FIRED.incrementAndGet();
		}
	}

	public static class SuspectDeadTimerHandle implements TimerHandle {
		public SuspectDeadTimerHandle() {
		}

		@Override
		public void onTimer(@NotNull TimerContext context) {
			SUSPECT_FIRED.incrementAndGet();
		}
	}

	@BeforeEach
	public void setUp() throws Exception {
		App.Instance.Start();
	}

	@Test
	@Timeout(60)
	public void testTimerTakeoverFires() throws Exception {
		var app = App.Instance.Zeze;
		Assertions.assertNotNull(app.getTimer(), "demo.App的Timer必须已由ProviderApp.startLast启动");
		Assertions.assertNotEquals("off", app.getTakeover().getMode(), "demo.App的Takeover必须启用（默认on）");

		// 伪造死者777的单节点循环链：1个simple timer，1.5秒后触发（接管后应在本进程fire）。
		var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
			var root = tNodeRoot(app).getOrAdd(deadServerId);
			root.setHeadNodeId(nodeId);
			root.setTailNodeId(nodeId);
			root.setVersion(0);
			root.setLoadSerialNo(5); // 死者root stamp=死者epoch
			var node = tNodes(app).getOrAdd(nodeId);
			node.setPrevNodeId(nodeId); // 循环链
			node.setNextNodeId(nodeId);
			var simple = new BSimpleTimer();
			simple.setNextExpectedTime(System.currentTimeMillis() + 1_500);
			var bTimer = new BTimer(timerId, DeadTimerHandle.class.getName(), 0);
			bTimer.setTimerObj(simple);
			node.getTimers().put(timerId, bTimer);
			tIndexs(app).insert(timerId, new BIndex(deadServerId, nodeId, 1, 0));
			return 0L;
		}, "TestTakeoverTimerDemoApp.forgeDead")).call();
		Assertions.assertEquals(0L, rc);

		// 过期租约（epoch=5与root stamp对齐）→ 直接tryTransfer，等OneByOne队列排空。
		TakeoverTestEnv.forgeLease(app, deadServerId, 5, System.currentTimeMillis() - 1_000);
		app.getTakeover().tryTransfer(deadServerId);
		TakeoverTestEnv.waitTryTransferQueue();

		// 1) 接管裁决成功：租约立墓碑。
		var lease = TakeoverTestEnv.readLease(app, deadServerId);
		Assertions.assertEquals(0L, lease[1], "Timer接管成功后租约应立墓碑");

		// 2) 结构校验：链拼到server 0的root、死者root清空+墓碑stamp、index重指向接管者。
		var rcAssert = TaskSpec.ofProcedure(app.newProcedure(() -> {
			var dead = tNodeRoot(app).get(deadServerId);
			Assertions.assertNotNull(dead);
			Assertions.assertEquals(0L, dead.getHeadNodeId(), "死者链头应清零");
			Assertions.assertEquals(0L, dead.getLoadSerialNo(), "死者root应立墓碑stamp=0");
			var mine = tNodeRoot(app).getOrAdd(app.getConfig().getServerId());
			Assertions.assertEquals(nodeId, mine.getHeadNodeId(), "接管链应拼到server 0的root");
			var index = tIndexs(app).get(timerId);
			Assertions.assertNotNull(index);
			Assertions.assertEquals(app.getConfig().getServerId(), index.getServerId(),
					"afterTransfer重载应把index重指向接管者");
			return 0L;
		}, "TestTakeoverTimerDemoApp.assertMoved")).call();
		Assertions.assertEquals(0L, rcAssert);

		// 3) 终极验证：afterTransfer已把搬来的链重调度本地，定时器在本进程真正触发。
		long deadline = System.currentTimeMillis() + 10_000;
		while (FIRED.get() == 0 && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
		Assertions.assertTrue(FIRED.get() >= 1, "被接管的定时器必须在本进程触发（nextExpectedTime=伪造时刻+1.5s）");
	}

	/**
	 * 端到端：SM真实广播Suspect → onSuspect接线 → tryTransfer → 搬运+重调度 → 定时器fire。
	 * <p>
	 * 死者的SM会话存在感用一个轻量Agent（Identify上报serverId=778）模拟；demo.App侧伪造
	 * 778的定时器链+过期租约后杀掉该Agent连接（异常下线），SM onClose立即广播Suspect(778)
	 * （新机制无广播延迟，不存在可配置的延迟参数）。本测试不直调tryTransfer——Suspect链路
	 * 是唯一触发源：若广播/接收/接线任一环断裂，租约永不被立碑，测试失败。
	 * Suspect→tryTransfer时租约已是过期态，无需等待TTL，全程约2秒。
	 */
	@Test
	@Timeout(60)
	public void testTimerTakeoverViaSuspectBroadcast() throws Exception {
		var app = App.Instance.Zeze;
		Assertions.assertNotNull(app.getTimer());

		// 1. 死者的SM会话：轻量Agent连接demo.App所连的同一个SM（默认127.0.0.1:5001，
		//    integrationTest由TestEnvLauncherListener提供），Identify上报serverId=778。
		var agentConf = new Config();
		agentConf.setServerId(suspectDeadId);
		var deadAgent = new Agent(agentConf);
		deadAgent.getClient().getConfig().addConnector(
				new Connector("127.0.0.1", harness.TestEnvLauncherListener.SERVICE_MANAGER_PORT));
		deadAgent.start();
		deadAgent.waitReady();
		try {
			// 2. 伪造死者778的单节点链：1个simple timer，1.5秒后触发 + 过期租约。
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				var root = tNodeRoot(app).getOrAdd(suspectDeadId);
				root.setHeadNodeId(suspectNodeId);
				root.setTailNodeId(suspectNodeId);
				root.setVersion(0);
				root.setLoadSerialNo(6);
				var node = tNodes(app).getOrAdd(suspectNodeId);
				node.setPrevNodeId(suspectNodeId);
				node.setNextNodeId(suspectNodeId);
				var simple = new BSimpleTimer();
				simple.setNextExpectedTime(System.currentTimeMillis() + 1_500);
				var bTimer = new BTimer(suspectTimerId, SuspectDeadTimerHandle.class.getName(), 0);
				bTimer.setTimerObj(simple);
				node.getTimers().put(suspectTimerId, bTimer);
				tIndexs(app).insert(suspectTimerId, new BIndex(suspectDeadId, suspectNodeId, 1, 0));
				return 0L;
			}, "TestTakeoverTimerDemoApp.forgeSuspectDead")).call();
			Assertions.assertEquals(0L, rc);
			TakeoverTestEnv.forgeLease(app, suspectDeadId, 6, System.currentTimeMillis() - 1_000);

			Thread.sleep(300); // 等Identify在SM侧生效（会话上记录serverId=778）。

			// 3. 异常下线：杀连接（不发任何善后），SM onClose → 立即广播Suspect(778)。
		} finally {
			deadAgent.stop();
		}

		// 4. Suspect → demo.App onSuspect → tryTransfer：等接管完成（租约立碑即裁决成功；
		//    本测试无其他触发源：未直调tryTransfer，扫描walk也看不见未checkpoint的伪造行）。
		long deadline = System.currentTimeMillis() + 10_000;
		while (TakeoverTestEnv.readLease(app, suspectDeadId)[1] != 0 && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
		Assertions.assertEquals(0L, TakeoverTestEnv.readLease(app, suspectDeadId)[1],
				"SM广播Suspect后应完成接管并立碑（链路唯一触发源，失败=广播/接收/接线断裂）");

		// 5. 结构校验 + 定时器在本进程真正触发。
		var rcAssert = TaskSpec.ofProcedure(app.newProcedure(() -> {
			var dead = tNodeRoot(app).get(suspectDeadId);
			Assertions.assertNotNull(dead);
			Assertions.assertEquals(0L, dead.getHeadNodeId(), "死者链头应清零");
			Assertions.assertEquals(0L, dead.getLoadSerialNo(), "死者root应立墓碑stamp=0");
			var mine = tNodeRoot(app).getOrAdd(app.getConfig().getServerId());
			Assertions.assertEquals(suspectNodeId, mine.getHeadNodeId(), "接管链应拼到server 0的root");
			var index = tIndexs(app).get(suspectTimerId);
			Assertions.assertNotNull(index);
			Assertions.assertEquals(app.getConfig().getServerId(), index.getServerId(),
					"afterTransfer重载应把index重指向接管者");
			return 0L;
		}, "TestTakeoverTimerDemoApp.assertSuspectMoved")).call();
		Assertions.assertEquals(0L, rcAssert);

		deadline = System.currentTimeMillis() + 10_000;
		while (SUSPECT_FIRED.get() == 0 && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
		Assertions.assertTrue(SUSPECT_FIRED.get() >= 1, "Suspect通道接管的定时器必须在本进程触发");
	}

	@SuppressWarnings("unchecked")
	private static TableX<Integer, BNodeRoot> tNodeRoot(Application app) {
		var t = app.getTable("Zeze_Builtin_Timer_tNodeRoot");
		Assertions.assertNotNull(t);
		return (TableX<Integer, BNodeRoot>)t;
	}

	@SuppressWarnings("unchecked")
	private static TableX<Long, BNode> tNodes(Application app) {
		var t = app.getTable("Zeze_Builtin_Timer_tNodes");
		Assertions.assertNotNull(t);
		return (TableX<Long, BNode>)t;
	}

	@SuppressWarnings("unchecked")
	private static TableX<String, BIndex> tIndexs(Application app) {
		var t = app.getTable("Zeze_Builtin_Timer_tIndexs");
		Assertions.assertNotNull(t);
		return (TableX<String, BIndex>)t;
	}
}
