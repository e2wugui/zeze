package Zeze.Services.ZokerImpl;

import harness.proc.Procs;
import harness.Extra;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND19 GE-D01：服务启停生命周期闭合（拍板方案A：service.properties 部署描述文件约定）。
 * 直构 ServiceManager（包内构造器，不依赖 Zoker 网络服务）。真进程编排按降级档用
 * Windows 最小真进程形态（ping 当保活睡眠器、cmd /c exit N 拿确定退出码），
 * 非Windows跳过真进程用例；纯解析/错误码用例全平台可跑。
 */
@Fast
@Extra
public class TestServiceLifecycle {
	private static final long NO_PROPS = IModule.errorCode(Zoker.ModuleId, Zoker.eNoServiceProperties);
	private static final long START_FAIL = IModule.errorCode(Zoker.ModuleId, Zoker.eStartFail);

	private static final boolean WINDOWS =
			System.getProperty("os.name", "").toLowerCase().contains("win");

	/** 字段注入（每方法新实例=每方法独立目录）：@AfterEach 先行兜底删除见 {@link TempDirBestEffort}。 */
	@TempDir
	Path tempDir;

	@AfterEach
	void cleanupTempDir() {
		TempDirBestEffort.delete(tempDir);
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

	// ---------- 描述文件缺失/不可用 → eNoServiceProperties（不再异常上抛超时） ----------

	@Test
	public void testNoDescriptorRejected() throws Exception {
		var servicesDir = servicesDir(tempDir);
		var sm = new ServiceManager(servicesDir);
		// 服务容器存在但从未 commit（无 current 指针）→ 描述文件必然缺失
		Files.createDirectories(servicesDir.toPath().resolve("svc"));
		assertEquals(NO_PROPS, sm.startService(startReq()));
		// current 在但描述文件缺失
		layoutVersion(servicesDir, null);
		assertEquals(NO_PROPS, sm.startService(startReq()));
		// 描述文件存在但缺 command 键（描述不完整=没有可用的部署描述）
		Files.writeString(servicesDir.toPath().resolve("svc").resolve("v1")
				.resolve(ServiceManager.SERVICE_PROPERTIES_NAME), "# no command here\n");
		assertEquals(NO_PROPS, sm.startService(startReq()));
	}

	/** 命令不存在 → 进程创建失败 eStartFail（区别于描述文件缺失的精确分类）。 */
	@Test
	public void testStartFailOnBadCommand() throws Exception {
		var servicesDir = servicesDir(tempDir);
		layoutVersion(servicesDir, "command=zeze-nonexistent-exe-0123456789");
		var sm = new ServiceManager(servicesDir);
		assertEquals(START_FAIL, sm.startService(startReq()));
	}

	// ---------- 命令解析（command/args/env），纯静态全平台 ----------

	@Test
	public void testParseLaunchSpecCommandOnly() throws Exception {
		var dir = Files.createDirectories(tempDir.resolve("v1"));
		Files.writeString(dir.resolve(ServiceManager.SERVICE_PROPERTIES_NAME), "command=java\n");
		var spec = ServiceManager.parseLaunchSpec(dir.toFile());
		assertEquals(dir.toFile(), spec.workingDir);
		assertEquals(java.util.List.of("java"), spec.command);
		assertTrue(spec.env.isEmpty());
	}

	@Test
	public void testParseLaunchSpecFull() throws Exception {
		var dir = Files.createDirectories(tempDir.resolve("v1"));
		// Properties格式：#注释、键值对；args空白分隔；env为k=v逗号分隔（值内可含空格）
		Files.writeString(dir.resolve(ServiceManager.SERVICE_PROPERTIES_NAME), """
				# deployment descriptor
				command=java
				args=-Xmx1g  -jar   app.jar
				env=LANG=C.UTF-8,FOO=bar baz
				""");
		var spec = ServiceManager.parseLaunchSpec(dir.toFile());
		assertEquals(java.util.List.of("java", "-Xmx1g", "-jar", "app.jar"), spec.command,
				"args空白分隔（多余空白收敛）");
		assertEquals(2, spec.env.size());
		assertEquals("C.UTF-8", spec.env.get("LANG"));
		assertEquals("bar baz", spec.env.get("FOO"), "env值内空格合法（逗号才是分隔符）");
	}

	@Test
	public void testParseLaunchSpecMalformed() throws Exception {
		var dir = Files.createDirectories(tempDir.resolve("v1"));
		// 文件缺失
		assertThrows(IOException.class, () -> ServiceManager.parseLaunchSpec(dir.toFile()));
		// command 为空
		Files.writeString(dir.resolve(ServiceManager.SERVICE_PROPERTIES_NAME), "args=-x\n");
		assertThrows(IOException.class, () -> ServiceManager.parseLaunchSpec(dir.toFile()));
		// env 条目缺 '='
		Files.writeString(dir.resolve(ServiceManager.SERVICE_PROPERTIES_NAME), "command=java\nenv=BADENTRY\n");
		assertThrows(IOException.class, () -> ServiceManager.parseLaunchSpec(dir.toFile()));
	}

	// ---------- 真进程编排（Windows 最小真进程形态；非Windows跳过） ----------

	/** 启动→Running→重复start幂等复用→stop三态结局→条目清除，全链路（GE-D01核心闭环）。 */
	@Test
	public void testStartListStopLifecycle() throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping/cmd）");
		var servicesDir = servicesDir(tempDir);
		layoutVersion(servicesDir, "command=" + Procs.specJavaw() + "\nargs=" + Procs.specArgs("Nap", "60000") + "\n");
		var sm = new ServiceManager(servicesDir);

		var r = startReq();
		assertEquals(0, sm.startService(r));
		assertEquals("svc", r.Result.getServiceName());
		assertEquals(ServiceManager.STATE_RUNNING, r.Result.getState());
		var process = sm.getProcessForTest("svc");
		assertNotNull(process);
		assertTrue(process.isAlive());
		assertEquals(ServiceManager.STATE_RUNNING, listState(sm), "listService 以 isAlive 判 Running");

		// 已运行再 start：幂等复用现役句柄，不重复拉起
		assertEquals(0, sm.startService(startReq()));
		assertSame(process, sm.getProcessForTest("svc"));

		// stop（非force）：Windows 不支持优雅终止必走强杀 → Force-Killed；Ps 带退出码
		var stop = stopReq(false);
		sm.stopService(stop);
		assertEquals("svc", stop.Result.getServiceName());
		assertTrue(ServiceManager.STATE_FORCE_KILLED.equals(stop.Result.getState())
						|| ServiceManager.STATE_STOPPED.equals(stop.Result.getState()),
				"stop结局必须是Force-Killed/Stopped之一: " + stop.Result.getState());
		assertTrue(stop.Result.getPs().contains("exit="), "Ps必须携带退出码: " + stop.Result.getPs());
		assertFalse(process.isAlive());
		assertNull(sm.getProcessForTest("svc"), "stop后条目清除");
		assertNotEquals(ServiceManager.STATE_RUNNING, listState(sm), "停机后不报running");
	}

