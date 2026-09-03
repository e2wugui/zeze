package Zeze.Hot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import Zeze.AppBase;
import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Checkpoint;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Procedure;

/**
 * FND-G1-1 / FND-G1-2 回归（HotManager.install / tryDistribute）：
 * 1. testBadPackageNoSchemasJar：ready 存在但缺 __hot_schemas__*.jar（坏包）时，
 *    loadSchemas 的异常必须在 install 内部回滚消化并以 eInstall 报告
 *    （tryDistribute 返回 0），不允许异常逃逸——修复前异常穿透 install 被
 *    tryDistribute 外层 catch 吞掉后返回 Procedure.Exception，已 stop 的旧模块
 *    无任何恢复（MainRollbackAction 注册晚于该阶段）。
 * 2. testConcurrentTryDistributeMutualExclusion：10 秒定时器与远程 TryDistribute
 *    两个通道并发调用 tryDistribute 时必须互斥：第一个调用的安装（installReadies）
 *    完成前，第二个调用不得进入；等待获得互斥后重查 ready（已被清理则空转返回）。
 * 自包含：NoDatabase 轻量 Application（Memory 库独立 url 分桶）+ @TempDir，
 * 不依赖外部 ServiceManager/数据库进程。
 */
@Fast
public class TestHotTryDistributeGuard {
	@TempDir
	static Path tempDir;

	private static Application app;
	private static AppBase appBase;

	@BeforeAll
	public static void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setNoDatabase(true);
		conf.setDefaultTableConf(new Config.TableConf());
		// DatabaseMemory 的表存储是 JVM 级静态 Map、按 url 分桶，独立 url 避免与其他测试 App 共桶。
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("hot_trydistribute_test");
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("TestHotTryDistributeGuard", conf);
		// install 在写锁内调用 app.getZeze().checkpointRun()；NoDatabase 轻量模式不创建
		// Checkpoint，注入未 start 的（与 TestProcessRpcResponseResolveOnce 相同模式）。
		var field = Application.class.getDeclaredField("checkpoint");
		field.setAccessible(true);
		field.set(app, new Checkpoint(app, CheckpointMode.Table, 0));
		appBase = new AppBase() {
			@Override
			public Application getZeze() {
				return app;
			}
		};
		app.initialize(appBase); // install 内通过 zeze.getAppBase() 取回 AppBase。
	}

	private static Path newHotDirs(String name) throws Exception {
		var workingDir = tempDir.resolve(name);
		Files.createDirectories(workingDir.resolve("interfaces"));
		Files.createDirectories(workingDir.resolve("modules"));
		Files.createDirectories(workingDir.resolve("distributes"));
		return workingDir;
	}

	@Test
	public void testBadPackageNoSchemasJar() throws Exception {
		var workingDir = newHotDirs("w1");
		var distributeDir = workingDir.resolve("distributes");
		var manager = new HotManager(appBase, workingDir.toString(), distributeDir.toString());
		// 坏包：有 ready，但没有任何 __hot_schemas__*.jar（也没有模块 jar 对）。
		var ready = distributeDir.resolve("ready");
		Files.writeString(ready, "#unit-test\n");

		var rc = manager.tryDistribute(false);

		// 修复后：loadSchemas 异常在 install 内触发回滚（恢复旧 schemas、重启已停止的旧模块）
		// 并返回 null → tryDistribute 走 eInstall 报告路径（setIdle(eInstall) + return 0）。
		// 修复前：异常逃逸出 install，被 tryDistribute 外层 catch 吞掉，返回 Procedure.Exception。
		Assertions.assertEquals(0, rc);
		// 坏包残留清理：ready 被 renameDistributes 挪进 backup 子目录。
		Assertions.assertFalse(Files.exists(ready), "ready must be moved to backup on install failure");
		Assertions.assertFalse(manager.isUpgrading());
	}

	@Test
	public void testConcurrentTryDistributeMutualExclusion() throws Exception {
		var workingDir = newHotDirs("w2");
		var distributeDir = workingDir.resolve("distributes");
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var inInstall = new AtomicInteger();
		var peak = new AtomicInteger();
		// 匿名子类把 installReadies 变成可控的“慢安装”：记录并发进入数并阻塞到放行，
		// tryDistribute 的其余部分（互斥锁、ready 检查、失败清理）全部走真实代码。
		var manager = new HotManager(appBase, workingDir.toString(), distributeDir.toString()) {
			@Override
			public ArrayList<HotModule> installReadies(boolean atomicAll) {
				inInstall.incrementAndGet();
				peak.accumulateAndGet(inInstall.get(), Math::max);
				entered.countDown();
				try {
					release.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				inInstall.decrementAndGet();
				return null; // 模拟安装失败，走 eInstall 报告路径。
			}
		};
		var ready = distributeDir.resolve("ready");
		Files.writeString(ready, "#unit-test\n");
		var rc1 = new AtomicLong(-1);
		var rc2 = new AtomicLong(-1);

		var t1 = new Thread(() -> rc1.set(manager.tryDistribute(false)), "tryDistribute-1");
		var t2 = new Thread(() -> rc2.set(manager.tryDistribute(false)), "tryDistribute-2");
		t1.start();
		Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS), "first caller must enter installReadies");
		t2.start();
		// 宽限窗口：若 tryDistribute 无互斥，第二个调用此刻会立即进入 installReadies。
		Thread.sleep(300);
		Assertions.assertEquals(1, inInstall.get(), "second caller must not enter install while first is running");
		Assertions.assertTrue(t2.isAlive(), "second caller must block until the first install finishes");
		release.countDown();
		t1.join(5000);
		t2.join(5000);
		Assertions.assertEquals(1, peak.get(), "installReadies must never run concurrently");
		// 第一个调用安装失败：eInstall 报告路径返回 0（异常不逃逸，同 FND-G1-1 行为）。
		Assertions.assertEquals(0, rc1.get());
		// 第二个调用等到互斥后重查 ready：已被第一次失败的 renameDistributes 清理，
		// 走现有 no-ready 空转路径返回 Procedure.Exception。
		Assertions.assertEquals(Procedure.Exception, rc2.get());
		Assertions.assertFalse(Files.exists(ready), "ready must be moved to backup by the first caller");
	}
}
