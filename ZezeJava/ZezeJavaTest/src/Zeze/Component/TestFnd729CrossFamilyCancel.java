package Zeze.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.AppBase;
import Zeze.Application;
import Zeze.Builtin.Timer.BArchOnlineTimer;
import Zeze.Builtin.Timer.BOnlineTimers;
import Zeze.Builtin.Timer.BSimpleTimer;
import Zeze.Config;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Procedure;
import Zeze.Util.Task;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND7-29回归：timerFutures三族（全局/在线/离线）共用同源timerId，取消路径"查无记录"
 * 分支的投机cancelFuture会误杀他族活future——记录在而future死，fire不再执行也无周期重装，
 * 静默停摆（全局族到进程重启、在线族到用户下线）。修复：查无本族记录不动共享runtime，
 * 孤儿future由fire侧自愈（fireSimple对index==null、fireOnline对bTimer==null）。
 * <p>
 * 两个误杀方向各一测；同族取消（全局/在线）语义不变作回归护栏。
 * TimerOnlineBase为包内抽象类，用map桩子类驱动cancelOnlineLocal（Online存储钩子以
 * ConcurrentHashMap替身，登录版本钩子返回常量已登录）。
 */
@Fast
public class TestFnd729CrossFamilyCancel {

	/** 在线族定时器桩：存储钩子全部map替身，供cancelOnlineLocal全路径驱动。 */
	private static final class StubOnlineTimers extends TimerOnlineBase<String> {
		private final @NotNull Timer timer;
		private final @NotNull ConcurrentHashMap<String, OnlineTimer<String>> onlineTimers = new ConcurrentHashMap<>();
		private final @NotNull ConcurrentHashMap<String, BOnlineTimers> localTimers = new ConcurrentHashMap<>();

		StubOnlineTimers(@NotNull Timer timer) {
			this.timer = timer;
		}

		@Override
		@NotNull Timer timer() {
			return timer;
		}

		@Override
		@NotNull String name() {
			return "StubOnlineTimers";
		}

		@Override
		@Nullable Long getLocalLoginVersion(@NotNull String id) {
			return 1L;
		}

		@Override
		@Nullable Long getSharedLoginVersion(@NotNull String id) {
			return 1L;
		}

		@Override
		@Nullable Long getLoginVersion(@NotNull String id) {
			return 1L;
		}

		@Override
		@NotNull OnlineTimer<String> newOnlineTimer(@NotNull String id, long loginVersion, long serialId,
													@NotNull Bean timerObj) {
			throw new UnsupportedOperationException();
		}

		@Override
		@Nullable OnlineTimer<String> getOnlineTimer(@NotNull String timerId) {
			return onlineTimers.get(timerId);
		}

		@Override
		void insertOnlineTimer(@NotNull String timerId, @NotNull OnlineTimer<String> onlineTimer) {
			onlineTimers.put(timerId, onlineTimer);
		}

		@Override
		void removeOnlineTimer(@NotNull String timerId) {
			onlineTimers.remove(timerId);
		}

		@Override
		@NotNull BOnlineTimers getOrAddLocalTimers(@NotNull String id) {
			return localTimers.computeIfAbsent(id, k -> new BOnlineTimers());
		}

		@Override
		@Nullable BOnlineTimers getLocalTimers(@NotNull String id) {
			return localTimers.get(id);
		}

		@Override
		void removeLocalTimers(@NotNull String id) {
			localTimers.remove(id);
		}

		@Override
		void transmitSimple(@NotNull String target, @NotNull Zeze.Builtin.Timer.BTransmitSimpleTimer p) {
		}

		@Override
		void transmitCron(@NotNull String target, @NotNull Zeze.Builtin.Timer.BTransmitCronTimer p) {
		}

		@Override
		void transmitCancel(@NotNull String target, @NotNull String timerId, long loginVersion) {
		}

		@Override
		@NotNull String identityString(@NotNull String id) {
			return id;
		}

		@Override
		void fillContext(@NotNull String id, @NotNull TimerContext context) {
		}
	}

	/** 在线族定时器记录桩：cancelOnlineLocal需要identity/serialId/loginVersion。 */
	private static final class StubOnlineTimer extends TimerOnlineBase.OnlineTimer<String> {
		private final long serialId;

		StubOnlineTimer(long serialId) {
			this.serialId = serialId;
		}

		@Override
		@NotNull Bean getTimerObj() {
			return new BSimpleTimer();
		}

		@Override
		long getSerialId() {
			return serialId;
		}

		@Override
		long getLoginVersion() {
			return 1L;
		}

		@Override
		@NotNull String identity() {
			return "owner1";
		}
	}

	public static class TestHandle implements TimerHandle {
		@Override
		public void onTimer(@NotNull TimerContext context) {
		}
	}

	// Application并发需要不同serverId与独立Memory桶（同TakeoverTestEnv口径，本类包内自持一份）。
	private static final AtomicInteger NextServerId = new AtomicInteger(300);

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

	private static final class TestEnv implements AutoCloseable {
		final @NotNull Application app;
		final @NotNull Timer timer;

		TestEnv(@NotNull String appName) throws Exception {
			Task.tryInitThreadPool();
			var conf = new Config();
			conf.setServiceManager("disable");
			int serverId = NextServerId.getAndIncrement();
			conf.setServerId(serverId);
			conf.setDefaultTableConf(new Config.TableConf());
			var dbConf = new Config.DatabaseConf();
			dbConf.setDatabaseUrl("fnd7_29_test_" + serverId);
			conf.getDatabaseConfMap().putIfAbsent("", dbConf);
			app = new Application(appName, conf);
			timer = new Timer(new TestAppBase(app));
			app.start();
			timer.loadCustomClassAnd();
			timer.start();
		}

