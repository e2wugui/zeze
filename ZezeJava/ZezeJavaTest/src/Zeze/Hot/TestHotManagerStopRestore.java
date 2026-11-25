package Zeze.Hot;

import static java.util.Objects.requireNonNull;

import org.jspecify.annotations.Nullable;

import harness.Fast;
import harness.FastServerIds;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import Zeze.Net.Service;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.CheckpointMode;
import Zeze.Util.FewModifyMap;
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

	@Test
	public void testStopAwaitsInFlightInstallBeforeRestoringHotGuard() throws Exception {
		Task.tryInitThreadPool();
		var originalGuard = Task.hotGuard;
		var manager = newManager("drain-inflight");
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var stopError = new AtomicReference<@Nullable Throwable>();
		try {
			manager.start();
			var execField = HotManager.class.getDeclaredField("installExecutor");
			execField.setAccessible(true);
			var installExecutor = (ExecutorService)execField.get(manager);
			installExecutor.execute(() -> {
				entered.countDown();
				try {
					release.await(10, TimeUnit.SECONDS);
				} catch (InterruptedException ignored) {
					Thread.currentThread().interrupt();
				}
			});
			Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS), "在途安装任务应已开始执行");

			var stopThread = new Thread(() -> {
				try {
					manager.stop();
				} catch (Throwable t) {
					stopError.set(t);
				}
			}, "test-hot-stop");
			stopThread.start();
			Thread.sleep(150);

			Assertions.assertTrue(stopThread.isAlive(), "在途安装未完成前 stop() 必须等待排空");
			try (var guard = Task.hotGuard.create()) {
				Assertions.assertNotSame(HotGuard.None, guard, "在途安装未排空前不得提前撤销 Task.hotGuard");
			}

			release.countDown();
			stopThread.join(5000);
			Assertions.assertFalse(stopThread.isAlive(), "在途安装结束后 stop() 应尽快返回");
			Assertions.assertNull(stopError.get());
			Assertions.assertSame(HotGuard.None, Task.hotGuard.create(), "排空完成后 Task.hotGuard 应复原为无操作哨兵");
		} finally {
			release.countDown();
			manager.stop();
			Task.hotGuard = originalGuard;
		}
	}

	@Test
	public void testStopClosesInterfaceJarsAndDistributeFiles() throws Exception {
		Task.tryInitThreadPool();
		var originalGuard = Task.hotGuard;
		var workingDir = newHotDirs("close-jars");
		var interfaceJar = workingDir.resolve("interfaces").resolve("Dummy.interface.jar");
		try (var jarOut = new JarOutputStream(Files.newOutputStream(interfaceJar))) {
			jarOut.putNextEntry(new JarEntry("dummy/InterfaceStub.class"));
			jarOut.write(new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE});
			jarOut.closeEntry();
		}
		var manager = new HotManager(requireNonNull(appBase), workingDir.toString(),
				workingDir.resolve("distributes").toString());
		try {
			manager.start();
			// 同时模拟一个尚未 closeAndVerify 的分布式上传文件句柄
			manager.getDistributeManager().open("partial.bin");
			var partialFile = workingDir.resolve("distributes").resolve("partial.bin");

			var jarsField = HotManager.class.getDeclaredField("jars");
			jarsField.setAccessible(true);
			var jars = (FewModifyMap<?, ?>)jarsField.get(manager);
			var entriesField = HotManager.class.getDeclaredField("zipEntries");
			entriesField.setAccessible(true);
			var zipEntries = (FewModifyMap<?, ?>)entriesField.get(manager);
			Assertions.assertEquals(1, jars.size());
			Assertions.assertEquals(1, zipEntries.size());

			manager.stop();

			Assertions.assertTrue(jars.isEmpty(), "stop() 后 jars 句柄缓存必须清空");
			Assertions.assertTrue(zipEntries.isEmpty(), "stop() 后 zipEntries 索引必须清空");
			// Windows 下若 JarFile 或 RandomAccessFile 未关闭，Files.delete 会抛 FileSystemException
			Files.delete(interfaceJar);
			Files.delete(partialFile);
			Assertions.assertFalse(Files.exists(interfaceJar));
			Assertions.assertFalse(Files.exists(partialFile));
		} finally {
			manager.stop();
			Task.hotGuard = originalGuard;
		}
	}

	@Test
	public void testStopModuleDoesNotStopHotManagerService() throws Exception {
		Task.tryInitThreadPool();
		var originalGuard = Task.hotGuard;
		var manager = newManager("stop-module-keeps-service");
		try {
			manager.start();
			var serviceField = HotManager.class.getDeclaredField("hotManagerService");
			serviceField.setAccessible(true);
			var hotManagerService = (Service)serviceField.get(manager);
			var stoppedField = Service.class.getDeclaredField("stopped");
			stoppedField.setAccessible(true);

			Assertions.assertFalse(stoppedField.getBoolean(hotManagerService), "start() 后发布服务应处于运行态");
			manager.stopModule("NonExistentModule");
			Assertions.assertFalse(stoppedField.getBoolean(hotManagerService),
					"stopModule() 仅停止单个模块，不得误停全局 hotManagerService");

			manager.stop();
			Assertions.assertTrue(stoppedField.getBoolean(hotManagerService), "HotManager.stop() 应停止全局发布服务");
		} finally {
			manager.stop();
			Task.hotGuard = originalGuard;
		}
	}
}
