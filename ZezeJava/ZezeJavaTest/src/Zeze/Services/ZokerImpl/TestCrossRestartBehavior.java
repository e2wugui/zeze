package Zeze.Services.ZokerImpl;

import harness.proc.Procs;
import harness.Extra;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import Zeze.Builtin.Zoker.BService;
import Zeze.Builtin.Zoker.StartService;
import Zeze.Builtin.Zoker.StopService;
import Zeze.IModule;
import Zeze.Services.Zoker;
import harness.proc.Procs;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static harness.DirCleanup.deleteBestEffort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND21 GE-D01（方案A）：进程记账跨 Zoker 重启的语义连续——只用既有 API 的行为级验证
 * （全用例可在修复前基线上编译运行，红点=行为红非链接红）。
 *
 * <p>修复前（基线）的三重幻觉逐条对应用例：startService 只查内存记账——新代 Zoker 对
 * 运行中的孤儿必然双启（{@code testStartAfterRestartIdempotentAdopts}）；stopService 无条目
 * 即 not-running 幂等成功——实际进程还在跑（{@code testStopAfterRestartReallyStopsOrphan}）；
 * 进程身份完全不落盘（{@code testStartWritesRunPidIdentity}）。外加误杀守卫
 * （{@code testStopUnverifiedPidFileNotKilled}：指纹不符绝不按裸 pid 领养/误杀）与保留字
 * 扩展（{@code testCommitRunPidReserved}：versionNo=run.pid 变体占据容器根身份文件位置）。</p>
 *
 * <p>直构 ServiceManager（包内构造器）。真进程编排按 FND19 降级档：Windows 最小真进程形态
 * （ping 当保活睡眠器），非 Windows 跳过真进程用例；纯文件用例全平台可跑。</p>
 */
@Fast
@Extra
public class TestCrossRestartBehavior {
	private static final long COMMIT_FAIL = IModule.errorCode(Zoker.ModuleId, Zoker.eCommitFail);

	private static final boolean WINDOWS =
			System.getProperty("os.name", "").toLowerCase().contains("win");

	/** 盘上身份文件的物理名（钉住 on-disk 契约；不引修复侧常量，保持基线可编译）。 */
	private static final String RUN_PID = "run.pid";

	/** 字段注入（非方法参数）：@AfterEach 要先行自删。真进程（ping）与 AV/索引器对
	 * services/svc/v1 的瞬态目录句柄会让 JUnit 收尾的整树删除抛 DirectoryNotEmptyException
	 * ——"Failed to close extension context"红（2026-09-30 全量轮实证；test40-4 的
	 * TempDir 瞬态句柄族同款）。先行重试自删把瞬态窗口吃掉，JUnit 随后只删空根。 */
	@TempDir
	private Path tempDir;

	@AfterEach
	public void cleanupTempDir() throws InterruptedException {
		for (var i = 0; i < 5; i++) {
			deleteBestEffort(tempDir);
			if (!Files.exists(tempDir))
				return;
			Thread.sleep(200);
		}
		// 耗尽仍有残留（长持有者）：留给系统临时目录清理，不再让JUnit收尾红。
		deleteBestEffort(tempDir);
	}

	private static File servicesDir(Path tempDir) throws IOException {
		var f = tempDir.resolve("services").toFile();
		Files.createDirectories(f.toPath());
		return f;
	}

	/** 摆 services/svc/current → v1 布局（props 为 null 表示不写描述文件）。 */
	private static void layoutVersion(File servicesDir, String props) throws IOException {
		var svc = servicesDir.toPath().resolve("svc");
		var v1 = Files.createDirectories(svc.resolve("v1"));
		Files.writeString(svc.resolve(DistributeManager.CURRENT_NAME), "v1");
		if (null != props)
			Files.writeString(v1.resolve(ServiceManager.SERVICE_PROPERTIES_NAME), props);
	}

	private static Path runPidPath(File servicesDir) {
		return servicesDir.toPath().resolve("svc").resolve(RUN_PID);
	}

	/** 手工解析行式 key=value（修复前的基线没有 RunPidRecord，行为红形态不引新符号）。 */
	private static String lineValue(Path runPid, String key) throws IOException {
		for (var line : Files.readAllLines(runPid))
			if (line.startsWith(key + "="))
				return line.substring(key.length() + 1);
		return null;
	}

	private static StartService startReq() {
		var r = new StartService();
		r.Argument.setServiceName("svc");
		return r;
	}

	private static StopService stopReq(boolean force) {
		var r = new StopService();
		r.Argument.setServiceName("svc");
		r.Argument.setForce(force);
		return r;
	}

	private static String listState(ServiceManager sm) {
		var out = new ArrayList<BService.Data>();
		sm.listService(out);
		assertEquals(1, out.size());
		return out.get(0).getState();
	}

