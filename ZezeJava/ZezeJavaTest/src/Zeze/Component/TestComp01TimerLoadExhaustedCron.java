package Zeze.Component;

import harness.FastServerIds;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertTrue;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Builtin.Timer.BCronTimer;
import Zeze.Builtin.Timer.BIndex;
import Zeze.Builtin.Timer.BNode;
import Zeze.Component.AbstractTimer;
import Zeze.Component.Timer;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerSpec;
import Zeze.Config;
import Zeze.Transaction.TableX;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * FND14 comp-01回归：loadTimer坏行catch列表漏IllegalArgumentException——missfirePolicy=Nothing
 * 的耗尽cron行（表达式相对now已无可行后续时间）在装载路径L1604经CronTimerSpec.cronNextTime抛IAE
 * （FND4-46把null NPE改成IAE时未同步catch列表），冲出per-timer catch使整个装载事务失败：
 * 死行每次重启报错+1s延迟且永不被摘除，同节点其余定时器全部被跳过装载。
 * 修复：L1604窄化包裹，IAE翻译为ParseException复用既定摘行通道（不扩大外层catch列表，
 * 避免误摘try块内其他来源IAE的健康行）。
 * 构造：先以未来年份表达式正常建行（build时不拒绝），表手术改成已耗尽表达式+backdate
 * nextExpectedTime（模拟进程跨过年份边界后重启），重启Timer触发装载路径。
 * 断言：耗尽行被摘除（修复前红：行残留且装载事务失败）。
 */
@Fast
public class TestComp01TimerLoadExhaustedCron {

	// 独立serverId+url：@Fast类并行时避免本地库互撞（对齐TestTimerLoadMissfireAsync）。
	private static final int ServerId = FastServerIds.TEST_COMP01_TIMER_LOAD_EXHAUSTED_CRON;

	private Application app;
	private Timer timer;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(ServerId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("timer_load_exhausted_cron_" + ServerId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		conf.setTakeoverMode("off");
		app = new Application("TestFnd14Comp01", conf);
		timer = new TakeoverTestEnv.AccessibleTimer(new AppBase() {
			@Override
			public Application getZeze() {
				return app;
			}
		});
		app.start();
		timer.loadCustomClassAnd(); // 初始化nodeId/timerId/timerSerial AutoKey（demo.App同型调用）
	}

	@AfterEach
	public void tearDown() throws Exception {
		timer.stop();
		app.stop();
	}

	/** 占位回调句柄（类由Timer按名字反射实例化，须有默认构造；本用例不期待触发）。 */
	public static class NoopHandle implements TimerHandle {
		@Override
		public void onTimer(TimerContext context) {
		}
	}

	@Test
	@Timeout(60)
	public void testExhaustedCronRowRemovedOnLoad() throws Exception {
		timer.start();
		// 以未来年份表达式正常建行（build时有效；模拟"跨过年份边界前"的持久化状态）
		var timerIdHolder = new String[1];
		var rc = app.newProcedure(() -> {
			timerIdHolder[0] = timer.schedule(
					TimerSpec.ofCron("0 0 0 1 1 ? 2040")
							.missfirePolicy(AbstractTimer.eMissfirePolicyNothing),
					NoopHandle.class, null);
			return Procedure.Success;
		}, "TestFnd14Comp01.schedule").call();
		assertEquals(Procedure.Success, rc);
		var timerId = timerIdHolder[0];

		// 表手术：表达式改成已耗尽的过去年份 + backdate nextExpectedTime（模拟停机迟到）
		rc = app.newProcedure(() -> {
			var index = tIndexs().get(timerId);
			assertTrue(index != null, "index行必须存在");
			var node = tNodes().get(index.getNodeId());
			assertTrue(node != null, "node行必须存在");
			var cronTimer = (BCronTimer)node.getTimers().get(timerId).getTimerObj().getBean();
			cronTimer.setCronExpression("0 0 0 1 1 ? 2020");
			cronTimer.setNextExpectedTime(System.currentTimeMillis() - 60_000);
			return Procedure.Success;
		}, "TestFnd14Comp01.surgery").call();
		assertEquals(Procedure.Success, rc);

		// 重启Timer：loadTimer发现missfire（Nothing分支）→cronNextTime对耗尽表达式抛IAE
		timer.stop();
		timer.start();

		// 核心断言（红绿双向）：耗尽行必须被摘除——修复前IAE冲出per-timer catch，装载事务
		// 失败，行残留为每次重启报错+1s延迟的永久死行
		var removedHolder = new boolean[1];
		rc = app.newProcedure(() -> {
			var index = tIndexs().get(timerId);
			assertTrue(index != null, "index行仍在（摘行只清node内的timer行）");
			var node = tNodes().get(index.getNodeId());
			assertTrue(node != null, "node行仍在");
			removedHolder[0] = !node.getTimers().containsKey(timerId);
			return Procedure.Success;
		}, "TestFnd14Comp01.verify").call();
		assertEquals(Procedure.Success, rc);
		assertTrue(removedHolder[0], "耗尽cron行必须在装载路径被摘除（修复前IAE冲出catch，死行永久残留）");
	}

	@SuppressWarnings("unchecked")
	private TableX<String, BIndex> tIndexs() {
		var t = app.getTable("Zeze_Builtin_Timer_tIndexs");
		assertTrue(t != null);
		return (TableX<String, BIndex>)t;
	}

	@SuppressWarnings("unchecked")
	private TableX<Long, BNode> tNodes() {
		var t = app.getTable("Zeze_Builtin_Timer_tNodes");
		assertTrue(t != null);
		return (TableX<Long, BNode>)t;
	}
}
