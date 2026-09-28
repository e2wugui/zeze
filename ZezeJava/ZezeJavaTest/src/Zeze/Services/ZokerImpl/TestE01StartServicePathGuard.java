package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Builtin.Zoker.StartService;
import Zeze.IModule;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND20 GE-C01：startService 入口的 serviceName 单段校验（RCE 链断链）。
 * 直构 ServiceManager（包内构造器，不依赖 Zoker 网络服务）。
 * 机制链：serviceName 无校验时经 loadLaunchSpec→currentVersionDir 可把容器目录解析到
 * services/ 之外（".."逃逸），与 open 写入的 distributes 内容组合成完整 RCE 链——
 * {@link #testEscapeToDistributesRejected} 在 services 同级摆好攻击现场（distributes/evil/v1
 * 带可执行描述文件 + current 指针），修复后入口直接拒绝；修复前（红）该现场会被解析并启动。
 * 其余用例为守卫矩阵回归：非单段名（分隔符/盘符/./..）一律 eNoServiceProperties。
 */
@Fast
public class TestE01StartServicePathGuard {
	private static final long NO_PROPS = IModule.errorCode(Zoker.ModuleId, Zoker.eNoServiceProperties);

	private static File servicesDir(Path tempDir) throws IOException {
		var f = tempDir.resolve("services").toFile();
		Files.createDirectories(f.toPath());
		return f;
	}

	private static StartService startReq(String name) {
		var r = new StartService();
		r.Argument.setServiceName(name);
		return r;
	}

	/**
	 * 攻击现场（案卷GE-C01三步链的第1、2步产物）：distributes/evil/v1/service.properties
	 * （command=cmd /c exit 0，良性命令——红跑时即便逃逸成功也只是无害进程）+
	 * distributes/evil/current="v1"。第三步 StartService{serviceName="../distributes/evil"}
	 * 在修复后被 eNoServiceProperties 拒绝，services/ 内无任何落点。
	 */
	@Test
	public void testEscapeToDistributesRejected(@TempDir Path tempDir) throws Exception {
		var servicesDir = servicesDir(tempDir);
		// 摆 services 同级的 distributes 攻击现场（open 的 checkInsideDir 只限制在 distributes 之内，
		// 这些内容本来就是网络可达的可写面）
		var evil = tempDir.resolve("distributes").resolve("evil");
		Files.createDirectories(evil.resolve("v1"));
		Files.writeString(evil.resolve("v1").resolve(ServiceManager.SERVICE_PROPERTIES_NAME),
				"command=cmd\nargs=/c exit 0\n");
		Files.writeString(evil.resolve(DistributeManager.CURRENT_NAME), "v1");

		var sm = new ServiceManager(servicesDir);
		// 案卷原始逃逸形态
		assertEquals(NO_PROPS, sm.startService(startReq("../distributes/evil")),
				"逃逸到 distributes 的 serviceName 必须被入口拒绝");
		// 一般 .. 逃逸（解析到 services 之外的任意目录）
		assertEquals(NO_PROPS, sm.startService(startReq("../..")));
		// 修复后不触碰 services/（无探测落点残留）
		assertFalse(new File(servicesDir, "evil").exists());
	}

	/** 守卫矩阵：分隔符/盘符/./.. 非单段名拒绝（部分在修复前也碰巧无副作用，回归固化）。 */
	@Test
	public void testUnsafeSegmentMatrix(@TempDir Path tempDir) throws Exception {
		var sm = new ServiceManager(servicesDir(tempDir));
		for (var name : new String[]{"a/b", "a\\b", "C:\\x", "..", ".", ""})
			assertEquals(NO_PROPS, sm.startService(startReq(name)), "unsafe: '" + name + "'");
	}

	/** null serviceName：生成 bean 的 setter 本身拒绝 null（协议层不可达），isSafePathSegment 的 null 拒绝为纵深防御，不单独立案。 */

	/** 守卫不过度：合法单段名的正常布局仍按既有语义启动（Windows 最小真进程，对齐D01形态）。 */
	@Test
	public void testValidNameStillStarts(@TempDir Path tempDir) throws Exception {
		org.junit.jupiter.api.Assumptions.assumeTrue(
				System.getProperty("os.name", "").toLowerCase().contains("win"), "Windows 真进程用例");
		var servicesDir = servicesDir(tempDir);
		var svc = servicesDir.toPath().resolve("svc");
		Files.createDirectories(svc.resolve("v1"));
		Files.writeString(svc.resolve(DistributeManager.CURRENT_NAME), "v1");
		Files.writeString(svc.resolve("v1").resolve(ServiceManager.SERVICE_PROPERTIES_NAME),
				"command=cmd\nargs=/c exit 0\n");
		var sm = new ServiceManager(servicesDir);
		assertEquals(0, sm.startService(startReq("svc")));
		// 等进程退出（工作目录=版本目录，Windows 上存活期间锁住 @TempDir 无法清理）
		var process = sm.getProcessForTest("svc");
		if (null != process)
			assertTrue(process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS), "cmd 应快速退出");
	}
}
