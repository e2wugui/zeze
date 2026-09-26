package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.Zoker.BService;
import Zeze.Builtin.Zoker.StartService;
import Zeze.Builtin.Zoker.StopService;
import harness.Fast;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND21 GE-D01（方案A）：启动对账领养（adoptOrphans）与身份解析（resolveRunPid）的
 * 新机制用例——覆盖 Zoker.start() listen 前扫描的领养门槛矩阵：
 * <ul>
 * <li>核实通过（pid 存活+startInstant/command 相符）→ 领养装账（AdoptedProcess）+挂 onExit，
 * list 可见、stop 可停（核心闭环）；</li>
 * <li>死/损坏/指纹不符 → 残留就地清理（对账收敛一切残局），绝不领养、绝不误杀；</li>
 * <li>指纹不可核实（盘上 start 空）→ 失明：告警不领养，文件保留（证据留给人工，与清理面区分）。</li>
 * </ul>
 * 外加容器根身份文件不在版本清理面（pruneVersions 只纳入目录）的布局回归钉。
 * 本类引用修复侧新符号（adoptOrphans/RunPidRecord/writeRunPid/AdoptedProcess），
 * 基线行为红由 {@link TestFnd21GED01CrossRestartBehavior}（零新符号）承担。
 * 直构 ServiceManager；真进程用例 Windows 形态（FND19 降级档），纯文件用例全平台。
 */
@Fast
public class TestFnd21GED01OrphanAdoptionScan {
	private static final boolean WINDOWS =
			System.getProperty("os.name", "").toLowerCase().contains("win");

	private static File servicesDir(Path tempDir) throws IOException {
		var f = tempDir.resolve("services").toFile();
		Files.createDirectories(f.toPath());
		return f;
	}

	private static void layoutVersion(File servicesDir, String props) throws IOException {
		var svc = servicesDir.toPath().resolve("svc");
		var v1 = Files.createDirectories(svc.resolve("v1"));
		Files.writeString(svc.resolve(DistributeManager.CURRENT_NAME), "v1");
		Files.writeString(v1.resolve(ServiceManager.SERVICE_PROPERTIES_NAME), props);
	}

	private static Path runPidPath(File servicesDir) {
		return servicesDir.toPath().resolve("svc").resolve(ServiceManager.RUN_PID_NAME);
	}

	private static File svcContainer(File servicesDir) throws IOException {
		var dir = servicesDir.toPath().resolve("svc");
		Files.createDirectories(dir);
		return dir.toFile();
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

	/** 跨进程可移植的短命真进程（java.home 定位，不依赖 PATH）。 */
	private static long deadProcessPid() throws Exception {
		var javaBin = Path.of(System.getProperty("java.home"), "bin", WINDOWS ? "java.exe" : "java");
		var shortLived = new ProcessBuilder(javaBin.toString(), "-version").start();
		assertTrue(shortLived.waitFor(60, TimeUnit.SECONDS));
		return shortLived.pid();
	}

	/** 核心闭环：Zoker 重启后启动扫描领养活孤儿——list 可见（Ps 标记 adopted）、stop 真停、
	 * 停毕条件删除盘上身份。 */
	@Test
	public void testScanAdoptsLiveOrphan(@TempDir Path tempDir) throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping）");
		var servicesDir = servicesDir(tempDir);
		layoutVersion(servicesDir, "command=ping\nargs=-n 60 127.0.0.1\n");
		var sm1 = new ServiceManager(servicesDir);
		assertEquals(0, sm1.startService(startReq()));
		var spawned = sm1.getProcessForTest("svc");
		assertNotNull(spawned);

		// 模拟 Zoker 重启：全新 ServiceManager（内存记账清零），盘上身份仍在
		var sm2 = new ServiceManager(servicesDir);
		assertEquals("", listState(sm2), "对账前=旧世界幻觉（孤儿不可见）");
		sm2.adoptOrphans();

		assertEquals(ServiceManager.STATE_RUNNING, listState(sm2), "启动对账后孤儿可见");
		var entry = sm2.getProcessForTest("svc");
		assertNotNull(entry);
		assertTrue(entry instanceof ServiceManager.AdoptedProcess, "领养形态装账（AdoptedProcess）");
		assertEquals(spawned.pid(), entry.pid(), "领养的必须是 run.pid 指认的原进程");
		assertTrue(entry.isAlive());

		var out = new ArrayList<BService.Data>();
		sm2.listService(out);
		assertEquals(1, out.size());
		assertTrue(out.get(0).getPs().contains("adopted"),
				"list 的 Ps 标记 adopted+pid: " + out.get(0).getPs());

		// stop 能真停（不再 not-running 幂等谎言）
		var stop = stopReq(true);
		sm2.stopService(stop);
		assertEquals(ServiceManager.STATE_FORCE_KILLED, stop.Result.getState());
		assertTrue(stop.Result.getPs().contains("pid="), "领养句柄 Ps 以 pid 表达（无 exitValue）: "
				+ stop.Result.getPs());
		assertFalse(spawned.isAlive(), "领养句柄必须能真杀原进程");
		assertFalse(Files.exists(runPidPath(servicesDir)), "停毕条件删除盘上身份");
	}

