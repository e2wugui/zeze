package Zeze.Services.ZokerImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Builtin.Zoker.StartService;
import Zeze.Builtin.Zoker.StopService;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * run.pid 条件删除与并发 start 的互斥：watchExit 回调的"读 run.pid 内容→比对 pid
 * →删路径"必须与 startService 的 launch→writeRunPid（opsLocks 临界区）串行——
 * 否则交错序列（退出回调读到旧身份后在窗口内暂停，外部守护亚秒级重启的 start 写入
 * 新身份，回调醒来按旧内容的比对删掉路径上的新身份文件）使运行中服务的盘上身份
 * 消失：pruneVersions 在用保护失明可误删在用版本目录，Zoker 重启后 adoptOrphans
 * 失明再 start 即同服务双实例。修复前回调删除路径不取锁（读-判-删非原子）。
 */
@Fast
public class TestRunPidDeleteRacesConcurrentStart {
	private static final boolean WINDOWS =
			System.getProperty("os.name", "").toLowerCase().contains("win");

	@TempDir
	Path tempDir;

	@AfterEach
	void cleanupTempDir() {
		TempDirBestEffort.delete(tempDir);
	}

	/**
	 * 确定性交错：测试 seam（判后删前注入点）让删除线程在比对通过后发起真实
	 * startService 并等其完成——修复前（删除不取 opsLocks）start 先写入新身份，
	 * 删除线程醒来把新身份文件删掉（红：新 pid 的 run.pid 消失）；修复后读-判-删
	 * 全程持锁，start 排队到删除完成之后才写新身份（绿：run.pid 完好且为新 pid）。
	 */
	@Test
	public void testOwnDeleteInterleavedWithStartKeepsNewRunPid() throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping/cmd）");
		var servicesDir = tempDir.resolve("services").toFile();
		Files.createDirectories(servicesDir.toPath());
		// 摆 services/svc/current → v1 + 保活命令（与 TestD01ServiceLifecycle 同构）
		var svc = servicesDir.toPath().resolve("svc");
		var v1 = Files.createDirectories(svc.resolve("v1"));
		Files.writeString(svc.resolve(DistributeManager.CURRENT_NAME), "v1");
		Files.writeString(v1.resolve(ServiceManager.SERVICE_PROPERTIES_NAME), "command=ping\nargs=-n 60 127.0.0.1\n");

		// 已退出的旧进程（真死 pid）：删除线程要比对的"自己的"身份。
		var exited = new ProcessBuilder("cmd", "/c", "exit 0").start();
		assertTrue(exited.waitFor(10, TimeUnit.SECONDS));
		ServiceManager.writeRunPid(svc.toFile(), new ServiceManager.RunPidRecord(
				exited.pid(), "2020-01-01T00:00:00Z", "cmd", "v1"));

		var sm = new ServiceManager(servicesDir);

		// 注入点内在删除线程上发起并发 start：红路径（删除无锁）start 立即完成写入
		// 新身份后删除醒来误删；绿路径（删除持 opsLocks）start 阻塞在锁上，注入点的
		// join 有界超时放行删除（按已判定的旧身份删旧文件，本就应删），锁序保证
		 // start 的写入落在删除之后。
		var windowEntered = new CountDownLatch(1);
		var startResult = new AtomicReference<Long>();
		var starter = new Thread(() -> {
			var r = new StartService();
			r.Argument.setServiceName("svc");
			startResult.set(sm.startService(r));
		});
		sm.setRunPidDeleteAfterJudgeHookForTest(() -> {
			windowEntered.countDown();
			starter.start();
			try {
				starter.join(5_000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		try {
			var deleter = new Thread(() -> sm.deleteRunPidIfOwn("svc", exited));
			deleter.start();
			assertTrue(windowEntered.await(10, TimeUnit.SECONDS), "删除线程必须进入判定后的窗口");

			deleter.join(60_000);
			starter.join(30_000);
			assertEquals(0L, startResult.get(), "并发 start 必须成功");

			var runPid = svc.resolve(ServiceManager.RUN_PID_NAME);
			assertTrue(Files.isRegularFile(runPid),
					"新身份不得被旧身份的条件删除误删（修复前：新 pid 的 run.pid 被删，"
							+ "prune 在用保护与防双启同时失守）");
			var rec = ServiceManager.RunPidRecord.parse(Files.readString(runPid));
			assertNotNull(rec);
			assertEquals(sm.getProcessForTest("svc").pid(), rec.pid, "盘上身份必须是新拉起进程的 pid");
		} finally {
			sm.setRunPidDeleteAfterJudgeHookForTest(null);
			var stop = new StopService();
			stop.Argument.setServiceName("svc");
			stop.Argument.setForce(true);
			sm.stopService(stop);
		}
	}
}
