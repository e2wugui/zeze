package Zeze.Services.ZokerImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.Zoker.StartService;
import harness.Fast;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND22 GE-C05：startService 拉起的子进程 stdout/stderr 管道从不读取也不重定向——子进程
 * 累计输出越过 OS 管道缓冲（~64KB）后 write 阻塞，服务静默冻结而 listService 恒 Running
 * （历轮短命测试进程从未越线故未暴露）。修复：launch() 把 stdout/stderr 重定向到
 * Redirect.DISCARD（零线程零 fd）；需要保留输出的部署在 command 自行重定向到文件。
 * 本测试拉起累计输出远超 64KB（~240KB）的短命子进程（cmd for 循环 echo），断言它能自行
 * 退出——修复前默认 PIPE 无人消费，写满 64KB 后阻塞、进程永不退出（waitFor 有界超时防测试
 * 自身挂死，finally 强杀收殓）。真进程用例 Windows 形态（FND19 降级档）。
 */
@Fast
public class TestE05SubprocessOutputNoFreeze {
	private static final boolean WINDOWS =
			System.getProperty("os.name", "").toLowerCase().contains("win");

	private static void layoutBigOutputService(Path tempDir) throws IOException {
		var servicesDir = tempDir.resolve("services");
		var v1 = Files.createDirectories(servicesDir.resolve("svc").resolve("v1"));
		Files.writeString(servicesDir.resolve("svc").resolve(DistributeManager.CURRENT_NAME), "v1");
		// 3000 行 × 81 字节（79 字符 + CRLF）≈ 240KB，约为 64KB 管道缓冲的 3.8 倍；
		// for 循环写完即退出——输出的持续产生+自行退出正是"无人读管道"的探针形态。
		Files.writeString(v1.resolve(ServiceManager.SERVICE_PROPERTIES_NAME),
				"command=cmd\n"
						+ "args=/c for /l %i in (1,1,3000) do @echo "
						+ "0123456789012345678901234567890123456789012345678901234567890123456789012345678\n");
	}

	private static StartService startReq() {
		var r = new StartService();
		r.Argument.setServiceName("svc");
		return r;
	}

	/** 核心红点：输出跨 64KB 的子进程必须能自行退出（管道丢弃不阻塞）。修复前：waitFor
	 * 超时（cmd 阻塞在自己的 WriteFile 上，进程活着不推进）。 */
	@Test
	public void testBigOutputSubprocessCompletes(@TempDir Path tempDir) throws Exception {
		Assumptions.assumeTrue(WINDOWS, "长输出子进程形态为Windows命令（cmd for 循环 echo）");
		layoutBigOutputService(tempDir);
		var sm = new ServiceManager(tempDir.resolve("services").toFile());

		assertEquals(0, sm.startService(startReq()));
		var entry = sm.getProcessForTest("svc");
		assertNotNull(entry);
		try {
			assertTrue(entry.waitFor(30, TimeUnit.SECONDS),
					"输出越过管道缓冲的子进程必须能自行退出（输出被丢弃，write 不阻塞）");
		} finally {
			entry.destroyForcibly(); // 已退出时为无害幂等；红路径防测试残留
		}

		// 退出后记账与盘上身份正常收殓（watchExit 条件移除+条件删 run.pid）
		for (var i = 0; i < 100 && null != sm.getProcessForTest("svc"); i++)
			Thread.sleep(50);
		assertNull(sm.getProcessForTest("svc"), "子进程退出后记账条目收殓");
		var runPid = tempDir.resolve("services").resolve("svc").resolve(ServiceManager.RUN_PID_NAME);
		for (var i = 0; i < 100 && Files.exists(runPid); i++)
			Thread.sleep(50);
		assertTrue(!Files.exists(runPid), "退出后盘上身份条件删除");
	}

	/** 正常短输出进程的启停语义不受重定向影响（DISCARD 不破坏生命周期契约，回归钉）。 */
	@Test
	public void testNormalLifecycleUnaffected(@TempDir Path tempDir) throws Exception {
		Assumptions.assumeTrue(WINDOWS, "最小真进程形态为Windows命令（ping）");
		var servicesDir = tempDir.resolve("services");
		var v1 = Files.createDirectories(servicesDir.resolve("svc").resolve("v1"));
		Files.writeString(servicesDir.resolve("svc").resolve(DistributeManager.CURRENT_NAME), "v1");
		Files.writeString(v1.resolve(ServiceManager.SERVICE_PROPERTIES_NAME),
				"command=ping\nargs=-n 30 127.0.0.1\n");
		var sm = new ServiceManager(servicesDir.toFile());

		assertEquals(0, sm.startService(startReq()));
		var entry = sm.getProcessForTest("svc");
		assertNotNull(entry);
		assertTrue(entry.isAlive());
		entry.destroyForcibly();
		assertTrue(entry.waitFor(10, TimeUnit.SECONDS));
	}
}
