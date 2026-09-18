package Zeze.Component;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import Zeze.AppBase;
import Zeze.Arch.ProviderApp;
import Zeze.Application;
import Zeze.Builtin.Timer.BAccountClientId;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-72回归：offline timer的内部终止路径（打完/用户回调抛异常）经Timer.cancel的
 * onTimerCancel钩子同步清离线簿记（tRoleOfflineTimers/tAccountOfflineTimers），
 * 杜绝"index已删、簿记残留"使同名重调度putIfAbsent撞残留抛IllegalStateException
 * （调用方整个事务回滚，残留行跨进程重启存活直到下次登录才被清算）。
 * 附带护栏：显式cancelOffline的簿记判定前移后仍返回true（TimerScope.cancel契约）。
 * <p>
 * 进程内轻量环境（SM=disable+Memory库，serverId独占a6段29601+），全部真实API构造、
 * 无伪造表行。TimerRole钩子的归属反查走defaultOnline兜底分支（生产为Game.
 * ProviderWithOnline按onlineSetName定位），以反射布线（对齐TestTimerNamedIdUnique
 * 对受保护成员的既有手法）；等待一律轮询条件，不赌时序。
 */
@Fast
public class TestFnd872OfflineTimerBookkeeping {

	public static class NormalHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) {
		}
	}

	public static class ThrowHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) {
			throw new RuntimeException("fnd872 boom");
		}
	}

	// Arch/Game Online的protected构造（load redirect入口）以命名子类放开，构造内为纯内存注册。
	private static final class TestArchOnline extends Zeze.Arch.Online {
		TestArchOnline(@NotNull AppBase app) {
			super(app);
		}
	}

	private static final class TestGameOnline extends Zeze.Game.Online {
		TestGameOnline(@NotNull AppBase app) {
			super(app);
		}
	}

	private static final class TestAppBase extends AppBase {
		private final @NotNull Application zeze;

		TestAppBase(@NotNull Application zeze) {
			this.zeze = zeze;
		}

		@Override
		public Application getZeze() {
			return zeze;
		}
	}

	// a6专属serverId段（上限16383内，避开默认0/100/300与Takeover伪造死者段777+）。
	private static final AtomicInteger NextServerId = new AtomicInteger(16181);

	private static final class Env implements AutoCloseable {
		final @NotNull Application zeze;
		final @NotNull Timer timer;
		final @NotNull TimerAccount accountTimer;
		final @NotNull TimerRole roleTimer;
		final @NotNull TestGameOnline gameOnline;

		Env(@NotNull String name) throws Exception {
			Task.tryInitThreadPool();
			var conf = new Config();
			conf.setServiceManager("disable");
			int serverId = NextServerId.getAndIncrement();
			conf.setServerId(serverId);
			conf.setDefaultTableConf(new Config.TableConf());
			var dbConf = new Config.DatabaseConf();
			dbConf.setDatabaseUrl("a6_fnd872_" + serverId); // 独立Memory桶
			conf.getDatabaseConfMap().putIfAbsent("", dbConf);
			conf.setTakeoverMode("dryrun");
			conf.setTakeoverTtl(600_000);
			conf.setTakeoverScanPeriod(600_000);

			zeze = new Application(name, conf);
			var providerApp = new ProviderApp(zeze); // fake ProviderApp：建立zeze.redirect，initialize创建App自有Timer
			var appBase = new TestAppBase(zeze);
			zeze.initialize(appBase);

			// 真实Arch Online + TimerAccount（仅构造走反射，生产仅经完整ProviderApp创建）。
			// Online必须在zeze.start()之前构造：其表在start时open。
			var archOnline = new TestArchOnline(appBase);
			var ctor = TimerAccount.class.getDeclaredConstructor(Zeze.Arch.Online.class);
			ctor.setAccessible(true);
			accountTimer = ctor.newInstance(archOnline);
			gameOnline = new TestGameOnline(appBase);
			roleTimer = gameOnline.getTimerRole();
			// OfflineHandle.onTimerCancel的归属反查：providerImplement按onlineSetName定位Online
			// （生产由initializeOnlineTimer经完整ProviderApp建立），fake下反射布线。
			var pwo = new Zeze.Game.ProviderWithOnline() {
			};
			var onlineField = Zeze.Game.ProviderWithOnline.class.getDeclaredField("online");
			onlineField.setAccessible(true);
			onlineField.set(pwo, gameOnline);
			var implField = Zeze.Arch.ProviderApp.class.getDeclaredField("providerImplement");
			implField.setAccessible(true);
			implField.set(providerApp, pwo);

			zeze.start();
			timer = zeze.getTimer();
			timer.start();
		}

		@Override
		public void close() throws Exception {
			zeze.stop();
		}
	}

	// 轮询等待条件成立（条件在独立事务内求值），不赌时序。
	private static boolean await(@NotNull Env env, @NotNull Supplier<Boolean> cond) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 10_000;
		while (System.currentTimeMillis() < deadline) {
			if (cond.get())
				return true;
			Thread.sleep(20);
		}
		return cond.get();
	}

	private static boolean indexGone(@NotNull Env env, @NotNull String timerId) {
		var gone = new boolean[1];
		Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
			gone[0] = env.timer.getTimerIndex(timerId) == null;
			return 0;
		}, "a6.fnd872.pollIndex")).call());
		return gone[0];
	}

	private static boolean roleBookkeepingGone(@NotNull Env env, long roleId, @NotNull String timerId) {
		var gone = new boolean[1];
		Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
			var row = env.gameOnline._tRoleOfflineTimers().get(roleId);
			gone[0] = row == null || row.getOfflineTimers().get(timerId) == null;
			return 0;
		}, "a6.fnd872.pollRoleBookkeeping")).call());
		return gone[0];
	}

	private static boolean accountBookkeepingGone(@NotNull Env env, @NotNull String account,
												  @NotNull String clientId, @NotNull String timerId) {
		var gone = new boolean[1];
		Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
			var row = env.timer.tAccountOfflineTimers().get(new BAccountClientId(account, clientId));
			gone[0] = row == null || row.getOfflineTimers().get(timerId) == null;
			return 0;
		}, "a6.fnd872.pollAccountBookkeeping")).call());
		return gone[0];
	}

	/** 打完终止：一次性角色offline timer触发完毕后簿记同步清除，同名重调度不再抛IAE。 */
	@Test
	public void testRoleOfflineFiredOutCleansBookkeeping() throws Exception {
		var timerId = "a6.fnd872.roleFiredOut";
		try (var env = new Env("TestFnd872RoleFiredOut")) {
			var r = new boolean[1];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.roleTimer.scheduleOfflineNamed(timerId, 16111L,
						TimerSpec.ofDelay(50).times(1), NormalHandle.class, null);
				return 0;
			}, "a6.fnd872.roleSchedule")).call());
			Assertions.assertTrue(r[0], "首次调度必须成功");
			Assertions.assertTrue(await(env, () -> indexGone(env, timerId)), "一次性timer触发后index必须消失");
			Assertions.assertTrue(roleBookkeepingGone(env, 16111L, timerId), "打完后离线簿记必须同步清除");
			// 修复前：残留簿记使putIfAbsent抛IllegalStateException，整个重调度事务失败。
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.roleTimer.scheduleOfflineNamed(timerId, 16111L,
						TimerSpec.ofDelay(60_000).times(1), NormalHandle.class, null);
				return 0;
			}, "a6.fnd872.roleReschedule")).call());
			Assertions.assertTrue(r[0], "同名重调度必须成功（不再撞残留簿记）");
		}
	}

	/** 回调异常终止：角色offline timer的handle抛异常自动取消后簿记同步清除，同名重调度成功。 */
	@Test
	public void testRoleOfflineHandleExceptionCleansBookkeeping() throws Exception {
		var timerId = "a6.fnd872.roleThrow";
		try (var env = new Env("TestFnd872RoleThrow")) {
			var r = new boolean[1];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.roleTimer.scheduleOfflineNamed(timerId, 16112L,
						TimerSpec.ofDelay(50).times(3), ThrowHandle.class, null);
				return 0;
			}, "a6.fnd872.roleScheduleThrow")).call());
			Assertions.assertTrue(r[0], "首次调度必须成功");
			Assertions.assertTrue(await(env, () -> indexGone(env, timerId)), "回调异常自动取消后index必须消失");
			Assertions.assertTrue(roleBookkeepingGone(env, 16112L, timerId), "异常取消后离线簿记必须同步清除");
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.roleTimer.scheduleOfflineNamed(timerId, 16112L,
						TimerSpec.ofDelay(60_000).times(1), NormalHandle.class, null);
				return 0;
			}, "a6.fnd872.roleRescheduleThrow")).call());
			Assertions.assertTrue(r[0], "同名重调度必须成功（不再撞残留簿记）");
		}
	}

	/** 打完终止的账号对称路径。 */
	@Test
	public void testAccountOfflineFiredOutCleansBookkeeping() throws Exception {
		var timerId = "a6.fnd872.accFiredOut";
		try (var env = new Env("TestFnd872AccFiredOut")) {
			var r = new boolean[1];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.accountTimer.scheduleOfflineNamed(timerId, "a6acc1", "a6cid1",
						TimerSpec.ofDelay(50).times(1), NormalHandle.class, null);
				return 0;
			}, "a6.fnd872.accSchedule")).call());
			Assertions.assertTrue(r[0], "首次调度必须成功");
			Assertions.assertTrue(await(env, () -> indexGone(env, timerId)), "一次性timer触发后index必须消失");
			Assertions.assertTrue(accountBookkeepingGone(env, "a6acc1", "a6cid1", timerId),
					"打完后账号离线簿记必须同步清除");
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.accountTimer.scheduleOfflineNamed(timerId, "a6acc1", "a6cid1",
						TimerSpec.ofDelay(60_000).times(1), NormalHandle.class, null);
				return 0;
			}, "a6.fnd872.accReschedule")).call());
			Assertions.assertTrue(r[0], "同名重调度必须成功（不再撞残留簿记）");
		}
	}

	/** 回调异常终止的账号对称路径。 */
	@Test
	public void testAccountOfflineHandleExceptionCleansBookkeeping() throws Exception {
		var timerId = "a6.fnd872.accThrow";
		try (var env = new Env("TestFnd872AccThrow")) {
			var r = new boolean[1];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.accountTimer.scheduleOfflineNamed(timerId, "a6acc2", "a6cid2",
						TimerSpec.ofDelay(50).times(3), ThrowHandle.class, null);
				return 0;
			}, "a6.fnd872.accScheduleThrow")).call());
			Assertions.assertTrue(r[0], "首次调度必须成功");
			Assertions.assertTrue(await(env, () -> indexGone(env, timerId)), "回调异常自动取消后index必须消失");
			Assertions.assertTrue(accountBookkeepingGone(env, "a6acc2", "a6cid2", timerId),
					"异常取消后账号离线簿记必须同步清除");
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.accountTimer.scheduleOfflineNamed(timerId, "a6acc2", "a6cid2",
						TimerSpec.ofDelay(60_000).times(1), NormalHandle.class, null);
				return 0;
			}, "a6.fnd872.accRescheduleThrow")).call());
			Assertions.assertTrue(r[0], "同名重调度必须成功（不再撞残留簿记）");
		}
	}

	/** 护栏：显式cancelOffline在钩子清理簿记后仍须返回true（TimerScope.cancel契约，判定已前移）。 */
	@Test
	public void testRoleExplicitCancelKeepsContract() throws Exception {
		var timerId = "a6.fnd872.roleCancel";
		try (var env = new Env("TestFnd872RoleCancel")) {
			var r = new boolean[2];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.roleTimer.scheduleOfflineNamed(timerId, 16113L,
						TimerSpec.ofDelay(60_000), NormalHandle.class, null);
				r[1] = env.roleTimer.cancelOffline(timerId, 16113L);
				return 0;
			}, "a6.fnd872.roleExplicitCancel")).call());
			Assertions.assertTrue(r[0], "首次调度必须成功");
			Assertions.assertTrue(r[1], "cancelOffline对真实存在的timer必须返回true（契约不因判定前移破坏）");
			Assertions.assertTrue(roleBookkeepingGone(env, 16113L, timerId), "显式取消后簿记必须清除");
		}
	}
}
