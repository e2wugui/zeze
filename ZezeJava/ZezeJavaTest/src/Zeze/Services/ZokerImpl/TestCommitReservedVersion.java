package Zeze.Services.ZokerImpl;

import harness.Extra;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.IModule;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND20 GE-C02：commit 的 versionNo 保留字排除。
 * services/&lt;svc&gt;/current 同时是现役指针文件的固有位置：versionNo="current" 且指针尚不存在
 * （首次部署常态）时，版本目录 rename 会占据该位置，此后该服务的一切 commit 恒失败
 * （AtomicFileWriter 无法原子 rename 到目录上）、currentVersionDir 恒 null、prune 永远
 * 执行不到——需人工删目录。修复后在参数校验处直接拒绝；修复前（红）第一次 commit 即把
 * services/svc/current 变成目录，后续正常 commit（testRecoveryAfterRejected）永久卡死。
 * 直构 DistributeManager（包内构造器），纯文件逻辑。
 */
@Fast
@Extra
public class TestCommitReservedVersion {
	private static final long COMMIT_FAIL = IModule.errorCode(Zoker.ModuleId, Zoker.eCommitFail);

	private static void upload(Path distributeDir, String serviceName, String content) throws IOException {
		var svc = distributeDir.resolve(serviceName);
		Files.createDirectories(svc);
		Files.writeString(svc.resolve("app.jar"), content);
	}

	/** 拒绝保留字：错误码 + 现场零残留（连 services/&lt;svc&gt; 容器都不创建）。 */
	@Test
	public void testReservedVersionRejectedNoFootprint(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		Files.createDirectories(servicesDir);
		upload(distributeDir, "svc", "evil-current");
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		assertEquals(COMMIT_FAIL, dm.commit("svc", DistributeManager.CURRENT_NAME));
		// 拒绝发生在一切副作用之前：distributes 未被消费、services/svc 未被创建
		assertTrue(Files.isRegularFile(distributeDir.resolve("svc").resolve("app.jar")));
		assertFalse(Files.exists(servicesDir.resolve("svc")));
	}

	/**
	 * 卡死链断链（红的判别核心）：被拒绝后同服务的正常 commit 必须成功。
	 * 修复前：services/svc/current 已被目录占位，commit("svc","v1") 的 switchCurrent 必失败。
	 */
	@Test
	public void testNormalCommitRecoverableAfterReservedRejected(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		Files.createDirectories(servicesDir);
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		upload(distributeDir, "svc", "x");
		assertEquals(COMMIT_FAIL, dm.commit("svc", DistributeManager.CURRENT_NAME));
		// 修复后 current 位置未被目录占据；修复前此处已是目录（red：下一行 commit != 0）
		assertFalse(Files.isDirectory(servicesDir.resolve("svc").resolve(DistributeManager.CURRENT_NAME)),
				"current 位置不得被版本目录占据");

		upload(distributeDir, "svc", "hello-v1");
		assertEquals(0, dm.commit("svc", "v1"), "被拒后正常 commit 必须成功（卡死链已断）");
		assertEquals("v1", Files.readString(servicesDir.resolve("svc").resolve(DistributeManager.CURRENT_NAME)));
		assertEquals("hello-v1", Files.readString(servicesDir.resolve("svc").resolve("v1").resolve("app.jar")));
	}
}
