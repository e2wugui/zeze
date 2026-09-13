package UnitTest.Zeze.Component;

import java.util.concurrent.atomic.AtomicInteger;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Component.TimerContext;
import Zeze.Component.TimerHandle;
import Zeze.Component.TimerSpec;
import Zeze.Config;
import Zeze.Transaction.Procedure;
import Zeze.Util.TaskSpec;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-20 停机闸门的重启回归：Timer.start() 必须先置 started 再 loadTimer——
 * 否则装载事务在本线程同步提交时，whileCommit 里的安装被闸门全部拒绝，
 * stop→start 重启后存量定时器静默停摆（本用例即锁住该顺序）。
 * 场景：注册持久化周期定时器→验证触发→stop()→start()→断言重启后继续触发。
 * 自包含（禁用SM、独立serverId与db目录、无网络），标 @Fast。
 */
@Fast
public class TestTimerStopStartRestart {
	// 与其他 @Fast 测试错开 serverId：并行时 Application 本地缓存按 serverId 一份。
	private static final AtomicInteger NextServerId = new AtomicInteger(7160);

	public static class RestartCountHandle implements TimerHandle {
		static final AtomicInteger FireCount = new AtomicInteger();

		@Override
		public void onTimer(@NotNull TimerContext timerContext) {
			FireCount.incrementAndGet();
		}
	}

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("timer_restart_test_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestTimerStopStartRestart" + conf.getServerId(), conf);
	}

	@Test
	public void testTimerFireAfterRestart() throws Exception {
		var zeze = newApp();
		// 走生产装配路径：fake ProviderApp 建立 zeze.redirect，initialize 创建 Application
		// 拥有的 Timer，start() 内完成 AutoKey 初始化（loadCustomClassAnd）。
		new Zeze.Arch.ProviderApp(zeze);
		var appBase = new AppBase() {
			@Override
			public Application getZeze() {
				return zeze;
			}
		};
		zeze.initialize(appBase);
		try {
			zeze.start();
			var timer = zeze.getTimer();
			timer.start();

			final int periodMs = 50;
			RestartCountHandle.FireCount.set(0);
			Assertions.assertEquals(Procedure.Success, TaskSpec.ofProcedure(zeze.newProcedure(() -> {
				timer.schedule(TimerSpec.ofDelay(1).period(periodMs), RestartCountHandle.class);
				return Procedure.Success;
			}, "test_restart_schedule")).call());

			waitFireAtLeast(1, 2000);
			Assertions.assertTrue(RestartCountHandle.FireCount.get() > 0, "重启前定时器必须已触发");

			// stop → start（重启）：装载走 loadTimer，安装经 whileCommit 在本线程同步提交。
			timer.stop();
			timer.start();

			// 重启后必须继续触发：若 started 晚于 loadTimer 置位，装载期安装被
			// FND5-20 闸门拒绝，这里等不到新触发即失败。
			var afterRestart = RestartCountHandle.FireCount.get();
			waitFireAtLeast(afterRestart + 1, 3000);
			Assertions.assertTrue(RestartCountHandle.FireCount.get() > afterRestart,
					"重启后定时器必须继续触发（FND5-20闸门不得拒绝装载期安装）");
		} finally {
			try {
				zeze.stop(); // Application.stop会先停自己拥有的timer
			} catch (Exception ignored) {
			}
		}
	}

	private static void waitFireAtLeast(int expect, long timeoutMs) throws InterruptedException {
		var deadline = System.currentTimeMillis() + timeoutMs;
		while (RestartCountHandle.FireCount.get() < expect) {
			if (System.currentTimeMillis() > deadline)
				return;
			Thread.sleep(10);
		}
	}
}
