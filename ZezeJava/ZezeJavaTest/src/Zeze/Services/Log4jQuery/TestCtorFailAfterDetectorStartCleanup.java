package Zeze.Services.Log4jQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static harness.DirCleanup.deleteBestEffort;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import Zeze.Services.Log4jQuery.Log4jFileManager;
import Zeze.Services.Log4jQuery.LogServiceConf;
import Zeze.Util.Task;

import harness.Fast;

/**
 * log4jquery-02回归：构造在fileCreateDetector.start()之后的schedulePeriodNow失败（Task调度池
 * 未初始化/停机拆池，Task.scheduledPoolOrThrow抛IllegalStateException且不自动重建——停机语义）
 * 时，外层finally只回滚logDirOwners独占登记不回收已启动的watch线程——线程携旧manager引用永驻，
 * 继续处理后续CREATE事件并写indexLinks；登记已被回滚，同（目录,活性）重建立即成功=双管并发写
 * 同一索引命名空间（nextLinkFile读max+1分派撞号交错写索引），登记机制要防的形态被它自己的回滚
 * 缺口击穿。装载段失败的回收形态由既有TestManagerConstructFailCleanup覆盖（内层catch），本类
 * 覆盖start之后的失败段。修复后外层finally的!constructed分支统一回收：timer已调度则cancel、
 * detector已start则stopAndJoin，然后才回滚登记（顺序对齐stop()的"登记最后释放"）。
 * 观察：构造抛异常后向目录写active文件——泄漏的watch线程会处理CREATE并在indexLinks/&lt;active&gt;/1
 * 创建索引文件（TestManagerConstructFailCleanup同款观察手法）；已join的线程无任何磁盘副作用。
 * 池状态控制：本类真实关闭全局Task池（TestOneByOnePoolInit同款手法），@Isolated独占运行——
 * gradle test的类级并行下不与其他类共享池状态竞态；AfterEach重建。
 */
@Fast
@Isolated
public class TestCtorFailAfterDetectorStartCleanup {
	@BeforeEach
	public void before() {
		Task.tryInitThreadPool();
	}

	@AfterEach
	public void after() {
		// 恢复全局池。注意：重建的是新池，原池上注册的静态周期任务不会恢复。
		Task.tryInitThreadPool();
	}

	@Test
	public void testCtorFailAfterDetectorStartReclaimsWatchThreadAndRollsBack() throws Exception {
		var logDir = Files.createTempDirectory("log4jquery-02-ctor-fail-after-start");
		var logConf = new LogServiceConf.LogConf();
		logConf.logActive = "zeze.log";
		logConf.logDir = logDir.toString();

		// 停机拆池形态：三静态池字段置null（先置null再等待终止），detector.start()之后的
		// schedulePeriodNow抛IllegalStateException。
		shutdownIgnoringTerminationTimeout();
		assertThrows(IllegalStateException.class, () -> new Log4jFileManager(logConf));
		Task.tryInitThreadPool(); // 构造失败已发生，立即恢复池：后续观察/重建走正常初始化形态

		// watch线程已回收的观察：向目录写active文件，泄漏线程会处理CREATE并在indexLinks/zeze.log/1
		// 创建索引文件（onFileCreated case-0的openFreshActiveIndex）；已join的线程无磁盘副作用。
		Files.write(logDir.resolve("zeze.log"), new byte[0]);
		var linkIndex = logDir.resolve("indexLinks").resolve("zeze.log").resolve("1");
		var deadline = System.currentTimeMillis() + 1_000;
		while (Files.notExists(linkIndex) && System.currentTimeMillis() < deadline)
			Thread.sleep(50);
		assertFalse(Files.exists(linkIndex),
				"构造失败（detector.start之后）应stopAndJoin回收watch线程，不得再处理文件创建事件");

		// 登记已回滚且旧线程已join：同（目录,活性）重建成功即单管——登记滞留会抛duplicate，
		// 旧线程泄漏时重建即双管并发写同一indexLinks命名空间（正是本案缺陷形态）。
		var manager = new Log4jFileManager(logConf);
		try {
			assertEquals(1, manager.size(), "同键重建成功：active文件被新manager登记（登记回滚未破坏）");
		} finally {
			manager.stop();
		}

		deleteBestEffort(logDir);
	}

	// 同TestOneByOnePoolInit：短等待关池（先置null再等待），终止超时忽略——用例只依赖
	// "三静态池字段已null"，不依赖遗留任务全部结束。
	private static void shutdownIgnoringTerminationTimeout() throws InterruptedException {
		try {
			Task.shutdown(200);
		} catch (java.util.concurrent.TimeoutException expected) {
			// 全套件环境下其他测试类遗留的周期任务令终止等待超时，忽略。
		}
	}
}
