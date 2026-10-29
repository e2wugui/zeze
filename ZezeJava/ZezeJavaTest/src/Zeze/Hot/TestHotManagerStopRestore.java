package Zeze.Hot;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import harness.Fast;
import harness.FastServerIds;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.CheckpointMode;
import Zeze.Util.Task;

/**
 * HotManager.stop() 停机恢复契约（2026-10-29 soak-1536 批 KCheckpointUnderCommitFlood
 * 3/5 红的修复回归）：
 * 1. testStopRestoresTaskHotGuard：start() 把 Task.hotGuard 换成本实例读锁守卫，
 *    stop() 必须撤销登记复原为无操作哨兵——否则停机的管理器（如 bench 车道
 *    BenchRoleTimer 起 Zezex server 后 stopAll）把僵尸 RRWL 读锁留给同 JVM 后续
 *    所有任务，无界 VT 下 >65535 个并发读持有者顶穿 "Maximum lock count exceeded"。
 * 2. testStopKeepsLaterManagerHook：Task.hotGuard 是 JVM 单槽静态，多管理器先后
 *    start 时后来者覆盖前者的钩；先停机者身份比对失败不得拆后来者的钩（否则活
 *    管理器期间任务派发绕过其安装互斥）。
 * 3. testStopNeverStartedManager：从未 start 的管理器（只调 tryDistribute 的用例
 *    形态）stop 无害且不碰全局钩。
 * 自包含：NoDatabase 轻量 Application + @TempDir，对齐 TestHotTryDistributeGuard。
 */
@Fast
public class TestHotManagerStopRestore {
	@TempDir
	static Path tempDir;

	private static @Nullable Application app;
	private static @Nullable AppBase appBase;

	@BeforeAll
	public static void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setNoDatabase(true);
		conf.setDefaultTableConf(new Config.TableConf());
		// DatabaseMemory 的表存储是 JVM 级静态 Map、按 url 分桶，独立 url 避免与其他测试 App 共桶。
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl(FastServerIds.URL_TEST_HOT_MANAGER_STOP_RESTORE);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("TestHotManagerStopRestore", conf);
		var field = Application.class.getDeclaredField("checkpoint");
		field.setAccessible(true);
		field.set(app, new Checkpoint(app, CheckpointMode.Table, 0));
		appBase = new AppBase() {
			@Override
			public @Nullable Application getZeze() {
				return app;
			}
		};
		app.initialize(appBase);
	}

	private static Path newHotDirs(String name) throws Exception {
		var workingDir = tempDir.resolve(name);
		Files.createDirectories(workingDir.resolve("interfaces"));
		Files.createDirectories(workingDir.resolve("modules"));
		Files.createDirectories(workingDir.resolve("distributes"));
		return workingDir;
	}

	private static HotManager newManager(String name) throws Exception {
		var workingDir = newHotDirs(name);
		return new HotManager(requireNonNull(appBase), workingDir.toString(),
				workingDir.resolve("distributes").toString());
	}

	@Test
	public void testStopRestoresTaskHotGuard() throws Exception {
		Task.tryInitThreadPool(); // start() 排周期安装定时器需要共享 scheduledPool
		var originalGuard = Task.hotGuard;
		var manager = newManager("restore");
		try {
			manager.start();
			try (var guard = Task.hotGuard.create()) {
				Assertions.assertNotSame(HotGuard.None, guard, "start() 应登记真实读锁守卫");
			}
			manager.stop();
			Assertions.assertSame(HotGuard.None, Task.hotGuard.create(), "stop() 应回复原无操作哨兵");
			manager.stop(); // 幂等
		} finally {
			manager.stop();
			Task.hotGuard = originalGuard;
		}
	}

	@Test
	public void testStopKeepsLaterManagerHook() throws Exception {
		Task.tryInitThreadPool();
		var originalGuard = Task.hotGuard;
		var first = newManager("interleave-1");
		var second = newManager("interleave-2");
		try {
			first.start();
			second.start(); // 单槽静态：后来者覆盖前者的钩
			first.stop();
			try (var guard = Task.hotGuard.create()) {
				Assertions.assertNotSame(HotGuard.None, guard, "先停机者不得拆掉后来者的钩");
			}
			second.stop();
			Assertions.assertSame(HotGuard.None, Task.hotGuard.create(), "最后停机者复原无操作哨兵");
		} finally {
			second.stop();
			first.stop();
			Task.hotGuard = originalGuard;
		}
	}

	@Test
	public void testStopNeverStartedManager() throws Exception {
		var originalGuard = Task.hotGuard;
		var manager = newManager("never-started");
		try {
			manager.stop(); // 从未 start：无钩可撤，收尾无害
			Assertions.assertSame(originalGuard, Task.hotGuard, "未登记者不得动全局钩");
			manager.stop(); // 幂等
		} finally {
			manager.stop();
			Task.hotGuard = originalGuard;
		}
	}
}
