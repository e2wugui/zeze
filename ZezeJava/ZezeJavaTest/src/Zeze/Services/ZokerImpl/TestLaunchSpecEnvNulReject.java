package Zeze.Services.ZokerImpl;

import harness.Extra;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import Zeze.Builtin.Zoker.StartService;
import Zeze.IModule;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FND29 zoker-03 守卫：env 键/值含 NUL 的部署描述在解析期按非法条目拒绝。
 * 修复前：parseLaunchSpec 原样保留 NUL，泄漏到 launch 的 pb.environment().putAll——
 * 行为随 JDK 漂移：校验 NUL 的 JDK（本机 Temurin 26 实证）抛 IllegalArgumentException
 * 逃逸出 startServiceLocked 的 IOException-catch，协议层无结果包，客户端等满 60s 超时
 * 且错误分类全失（违背 Zoker.java StartService"失败映射协议错误码，不异常上抛"契约）；
 * 不校验的 JDK（本仓测试 JVM Adoptium 25，红阶段实测）NUL 静默通过 putAll 进入子进程
 * 环境块（静默损坏）。修复：解析期拒绝（IOException→eNoServiceProperties，归入既有
 * "env 条目非法"类别），错误位置精确到条目，两种 JDK 形态都闭合。
 * 纯文件直构（parseLaunchSpec 包内静态）+ServiceManager 直构，全平台确定性；
 * 修复前红点：解析不抛（NUL 入 spec.env）；startService 红形态随 JDK（IAE 逃逸或 eStartFail）。
 */
@Fast
@Extra
public class TestLaunchSpecEnvNulReject {

	private static final long NO_SERVICE_PROPERTIES = IModule.errorCode(Zoker.ModuleId, Zoker.eNoServiceProperties);

	private static ServiceManager.LaunchSpec parse(Path versionDir, String content) throws IOException {
		Files.createDirectories(versionDir);
		Files.writeString(versionDir.resolve(ServiceManager.SERVICE_PROPERTIES_NAME), content);
		return ServiceManager.parseLaunchSpec(versionDir.toFile());
	}

	/** 核心红点：值嵌入 NUL——解析期必须拒绝（修复前原样入 spec.env，launch 期 IAE）。 */
	@Test
	public void testEnvValueNulRejectedAtParse(@TempDir Path tempDir) throws Exception {
		assertThrows(IOException.class, () -> parse(tempDir.resolve("v1"), "command=ping\nenv=A=B\u0000C\n"),
				"含 NUL 的 env 值必须在解析期拒绝");
	}

	/** 键嵌入 NUL 同样拒绝：trim 只剥两端，嵌入 NUL 原样保留才是真实到达 putAll 的形态。 */
	@Test
	public void testEnvKeyNulRejectedAtParse(@TempDir Path tempDir) throws Exception {
		assertThrows(IOException.class, () -> parse(tempDir.resolve("v1"), "command=ping\nenv=K\u0000K2=V\n"),
				"含 NUL 的 env 键必须在解析期拒绝");
	}

	/** 全链路契约：startService 收到协议错误码（eNoServiceProperties）而非逃逸/静默注入
	 * （IAE 逃逸=无结果包=客户端等满 60s 超时）。两种红形态都无进程副作用：
	 * IAE 在 pb.start 之前抛出；不校验的 JDK 下命令不存在使 start 即 IOException。 */
	@Test
	public void testStartServiceEnvNulMapsErrorCode(@TempDir Path tempDir) throws Exception {
		var servicesDir = tempDir.resolve("services");
		var v1 = Files.createDirectories(servicesDir.resolve("svc").resolve("v1"));
		Files.writeString(servicesDir.resolve("svc").resolve(DistributeManager.CURRENT_NAME), "v1");
		Files.writeString(v1.resolve(ServiceManager.SERVICE_PROPERTIES_NAME),
				"command=no-such-command\nenv=A=B\u0000C\n");

		var sm = new ServiceManager(servicesDir.toFile());
		var r = new StartService();
		r.Argument.setServiceName("svc");
		assertEquals(NO_SERVICE_PROPERTIES, sm.startService(r),
				"env 含 NUL 必须映射 eNoServiceProperties（修复前 IAE 逃逸，无结果包=客户端超时）");
		assertNull(sm.getProcessForTest("svc"), "失败无副作用：不装账");
	}

	/** 回归钉：值内 '=' 合法（首个'='定键）——NUL 校验不得收紧既有语义。 */
	@Test
	public void testEnvValueEqualsStillParses(@TempDir Path tempDir) throws Exception {
		var spec = parse(tempDir.resolve("v1"), "command=ping\nenv=A=B=C\n");
		assertEquals(Map.of("A", "B=C"), spec.env);
	}
}