	/** start 条目缺失的领养查重走同一身份解析：装账形态为 AdoptedProcess（类型级验证）。 */
	@Test
	public void testStartAdoptsIntoAdoptedProcessEntry(@TempDir Path tempDir) throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping）");
		var servicesDir = servicesDir(tempDir);
		layoutVersion(servicesDir, "command=ping\nargs=-n 60 127.0.0.1\n");
		var sm1 = new ServiceManager(servicesDir);
		assertEquals(0, sm1.startService(startReq()));
		var spawned = sm1.getProcessForTest("svc");
		assertNotNull(spawned);

		var sm2 = new ServiceManager(servicesDir);
		assertEquals(0, sm2.startService(startReq()));
		var entry = sm2.getProcessForTest("svc");
		assertNotNull(entry);
		assertTrue(entry instanceof ServiceManager.AdoptedProcess, "start 查重领养装账");
		assertEquals(spawned.pid(), entry.pid(), "绝不双启");
		assertTrue(spawned.isAlive());

		sm2.stopService(stopReq(true));
		assertFalse(spawned.isAlive());
	}

	/** 死 pid 残留：对账清理文件、不领养（全平台，java.home 定位短命进程）。 */
	@Test
	public void testScanCleansDeadResidue(@TempDir Path tempDir) throws Exception {
		var servicesDir = servicesDir(tempDir);
		var container = svcContainer(servicesDir);
		ServiceManager.writeRunPid(container,
				new ServiceManager.RunPidRecord(deadProcessPid(), "2020-01-01T00:00:00Z", "gone"));

		var sm = new ServiceManager(servicesDir);
		sm.adoptOrphans();
		assertNull(sm.getProcessForTest("svc"), "死 pid 不领养");
		assertFalse(Files.exists(runPidPath(servicesDir)), "死 pid 残留清理（对账收敛残局）");
		assertEquals("", listState(sm));
	}

	/** 损坏文件（不可解析）：对账清理、不领养（全平台纯文件用例）。 */
	@Test
	public void testScanCleansCorruptResidue(@TempDir Path tempDir) throws Exception {
		var servicesDir = servicesDir(tempDir);
		svcContainer(servicesDir); // 先建容器目录再摆损坏文件
		Files.writeString(runPidPath(servicesDir), "this is not a pid file\nno key value\n");
		var sm = new ServiceManager(servicesDir);
		sm.adoptOrphans();
		assertNull(sm.getProcessForTest("svc"), "损坏身份不领养");
		assertFalse(Files.exists(runPidPath(servicesDir)), "损坏残留清理");
	}

	/** 指纹不符（活 pid 但 startInstant 不符=PID 复用）：不领养、不误杀、残留清理。 */
	@Test
	public void testScanMismatchNotAdoptedNotKilled(@TempDir Path tempDir) throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping）");
		var servicesDir = servicesDir(tempDir);
		var container = svcContainer(servicesDir);
		var stranger = new ProcessBuilder("ping", "-n", "60", "127.0.0.1").start();
		try {
			// 记录 stranger 的 pid 但 startInstant 是错的——同 pid 已是另一个进程实例
			ServiceManager.writeRunPid(container,
					new ServiceManager.RunPidRecord(stranger.pid(), "1999-01-01T00:00:00Z", "stale"));
			var sm = new ServiceManager(servicesDir);
			sm.adoptOrphans();
			assertNull(sm.getProcessForTest("svc"), "指纹不符绝不领养（宁可失明不误杀）");
			assertFalse(Files.exists(runPidPath(servicesDir)), "不符残留清理");
			assertTrue(stranger.isAlive(), "指纹不符绝不误杀");
		} finally {
			stranger.destroyForcibly();
		}
	}

	/** 指纹不可核实（盘上 start 为空）：失明告警不领养，但文件保留（证据），进程不误杀——
	 * 失明与清理是两个不同的处置面。 */
	@Test
	public void testScanUnverifiableBlindKeepsFile(@TempDir Path tempDir) throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping）");
		var servicesDir = servicesDir(tempDir);
		var container = svcContainer(servicesDir);
		var stranger = new ProcessBuilder("ping", "-n", "60", "127.0.0.1").start();
		try {
			ServiceManager.writeRunPid(container, new ServiceManager.RunPidRecord(stranger.pid(), "", ""));
			var sm = new ServiceManager(servicesDir);
			sm.adoptOrphans();
			assertNull(sm.getProcessForTest("svc"), "指纹不可核实=失明不领养（绝不按裸 pid 领养）");
			assertTrue(Files.exists(runPidPath(servicesDir)),
					"失明≠清理：文件保留（证据留给人工；下次 start 原子覆盖）");
			assertTrue(stranger.isAlive(), "失明不误杀");
		} finally {
			stranger.destroyForcibly();
		}
	}

	/** command（辅证据）不符不否决领养：pid+startInstant 已核实同一进程实例
	 * （startInstant 是判别门——exec 链下创建时间不变而 command 变，否决会复活双启主缺陷）。 */
	@Test
	public void testCommandMismatchDoesNotVetoAdoption(@TempDir Path tempDir) throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping）");
		var servicesDir = servicesDir(tempDir);
		var container = svcContainer(servicesDir);
		var stranger = new ProcessBuilder("ping", "-n", "60", "127.0.0.1").start();
		try {
			// 记录正确的 pid+startInstant（身份核实通过），但 command 写错（模拟 exec 链换命令行）
			var liveStart = ProcessHandle.of(stranger.pid()).orElseThrow()
					.info().startInstant().map(Object::toString).orElse("");
			assertFalse(liveStart.isEmpty());
			ServiceManager.writeRunPid(container,
					new ServiceManager.RunPidRecord(stranger.pid(), liveStart, "totally-different-command"));
			var sm = new ServiceManager(servicesDir);
			sm.adoptOrphans();
			var entry = sm.getProcessForTest("svc");
			assertNotNull(entry, "startInstant 核实通过即领养（command 只告警不否决）");
			assertTrue(entry instanceof ServiceManager.AdoptedProcess);
			assertEquals(stranger.pid(), entry.pid());
			assertTrue(stranger.isAlive());
		} finally {
			stranger.destroyForcibly();
		}
	}

	/** 布局回归钉：容器根 run.pid（与 current 指针）不在版本保留策略清理面
	 * （pruneVersions 只纳入目录，设计引证 DistributeManager:369-371）。 */
	@Test
	public void testPruneVersionsKeepsRunPid(@TempDir Path tempDir) throws Exception {
		var servicesDir = servicesDir(tempDir);
		var svcDir = servicesDir.toPath().resolve("svc");
		Files.createDirectories(svcDir.resolve("v1"));
		Files.createDirectories(svcDir.resolve("v2"));
		Files.writeString(svcDir.resolve(DistributeManager.CURRENT_NAME), "v2");
		ServiceManager.writeRunPid(svcDir.toFile(), new ServiceManager.RunPidRecord(12345L, "x", "y"));

		var dm = new DistributeManager(tempDir.resolve("distributes").toFile(), servicesDir);
		dm.setKeepVersions(1);
		dm.pruneVersions(svcDir.toFile(), "v2");

		assertFalse(Files.exists(svcDir.resolve("v1")), "最老非现役版本清理");
		assertTrue(Files.isDirectory(svcDir.resolve("v2")), "现役版本保留");
		assertTrue(Files.isRegularFile(svcDir.resolve(DistributeManager.CURRENT_NAME)), "current 指针保留");
		assertTrue(Files.isRegularFile(runPidPath(servicesDir)), "容器根身份文件不在版本清理面");
	}
}