	/** 死条目（onExit回调未及清理的窗口）：listService 不报 Running；再 start 替换重启。 */
	@Test
	public void testDeadHandleRestart() throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping/cmd）");
		var servicesDir = servicesDir(tempDir);
		layoutVersion(servicesDir, "command=" + Procs.specJavaw() + "\nargs=" + Procs.specArgs("Nap", "60000") + "\n");
		var sm = new ServiceManager(servicesDir);

		// 注入死句柄：cmd /c exit 7 自然退出（修复前 computeIfAbsent 命中死句柄报 running 且永不重启）
		var dead = new ProcessBuilder(Procs.command("Exit", "7")).start();
		assertTrue(dead.waitFor(10, TimeUnit.SECONDS));
		sm.putProcessForTest("svc", dead);

		assertEquals(ServiceManager.STATE_STOPPED, listState(sm), "条目在但进程死=Stopped不报running");
		assertEquals(0, sm.startService(startReq()), "对死条目再start必须能重新拉起");
		var relaunched = sm.getProcessForTest("svc");
		assertNotNull(relaunched);
		assertNotSame(dead, relaunched, "死句柄被替换");
		assertTrue(relaunched.isAlive());
		assertEquals(ServiceManager.STATE_RUNNING, listState(sm));

		// 清理
		sm.stopService(stopReq(true));
		assertFalse(relaunched.isAlive());
	}

	/** stop 死条目：结局 Stopped + 自然退出码（优雅退出语义的确定性验证）。 */
	@Test
	public void testStopDeadHandleReportsNaturalExitCode() throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（cmd）");
		var sm = new ServiceManager(servicesDir(tempDir));
		var dead = new ProcessBuilder(Procs.command("Exit", "7")).start();
		assertTrue(dead.waitFor(10, TimeUnit.SECONDS));
		sm.putProcessForTest("svc", dead);

		var stop = stopReq(false);
		sm.stopService(stop);
		assertEquals(ServiceManager.STATE_STOPPED, stop.Result.getState());
		assertEquals("exit=7", stop.Result.getPs());
		assertNull(sm.getProcessForTest("svc"));
	}

	/** 未运行就 stop（从未启动/已停止/已被onExit清理）：幂等成功。 */
	@Test
	public void testStopNotRunningIdempotent() throws Exception {
		var sm = new ServiceManager(servicesDir(tempDir));
		var stop = stopReq(true);
		sm.stopService(stop);
		assertEquals("svc", stop.Result.getServiceName());
		assertEquals(ServiceManager.STATE_STOPPED, stop.Result.getState());
		assertEquals("not-running", stop.Result.getPs());
	}

	/** 进程自然退出 → onExit 退出监控清理条目并记录退出码（死进程不占用 running 语义）。 */
	@Test
	public void testOnExitCleansUpEntry() throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（cmd）");
		var servicesDir = servicesDir(tempDir);
		// Nap 2000 当延迟自然退出（~2s）：Exit 毫秒级即死，onExit收殓跑赢下一行的
		// assertNotNull（test40-4实证round14红）——前提断言需要进程确定存活过断言时刻。
		layoutVersion(servicesDir, "command=" + Procs.specJavaw() + "\nargs=" + Procs.specArgs("Nap", "2000") + "\n");
		var sm = new ServiceManager(servicesDir);

		assertEquals(0, sm.startService(startReq()));
		assertNotNull(sm.getProcessForTest("svc"));
		// onExit 回调异步执行：有界轮询等待条目被清理
		var deadline = System.currentTimeMillis() + 10_000;
		while (null != sm.getProcessForTest("svc") && System.currentTimeMillis() < deadline)
			//noinspection BusyWait
			Thread.sleep(50);
		assertNull(sm.getProcessForTest("svc"), "退出监控必须清理processes条目");
		// 死条目清理后无记账：按 Stopped 汇报（zoker-11：空串超协议契约），不报 running
		assertEquals(ServiceManager.STATE_STOPPED, listState(sm), "死条目清理后不报running");
	}

	/** env 真注入进程环境（可观察形态：env 变量命中时 cmd 以退出码5退出）。 */
	@Test
	public void testEnvAppliedToProcess() throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（cmd）");
		var servicesDir = servicesDir(tempDir);
		// 前置 Nap 2000 延迟：裸 Exit 5 毫秒级即死，onExit收殓跑赢下一行的
		// assertNotNull（test40-4实证round20红）——需要条目在断言时刻确定在场以取句柄验退出码。
		layoutVersion(servicesDir, "command=" + Procs.specJavaw() + "\nargs="
				+ Procs.specArgs("NapEnv", "2000", "ZEZE_SVC_LIFECYCLE", "hit", "5") + "\nenv=ZEZE_SVC_LIFECYCLE=hit\n");
		var sm = new ServiceManager(servicesDir);

		assertEquals(0, sm.startService(startReq()));
		var process = sm.getProcessForTest("svc");
		assertNotNull(process);
		assertTrue(process.waitFor(10, TimeUnit.SECONDS));
		assertEquals(5, process.exitValue(), "env必须注入进程环境（ZEZE_SVC_LIFECYCLE=hit 命中 exit 5）");
	}
}