	// ---------- start 落盘身份（盘是真相源） ----------

	/** 启动成功即把进程身份写进容器根 services/svc/run.pid（与 current 同层）。修复前红点：文件不存在。 */
	@Test
	public void testStartWritesRunPidIdentity() throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping）");
		var servicesDir = servicesDir(tempDir);
		layoutVersion(servicesDir, "command=" + Procs.specJavaw() + "\nargs=" + Procs.specArgs("Nap", "60000") + "\n");
		var sm = new ServiceManager(servicesDir);

		assertEquals(0, sm.startService(startReq()));
		var spawned = sm.getProcessForTest("svc");
		assertNotNull(spawned);
		var runPid = runPidPath(servicesDir);
		assertTrue(Files.isRegularFile(runPid), "start 成功即落盘身份——盘是真相源");
		assertEquals(spawned.pid(), Long.parseLong(lineValue(runPid, "pid").trim()), "身份 pid=进程 pid");
		var start = lineValue(runPid, "start");
		assertNotNull(start);
		assertFalse(start.isEmpty(), "指纹必须记录 startInstant");
		var command = lineValue(runPid, "command");
		assertNotNull(command);
		// Windows 落盘命令行是规范化全路径（如 "C:\Windows\System32\PING.EXE ..."），大小写不敏感比对
		assertTrue(command.toLowerCase().contains("javaw"), "辅指纹=命令行: " + command);
		// 容器根与 current 同层：不随版本切换/清理消失（pruneVersions 只纳入目录）
		assertTrue(Files.isRegularFile(servicesDir.toPath().resolve("svc").resolve(DistributeManager.CURRENT_NAME)));