		@Override
		public void close() throws Exception {
			app.stop();
		}
	}

	/** 方向1：在线族timerId传入全局取消入口Timer.cancel（index==null），不得杀其future。 */
	@Test
	public void testGlobalEntryMustNotKillOnlineFuture() throws Exception {
		try (var env = new TestEnv("TestFnd729CrossFamilyCancel1")) {
			var onlineTid = "UnitTest.FND7_29.onlineAlive";
			// 伪造在线族占用：_tAccountTimers行 + 已安装future（在线族不写_tIndexs）
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				env.timer.tAccountTimers().insert(onlineTid, new BArchOnlineTimer("acc", "cid", 1L, 1L));
				return Procedure.Success;
			}, "FND7_29.forgeOnlineTimer").call());
			env.timer.timerFutures.put(onlineTid, new CompletableFuture<>());
			Assertions.assertTrue(env.timer.timerFutures.containsKey(onlineTid));

			var canceled = new boolean[1];
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				canceled[0] = env.timer.cancel(onlineTid); // 查无_tIndexs记录，须返回false且不动future
				return Procedure.Success;
			}, "FND7_29.globalCancelOnlineTid").call());
			Assertions.assertFalse(canceled[0], "全局入口对在线族timerId必须返回false");
			Assertions.assertTrue(env.timer.timerFutures.containsKey(onlineTid),
					"查无记录分支不得cancelFuture他族活future（FND7-29：在线族定时器静默停摆）");
			// 护栏：在线表记录未被全局取消路径误删（表读需事务内）
			var onlineRecordAlive = new boolean[1];
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				onlineRecordAlive[0] = env.timer.tAccountTimers().get(onlineTid) != null;
				return Procedure.Success;
			}, "FND7_29.checkOnlineRow").call());
			Assertions.assertTrue(onlineRecordAlive[0], "在线表记录必须完好");
		}
	}

	/** 方向2：全局族timerId传入在线取消入口cancelOnlineLocal（bTimer==null），不得杀其future。 */
	@Test
	public void testOnlineEntryMustNotKillGlobalFuture() throws Exception {
		try (var env = new TestEnv("TestFnd729CrossFamilyCancel2")) {
			var stub = new StubOnlineTimers(env.timer);
			var globalTid = "UnitTest.FND7_29.globalAlive";
			var accepted = new boolean[1];
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				accepted[0] = env.timer.scheduleNamed(globalTid, TimerSpec.ofDelay(60_000), TestHandle.class);
				return Procedure.Success;
			}, "FND7_29.scheduleGlobal").call());
			Assertions.assertTrue(accepted[0]);
			Assertions.assertTrue(env.timer.timerFutures.containsKey(globalTid), "全局定时器future已安装");

			var canceled = new boolean[1];
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				canceled[0] = stub.cancelOnlineLocal(globalTid, "someone"); // 查无在线记录，须false且不动future
				return Procedure.Success;
			}, "FND7_29.onlineCancelGlobalTid").call());
			Assertions.assertFalse(canceled[0], "在线入口对全局族timerId必须返回false");
			Assertions.assertTrue(env.timer.timerFutures.containsKey(globalTid),
					"查无记录分支不得cancelFuture他族活future（FND7-29：全局定时器停摆到重启）");
			var globalRecordAlive = new boolean[1];
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				globalRecordAlive[0] = env.timer.tIndexs().get(globalTid) != null;
				return Procedure.Success;
			}, "FND7_29.checkGlobalRow").call());
			Assertions.assertTrue(globalRecordAlive[0], "全局定时器存储记录必须完好");

			// 护栏：同族取消（全局入口）语义不变——真取消并卸载future
			var canceled2 = new boolean[1];
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				canceled2[0] = env.timer.cancel(globalTid);
				return Procedure.Success;
			}, "FND7_29.globalCancelGlobalTid").call());
			Assertions.assertTrue(canceled2[0]);
			Assertions.assertFalse(env.timer.timerFutures.containsKey(globalTid), "同族取消必须卸载future");
			var globalRecordGone = new boolean[1];
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				globalRecordGone[0] = env.timer.tIndexs().get(globalTid) == null;
				return Procedure.Success;
			}, "FND7_29.checkGlobalRowGone").call());
			Assertions.assertTrue(globalRecordGone[0]);
		}
	}

	/** 护栏：在线族同族取消（归属校验通过）仍真取消——移除future与表记录。 */
	@Test
	public void testSameFamilyOnlineCancelStillWorks() throws Exception {
		try (var env = new TestEnv("TestFnd729CrossFamilyCancel3")) {
			var stub = new StubOnlineTimers(env.timer);
			var onlineTid = "UnitTest.FND7_29.onlineCancelMe";
			stub.onlineTimers.put(onlineTid, new StubOnlineTimer(1L));
			stub.getOrAddLocalTimers("owner1").getTimerIds().getOrAdd(onlineTid);
			var victimFuture = new CompletableFuture<>();
			env.timer.timerFutures.put(onlineTid, victimFuture);

			var canceled = new boolean[1];
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				canceled[0] = stub.cancelOnlineLocal(onlineTid, "owner1");
				return Procedure.Success;
			}, "FND7_29.sameFamilyOnlineCancel").call());
			Assertions.assertTrue(canceled[0], "同族归属校验通过的取消必须成功");
			Assertions.assertTrue(victimFuture.isCancelled(), "同族取消必须cancel future");
			Assertions.assertFalse(env.timer.timerFutures.containsKey(onlineTid));
			Assertions.assertNull(stub.getOnlineTimer(onlineTid), "在线表记录必须移除");
		}
	}
}
