package UnitTest.Zeze.Component;

import Zeze.Application;
import Zeze.Builtin.Timer.BArchOnlineTimer;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerSpec;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND4-41：命名timerId全局唯一不变量的跨族查重。原实现三套查重互不相通（online入口只查
 * 本族在线表、全局/离线入口只查_tIndexs），同名online/全局定时器可并存并共用timerFutures
 * 相互覆盖——被覆盖一方本地调度被杀（DB记录仍在，静默停摆），违反唯一性公开契约。
 * 修复：查重收口到Timer.isNamedTimerIdOccupied（_tIndexs+全部onlineSet的Role在线表+
 * Account在线表）；全局/离线入口保留同族同server重建语义，仅叠加在线族碰撞检查。
 * <p>
 * Account在线表占用通过直接伪造表行构造（scheduleOnlineNamed需完整登录栈）；
 * Role在线表方向（表名带onlineSet后缀，需provider栈）以定向核查覆盖；同族重建语义
 * （scheduleNamed同名重调度仍成功）作为回归护栏。
 */
@Fast
public class TestTimerNamedIdUnique {

	public static class TestHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) {
		}
	}

	@SuppressWarnings("unchecked")
	private static Zeze.Builtin.Timer.tAccountTimers accountTimers(Zeze.Component.Timer timer) throws Exception {
		var field = Zeze.Component.AbstractTimer.class.getDeclaredField("_tAccountTimers");
		field.setAccessible(true);
		return (Zeze.Builtin.Timer.tAccountTimers)field.get(timer);
	}

	@Test
	public void testAccountOnlineOccupiesGlobalName() throws Exception {
		Task.tryInitThreadPool();
		var timerId = "UnitTest.FND4_41.AccountOnlineOccupied";
		var conf = TakeoverTestEnv.newConf("dryrun", 600_000, 600_000);
		var app = new Application("TestTimerNamedIdUnique", conf);
		var timer = new TakeoverTestEnv.AccessibleTimer(new TakeoverTestEnv.TestAppBase(app));
		try {
			app.start();
			timer.loadCustomClassAnd(); // Application.start只对自己的timer调用（SM=disable时为null）
			timer.start();

			// 伪造Account在线表占用（占用事实即可判定跨族闸门，不经登录栈）
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(app.newProcedure(() -> {
				accountTimers(timer).insert(timerId, new BArchOnlineTimer("acc", "cid", 1L, 1L));
				return Procedure.Success;
			}, "FND4_41.insertAccountOnline")).call());

			// 全局scheduleNamed同id必须被拒：同名并存会共用timerFutures相互覆盖
			var accepted = new boolean[1];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(app.newProcedure(() -> {
				accepted[0] = timer.scheduleNamed(timerId, TimerSpec.ofDelay(60_000), TestHandle.class);
				return Procedure.Success;
			}, "FND4_41.scheduleNamedGlobal")).call());
			Assertions.assertFalse(accepted[0], "被Account在线定时器占用的timerId，全局scheduleNamed必须返回false");
		} finally {
			app.stop();
		}
	}

	/** 回归护栏：同族（全局）同名重调度仍成功（重建语义不受跨族查重影响）。 */
	@Test
	public void testSameFamilyRescheduleStillWorks() throws Exception {
		Task.tryInitThreadPool();
		var timerId = "UnitTest.FND4_41.SameFamilyReschedule";
		var conf = TakeoverTestEnv.newConf("dryrun", 600_000, 600_000);
		var app = new Application("TestTimerNamedIdUnique2", conf);
		var timer = new TakeoverTestEnv.AccessibleTimer(new TakeoverTestEnv.TestAppBase(app));
		try {
			app.start();
			timer.loadCustomClassAnd(); // Application.start只对自己的timer调用（SM=disable时为null）
			timer.start();

			var accepted = new boolean[2];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(app.newProcedure(() -> {
				accepted[0] = timer.scheduleNamed(timerId, TimerSpec.ofDelay(60_000), TestHandle.class);
				accepted[1] = timer.scheduleNamed(timerId, TimerSpec.ofDelay(90_000), TestHandle.class);
				return Procedure.Success;
			}, "FND4_41.sameFamilyReschedule")).call());
			Assertions.assertTrue(accepted[0], "首次调度必须成功");
			Assertions.assertTrue(accepted[1], "同族同名重调度必须保留重建语义（仍成功）");
		} finally {
			app.stop();
		}
	}
}