		sm.stopService(stopReq(true));
		assertFalse(spawned.isAlive());
	}

	/** 停毕条件删除盘上身份（残留不留给下次对账）。修复前红点：从未落盘。 */
	@Test
	public void testStopCleansRunPidAfterStop() throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping）");
		var servicesDir = servicesDir(tempDir);
		layoutVersion(servicesDir, "command=" + Procs.specJavaw() + "\nargs=" + Procs.specArgs("Nap", "60000") + "\n");
		var sm = new ServiceManager(servicesDir);
		assertEquals(0, sm.startService(startReq()));
		var spawned = sm.getProcessForTest("svc");
		assertNotNull(spawned);
		assertTrue(Files.isRegularFile(runPidPath(servicesDir)), "停前身份在盘");

		sm.stopService(stopReq(true));
		assertFalse(spawned.isAlive());
		assertFalse(Files.exists(runPidPath(servicesDir)), "停毕条件删除 run.pid");
	}

	// ---------- 跨 Zoker 重启（全新 ServiceManager=内存记账清零，盘上身份仍在） ----------

	/** stop 条目缺失先解析 run.pid 再判 not-running——领养句柄能真停孤儿。
	 * 修复前红点：Stopped/not-running 幂等谎言 + 原进程未被杀。 */
	@Test
	public void testStopAfterRestartReallyStopsOrphan() throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping）");
		var servicesDir = servicesDir(tempDir);
		layoutVersion(servicesDir, "command=" + Procs.specJavaw() + "\nargs=" + Procs.specArgs("Nap", "60000") + "\n");
		var sm1 = new ServiceManager(servicesDir);
		assertEquals(0, sm1.startService(startReq()));
		var spawned = sm1.getProcessForTest("svc");
		assertNotNull(spawned);

		// 模拟 Zoker 重启：全新 ServiceManager（另一张空账）
		var sm2 = new ServiceManager(servicesDir);
		var stop = stopReq(true);
		sm2.stopService(stop);
		assertEquals("svc", stop.Result.getServiceName());
		assertEquals(ServiceManager.STATE_FORCE_KILLED, stop.Result.getState(),
				"条目缺失≠not-running：领养句柄直接进入停机三态路径");
		assertFalse(spawned.isAlive(), "孤儿必须被真停");
		assertFalse(Files.exists(runPidPath(servicesDir)), "停毕清理盘上身份");
	}

	/** start 条目缺失先解析 run.pid 查重——存活且核实→领养幂等返回 Running（Ps 标记 adopted），
	 * 绝不盲目双启。修复前红点：无盘上身份→直接拉起第二个进程。 */
	@Test
	public void testStartAfterRestartIdempotentAdopts() throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping）");
		var servicesDir = servicesDir(tempDir);
		layoutVersion(servicesDir, "command=" + Procs.specJavaw() + "\nargs=" + Procs.specArgs("Nap", "60000") + "\n");
		var sm1 = new ServiceManager(servicesDir);
		assertEquals(0, sm1.startService(startReq()));
		var spawned = sm1.getProcessForTest("svc");
		assertNotNull(spawned);

		var sm2 = new ServiceManager(servicesDir);
		var r = startReq();
		assertEquals(0, sm2.startService(r));
		assertEquals("svc", r.Result.getServiceName());
		assertEquals(ServiceManager.STATE_RUNNING, r.Result.getState());
		assertTrue(r.Result.getPs().contains("adopted"),
				"领养返回必须 Ps 标记 adopted+pid: " + r.Result.getPs());
		var entry = sm2.getProcessForTest("svc");
		assertNotNull(entry);
		assertEquals(spawned.pid(), entry.pid(), "绝不双启：领养的是 run.pid 指认的原进程");
		assertTrue(spawned.isAlive(), "原进程不被误杀");
		assertEquals(spawned.pid(), Long.parseLong(lineValue(runPidPath(servicesDir), "pid").trim()),
				"盘上身份未被改写（无新拉起）");

		// 收殓：sm2 已领养，stop 经它闭环
		sm2.stopService(stopReq(true));
		assertFalse(spawned.isAlive());
	}

	// ---------- 误杀守卫（指纹不符绝不按裸 pid 领养/误杀） ----------

	/** run.pid 指认的活进程指纹不符（pid 被无关进程复用/陈旧文件）：stop 走 not-running
	 * 幂等，绝不杀未核实进程；不符残留就地清理。修复前红点：残留文件不清理。 */
	@Test
	public void testStopUnverifiedPidFileNotKilled() throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping）");
		var servicesDir = servicesDir(tempDir);
		Files.createDirectories(servicesDir.toPath().resolve("svc"));
		// 无关进程占位：活 ping，但盘上身份记录错误 startInstant（模拟 PID 复用后的陈旧文件）
		var stranger = new ProcessBuilder(Procs.command("Nap", "60000")).start();
		try {
			var runPid = runPidPath(servicesDir);
			Files.writeString(runPid,
					"pid=" + stranger.pid() + "\nstart=1999-01-01T00:00:00Z\ncommand=stale\n");
			var sm = new ServiceManager(servicesDir);
			var stop = stopReq(true);
			sm.stopService(stop);
			assertEquals(ServiceManager.STATE_STOPPED, stop.Result.getState());
			assertEquals("not-running", stop.Result.getPs());
			assertTrue(stranger.isAlive(), "指纹不符绝不按裸 pid 误杀");
			assertFalse(Files.exists(runPid), "不符残留必须清理（不留给下次对账）");
		} finally {
			stranger.destroyForcibly();
		}
	}

	// ---------- 保留字扩展（versionNo=run.pid 变体，GE-C01 同族） ----------

	/** versionNo 碰撞容器根身份文件固有位置（大小写/Win32 剥尾点空格变体）必须拒绝；
	 * 修复前红点：变体被接受，身份落盘恒失败→该服务一切 start 恒 eStartFail（无自愈）。 */
	@Test
	public void testCommitRunPidReserved() throws Exception {
		for (var versionNo : List.of("run.pid", "Run.Pid", "RUN.PID", "run.pid.", "run.pid ")) {
			var tag = Integer.toHexString(versionNo.hashCode());
			var distributeDir = tempDir.resolve("distributes-" + tag);
			var servicesDir = tempDir.resolve("services-" + tag);
			Files.createDirectories(distributeDir);
			Files.createDirectories(servicesDir);
			var svc = distributeDir.resolve("svc");
			Files.createDirectories(svc);
			Files.writeString(svc.resolve("app.jar"), "evil-" + versionNo);
			var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

			assertEquals(COMMIT_FAIL, dm.commit("svc", versionNo),
					"run.pid 变体必须被拒: '" + versionNo + "'");
			assertTrue(Files.isRegularFile(svc.resolve("app.jar")),
					"distributes 未被消费: '" + versionNo + "'");
			// 拒绝发生在一切副作用之前：容器目录不得创建（trailing-space 变体在 Windows 上
			// 无法经 java.nio 表达，按容器不创建统一断言）
			assertFalse(Files.exists(servicesDir.resolve("svc")),
					"services 容器不得创建: '" + versionNo + "'");
		}
		// 合法版本不受影响（拒绝面不扩大）
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		Files.createDirectories(servicesDir);
		var svc = distributeDir.resolve("svc");
		Files.createDirectories(svc);
		Files.writeString(svc.resolve("app.jar"), "hello-v1");
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());
		assertEquals(0, dm.commit("svc", "v1"), "正常版本不受保留字扩展影响");
		assertEquals("v1", Files.readString(servicesDir.resolve("svc").resolve(DistributeManager.CURRENT_NAME)));
	}
}
