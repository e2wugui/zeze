package UnitTest.Zeze.Component;

import Zeze.AppBase;
import Zeze.Arch.ProviderApp;
import Zeze.Application;
import Zeze.Component.TimerAccount;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerRole;
import Zeze.Component.TimerSpec;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 命名timerId碰撞矩阵（offline/global/online三族同名互斥）global→offline方向的闭环回归：
 * 全局scheduleNamed撞已存在的offline timer时（Timer.scheduleNamed的index命中分支），
 * 原实现无族校验直接cancel重建为全局timer，残留tAccountOfflineTimers/_tRoleOfflineTimers
 * 脏簿记——属主下次登录按簿记反向cancel，静默杀死重建的全局timer。修复后该方向返回false。
 * 其余三方向（offline→global FND6-20、global→online FND4-41、online→global FND4-41）
 * 与全局同族重建语义一并作为回归护栏。
 * <p>
 * 进程内轻量环境（SM=disable+Memory库），全部真实API构造、无伪造表行：
 * fake ProviderApp（public单参构造）建立zeze.redirect后，initialize创建App自有Timer，
 * Arch/Game两套Online经protected构造（load redirect入口）以命名子类构造（其表在start前
 * 手动RegisterZezeTables补注册），Game侧构造内自带真实TimerRole；getLogoutVersion对
 * 无在线记录返回0L，offline调度无需登录栈。唯一反射点：TimerAccount无公开构造入口
 * （生产仅Timer.initializeOnlineTimer经完整ProviderApp创建，测试无网络栈），以反射调用
 * 其包私有构造创建真实对象（对齐TestTimerNamedIdUnique对受保护成员的既有手法）。
 */
@Fast
public class TestTimerOfflineNamedCollision {

