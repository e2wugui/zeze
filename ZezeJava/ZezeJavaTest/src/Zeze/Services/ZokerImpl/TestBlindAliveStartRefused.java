package Zeze.Services.ZokerImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Builtin.Zoker.StartService;
import Zeze.IModule;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND24 zoker-10 守卫：run.pid 失明（指纹不可核实）且 pid 存活时 startService 拒绝启动。
 * 修复前：失明=返回 null 照常走"无可领养身份"分支拉新进程——旧 pid 未处置，同服务双实例
 * 并跑（端口冲突/数据竞争，start"成功"的静默错账）。修复：RunPidState 三态带出 blindAlivePid，
 * startService 拒绝（eStartFail+error 日志附 pid），状态零变更（不装账、不拉起、文件保留）——
 * 人工处置旧 pid 或其死亡后重试自愈。
 * 摆盘：run.pid 的 start 行空串（写入时 startInstant 不可得的失明形态）、pid=本测试 JVM
 * （存活但指纹不可核实，全平台确定性）。修复前红点：返回 0 且 processes 装账（双实例错账本体）。
 */
@Fast
public class TestBlindAliveStartRefused {

	private static final long START_FAIL = IModule.errorCode(Zoker.ModuleId, Zoker.eStartFail);

	/** 跨平台可用命令（java.home 定位）：使修复路径若误走拉起分支必然成功，红点不被 eNoServiceProperties 掩盖。 */
	private static String javaBin() {
		return Path.of(System.getProperty("java.home"), "bin",
				System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java").toString();
	}

	/** 核心红点：失明存活=拒绝启动（eStartFail）+ 状态零变更（无装账、run.pid 保留）。 */
	@Test
	public void testBlindAlivePidRefusesStart(@TempDir Path tempDir) throws Exception {
		var servicesDir = tempDir.resolve("services");
		var svc = Files.createDirectories(servicesDir.resolve("svc"));
		var v1 = Files.createDirectories(svc.resolve("v1"));
		Files.writeString(svc.resolve(DistributeManager.CURRENT_NAME), "v1");
		Files.writeString(v1.resolve(ServiceManager.SERVICE_PROPERTIES_NAME), "command=" + javaBin() + "\nargs=-version\n");

		// 失明身份：start 行空串（写入时不可得）+ pid 存活（本测试 JVM）
		ServiceManager.writeRunPid(svc.toFile(), new ServiceManager.RunPidRecord(
				ProcessHandle.current().pid(), "", "evidence-command", "v1"));

		var sm = new ServiceManager(servicesDir.toFile());
		var r = new StartService();
		r.Argument.setServiceName("svc");
		assertEquals(START_FAIL, sm.startService(r), "失明存活必须拒绝启动（拉新=同服务双实例）");

		assertNull(sm.getProcessForTest("svc"), "拒绝=状态零变更：不装账");
		assertTrue(Files.exists(svc.resolve(ServiceManager.RUN_PID_NAME)),
				"拒绝=状态零变更：run.pid 保留（证据留给人工，处置入口=start 的拒绝日志）");
	}
}
