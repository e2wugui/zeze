package Zeze.Services.ZokerImpl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import Zeze.IModule;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND26 zoker-05 回归：commit 的集合级完整性屏障（清单 manifest）。
 * 文件级md5（CloseFile）与目录级搬运（rename）之间缺"本次发布集合已完整"的事实——
 * 修复前：open预建空目录、上传中断的部分集合都可整目录成版并切current，start从坏版本
 * 启动（NoClassDefFound）或恒eNoServiceProperties。
 * 屏障=部署方在全部文件收口后补传的 {@link DistributeManager#DISTRIBUTE_MANIFEST_NAME}
 *（行=文件相对distributeDir根的路径，与OpenFile寻址同根）：清单存在即声明集合完整，
 * commit校验齐全+清退清单外残留+空目录拒绝；无清单走legacy路径（既有行为不变）。
 */
@Fast
public class TestD05CommitManifestBarrier {
	private static final long COMMIT_FAIL = IModule.errorCode(Zoker.ModuleId, Zoker.eCommitFail);

	/** 在 distributes/<svc> 下放置文件并写集合清单（行含服务名前缀，与ZokerAgent同构）。 */
	private static void uploadWithManifest(File distributeDir, String serviceName,
											List<String> files, List<String> manifestLines) throws Exception {
		var svc = distributeDir.toPath().resolve(serviceName);
		Files.createDirectories(svc);
		for (var rel : files) {
			var p = svc.resolve(rel);
			Files.createDirectories(p.getParent());
			Files.writeString(p, "content-of-" + rel);
		}
		if (null != manifestLines) {
			var lines = new StringBuilder();
			for (var line : manifestLines)
				lines.append(serviceName).append('/').append(line).append('\n');
			Files.writeString(svc.resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME), lines.toString());
		}
	}

	/** 空目录拒绝：open路径预建的 distributes/<svc> 空目录（上传被拒/中断后残留）不得成版。 */
	@Test
	public void testEmptyDistributeDirRejected(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		Files.createDirectories(distributeDir.toPath().resolve("svc").getParent());
		Files.createDirectories(distributeDir.toPath().resolve("svc")); // open预建形态
		var dm = new DistributeManager(distributeDir, servicesDir);

		assertEquals(COMMIT_FAIL, dm.commit("svc", "v1"),
				"空目录必须拒绝（修复前整目录成版并切current，start恒eNoServiceProperties）");
		assertFalse(new File(servicesDir, "svc").exists(), "失败无副作用：不建容器目录");
	}

	/** 清单缺文件=部分集合：拒绝成版（修复前部分集合成版，start从坏版本启动）。 */
	@Test
	public void testManifestMissingEntryRejected(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		uploadWithManifest(distributeDir, "svc",
				List.of("app.jar", "lib/x.jar"),
				List.of("app.jar", "lib/never-uploaded.jar"));
		var dm = new DistributeManager(distributeDir, servicesDir);

		assertEquals(COMMIT_FAIL, dm.commit("svc", "v1"), "清单内文件缺失必须拒绝（部分集合不得成版）");
		assertFalse(new File(servicesDir, "svc").exists(), "失败无副作用");
	}

	/** 清单外残留清退：版本内容=清单声明的精确集合（前次中断部署的残留不混入）。 */
	@Test
	public void testManifestPrunesUnlistedResidue(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		uploadWithManifest(distributeDir, "svc",
				List.of("app.jar", "lib/x.jar", "stale-old-deploy.jar"),
				List.of("app.jar", "lib/x.jar"));
		var dm = new DistributeManager(distributeDir, servicesDir);

		assertEquals(0, dm.commit("svc", "v1"));
		var version = Path.of(servicesDir.getPath(), "svc", "v1");
		assertTrue(Files.isRegularFile(version.resolve("app.jar")), "清单内文件随版本成版");
		assertTrue(Files.isRegularFile(version.resolve("lib/x.jar")), "子目录文件随版本成版");
		assertFalse(Files.exists(version.resolve("stale-old-deploy.jar")),
				"清单外残留必须清退（混入即新旧混合的部分集合形态）");
		assertTrue(Files.isRegularFile(version.resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME)),
				"清单自身随版本目录保留（无消费者，仅存档）");
		assertEquals("v1", Files.readString(Path.of(servicesDir.getPath(), "svc", "current")));
	}

	/** 清单齐全：正常成版（含子目录），行为与legacy成功路径一致。 */
	@Test
	public void testManifestCompleteCommits(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		uploadWithManifest(distributeDir, "svc", List.of("app.jar", "lib/x.jar"),
				List.of("app.jar", "lib/x.jar"));
		var dm = new DistributeManager(distributeDir, servicesDir);

		assertEquals(0, dm.commit("svc", "v1"));
		assertEquals("content-of-app.jar", Files.readString(Path.of(servicesDir.getPath(), "svc", "v1", "app.jar")));
		assertEquals("v1", Files.readString(Path.of(servicesDir.getPath(), "svc", "current")));
	}

	/** legacy路径：无清单（外部部署工具/直构形态）不设障，既有行为不变。 */
	@Test
	public void testLegacyNoManifestStillCommits(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		uploadWithManifest(distributeDir, "svc", List.of("app.jar"), null);
		var dm = new DistributeManager(distributeDir, servicesDir);

		assertEquals(0, dm.commit("svc", "v1"), "无清单走legacy路径：既有行为不变");
		assertTrue(Files.isRegularFile(Path.of(servicesDir.getPath(), "svc", "v1", "app.jar")));
	}

	/** 清单是数据不是可信输入：逃逸行（../）响亮拒绝。 */
	@Test
	public void testUnsafeManifestLineRejected(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		uploadWithManifest(distributeDir, "svc", List.of("app.jar"), List.of("../escape"));
		var dm = new DistributeManager(distributeDir, servicesDir);

		assertEquals(COMMIT_FAIL, dm.commit("svc", "v1"), "清单逃逸行必须拒绝");
		assertFalse(new File(servicesDir, "svc").exists());
	}
}