	public static class TestHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) {
		}
	}

	// Arch.Online的protected构造（load redirect用）以命名子类放开super访问；
	// 构造内RegisterProtocols/RegisterZezeTables均为纯内存注册。
	private static final class TestArchOnline extends Zeze.Arch.Online {
		TestArchOnline(@NotNull AppBase app) {
			super(app);
		}
	}

	// Game.Online同上；其构造内即创建真实TimerRole（timerRole = new TimerRole(this)）。
	private static final class TestGameOnline extends Zeze.Game.Online {
		TestGameOnline(@NotNull AppBase app) {
			super(app);
		}
	}

	/** 单App环境：App自有Timer + 真实TimerAccount/TimerRole（Arch/Game两套Online共存，表名不冲突）。 */
	private static final class Env implements AutoCloseable {
		final Application zeze;
		final Zeze.Component.Timer timer;
		final TimerAccount accountTimer;
		final TimerRole roleTimer;

		Env(String name) throws Exception {
			var conf = TakeoverTestEnv.newConf("dryrun", 600_000, 600_000);
			zeze = new Application(name, conf);
			new ProviderApp(zeze); // fake ProviderApp：建立zeze.redirect，供initialize创建Timer
			var appBase = new TakeoverTestEnv.TestAppBase(zeze);
			zeze.initialize(appBase); // redirect!=null且非NoDatabase：创建App自有Timer（Timer.create）

			// 真实Arch Online + 真实TimerAccount（仅构造调用反射，见类注释）。
			// Online构造器自行RegisterZezeTables，但TableX.cache在zeze.start()时open设置——
			// 必须在start之前构造，start之后构造的Online表未open，访问即NPE(cache==null)。
			var archOnline = new TestArchOnline(appBase);
			var ctor = TimerAccount.class.getDeclaredConstructor(Zeze.Arch.Online.class);
			ctor.setAccessible(true);
			accountTimer = ctor.newInstance(archOnline);
			roleTimer = new TestGameOnline(appBase).getTimerRole();

			zeze.start();
			timer = zeze.getTimer();
			timer.start();
		}

		@Override
		public void close() throws Exception {
			zeze.stop();
		}
	}

	/** 用例a：全局"Z"占名后，账号与角色的offline scheduleNamed都必须被拒（offline→global方向FND6-20回归）。 */
	@Test
	public void testGlobalFirstRejectsBothOfflineFamilies() throws Exception {
		Task.tryInitThreadPool();
		var timerId = "UnitTest.Collision.Z";
		try (var env = new Env("TestTimerOfflineNamedCollision1")) {
			var r = new boolean[3];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.timer.scheduleNamed(timerId, TimerSpec.ofDelay(60_000), TestHandle.class, null);
				r[1] = env.accountTimer.scheduleOfflineNamed(timerId, "acc1", "cid1",
						TimerSpec.ofDelay(60_000), TestHandle.class, null);
				r[2] = env.roleTimer.scheduleOfflineNamed(timerId, 1001L,
						TimerSpec.ofDelay(60_000), TestHandle.class, null);
				return Procedure.Success;
			}, "collision.globalFirst")).call());
			Assertions.assertTrue(r[0], "全局首次调度必须成功");
			Assertions.assertFalse(r[1], "撞全局同名timer，账号offline scheduleNamed必须返回false");
			Assertions.assertFalse(r[2], "撞全局同名timer，角色offline scheduleNamed必须返回false");
		}
	}

	/** 用例b+c：账号A offline占名"X"后，异主（账号B）被拒（族+归属判定）；同主再次调用仍true（cancel+重建不回归）。 */
	@Test
	public void testAccountOfflineOwnerIsolation() throws Exception {
		Task.tryInitThreadPool();
		var timerId = "UnitTest.Collision.X";
		try (var env = new Env("TestTimerOfflineNamedCollision2")) {
			var r = new boolean[3];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.accountTimer.scheduleOfflineNamed(timerId, "accA", "cidA",
						TimerSpec.ofDelay(60_000), TestHandle.class, null);
				r[1] = env.accountTimer.scheduleOfflineNamed(timerId, "accB", "cidB",
						TimerSpec.ofDelay(60_000), TestHandle.class, null);
				r[2] = env.accountTimer.scheduleOfflineNamed(timerId, "accA", "cidA",
						TimerSpec.ofDelay(90_000), TestHandle.class, null);
				return Procedure.Success;
			}, "collision.ownerIsolation")).call());
			Assertions.assertTrue(r[0], "账号A首次offline调度必须成功");
			Assertions.assertFalse(r[1], "异主（账号B）撞账号A的offline timer必须返回false");
			Assertions.assertTrue(r[2], "同主重调度必须保留cancel+重建语义（仍成功）");
		}
	}

	/**
	 * 用例d（本轮闭环点）：账号offline占名后，全局scheduleNamed的simple与cron两路径都必须被拒，
	 * 且原offline timer未被杀——同主再次offline调度仍true（若被全局cancel重建为全局timer，
	 * 族+归属判定将失败返回false，此断言即存活证明）。
	 */
	@Test
	public void testAccountOfflineFirstRejectsGlobal() throws Exception {
		Task.tryInitThreadPool();
		var simpleId = "UnitTest.Collision.Y";
		var cronId = "UnitTest.Collision.YC";
		try (var env = new Env("TestTimerOfflineNamedCollision3")) {
			var r = new boolean[5];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.accountTimer.scheduleOfflineNamed(simpleId, "accA", "cidA",
						TimerSpec.ofDelay(60_000), TestHandle.class, null);
				r[1] = env.timer.scheduleNamed(simpleId, TimerSpec.ofDelay(60_000), TestHandle.class, null);
				r[2] = env.timer.scheduleNamed(simpleId, TimerSpec.ofCron("*/1 * * * * ?"),
						TestHandle.class, null);
				r[3] = env.accountTimer.scheduleOfflineNamed(cronId, "accA", "cidA",
						TimerSpec.ofCron("*/1 * * * * ?"), TestHandle.class, null);
				r[4] = env.timer.scheduleNamed(cronId, TimerSpec.ofDelay(60_000), TestHandle.class, null);
				return Procedure.Success;
			}, "collision.offlineFirstAccount")).call());
			Assertions.assertTrue(r[0], "账号offline首次调度必须成功");
			Assertions.assertFalse(r[1], "撞账号offline timer，全局scheduleNamed(simple)必须返回false");
			Assertions.assertFalse(r[2], "撞账号offline timer，全局scheduleNamed(cron)必须返回false");
			Assertions.assertTrue(r[3], "账号offline以cron占名必须成功");
			Assertions.assertFalse(r[4], "撞账号offline(cron)timer，全局scheduleNamed必须返回false");
			// 存活证明：同主再次调度走族+归属判定通过→cancel+重建，返回true；
			// 若全局路径曾杀掉offline timer重建为全局族，这里会因族不符返回false。
			var alive = new boolean[2];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				alive[0] = env.accountTimer.scheduleOfflineNamed(simpleId, "accA", "cidA",
						TimerSpec.ofDelay(90_000), TestHandle.class, null);
				alive[1] = env.accountTimer.scheduleOfflineNamed(cronId, "accA", "cidA",
						TimerSpec.ofCron("*/2 * * * * ?"), TestHandle.class, null);
				return Procedure.Success;
			}, "collision.offlineAliveAccount")).call());
			Assertions.assertTrue(alive[0], "被全局撞名后的simple offline timer必须仍属原主（存活）");
			Assertions.assertTrue(alive[1], "被全局撞名后的cron offline timer必须仍属原主（存活）");
		}
	}

	/** 用例d的角色方向镜像：offline族判定覆盖BOfflineRoleCustom分支（simple/cron两路径）。 */
	@Test
	public void testRoleOfflineFirstRejectsGlobal() throws Exception {
		Task.tryInitThreadPool();
		var simpleId = "UnitTest.Collision.YR";
		var cronId = "UnitTest.Collision.YRC";
		try (var env = new Env("TestTimerOfflineNamedCollision4")) {
			var r = new boolean[4];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.roleTimer.scheduleOfflineNamed(simpleId, 2001L,
						TimerSpec.ofDelay(60_000), TestHandle.class, null);
				r[1] = env.timer.scheduleNamed(simpleId, TimerSpec.ofDelay(60_000), TestHandle.class, null);
				r[2] = env.roleTimer.scheduleOfflineNamed(cronId, 2001L,
						TimerSpec.ofCron("*/1 * * * * ?"), TestHandle.class, null);
				r[3] = env.timer.scheduleNamed(cronId, TimerSpec.ofCron("*/1 * * * * ?"),
						TestHandle.class, null);
				return Procedure.Success;
			}, "collision.offlineFirstRole")).call());
			Assertions.assertTrue(r[0], "角色offline首次调度必须成功");
			Assertions.assertFalse(r[1], "撞角色offline timer，全局scheduleNamed(simple)必须返回false");
			Assertions.assertTrue(r[2], "角色offline以cron占名必须成功");
			Assertions.assertFalse(r[3], "撞角色offline(cron)timer，全局scheduleNamed(cron)必须返回false");
			// 存活证明（同用例d）
			var alive = new boolean[2];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				alive[0] = env.roleTimer.scheduleOfflineNamed(simpleId, 2001L,
						TimerSpec.ofDelay(90_000), TestHandle.class, null);
				alive[1] = env.roleTimer.scheduleOfflineNamed(cronId, 2001L,
						TimerSpec.ofCron("*/2 * * * * ?"), TestHandle.class, null);
				return Procedure.Success;
			}, "collision.offlineAliveRole")).call());
			Assertions.assertTrue(alive[0], "被全局撞名后的simple offline timer必须仍属原主（存活）");
			Assertions.assertTrue(alive[1], "被全局撞名后的cron offline timer必须仍属原主（存活）");
		}
	}

	/** 用例e：全局同族同名重调度保留cancel+重建语义（两次均true，跨族守卫不影响同族）。 */
	@Test
	public void testGlobalRebuildStillWorks() throws Exception {
		Task.tryInitThreadPool();
		var timerId = "UnitTest.Collision.W";
		try (var env = new Env("TestTimerOfflineNamedCollision5")) {
			var r = new boolean[2];
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(env.zeze.newProcedure(() -> {
				r[0] = env.timer.scheduleNamed(timerId, TimerSpec.ofDelay(60_000), TestHandle.class, null);
				r[1] = env.timer.scheduleNamed(timerId, TimerSpec.ofDelay(90_000), TestHandle.class, null);
				return Procedure.Success;
			}, "collision.globalRebuild")).call());
			Assertions.assertTrue(r[0], "全局首次调度必须成功");
			Assertions.assertTrue(r[1], "全局同族同名重调度必须保留重建语义（仍成功）");
		}
	}
}
