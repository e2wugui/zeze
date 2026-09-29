package Zeze.Services.ZokerImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * commit 消费的清单按部署（版本号）归属：同名服务两路并发分发共享同一暂存区
 * distributes/&lt;svc&gt;，裸名清单（.zoker-manifest）是共享路径上的普通文件、
 * 最后写者胜——修复前 commit 只读提交时刻盘上现存的那份清单，先到的 commit 消费
 * 后到者的清单：其文件被当残留清退、后到者的文件整目录成版进自己的版本号并切
 * current，回执成功的版本内容与版本号错配。
 * 修复后清单带版本限定名（.zoker-manifest.&lt;versionNo&gt;，部署方上传时与服务端
 * commit 按本次版本号对应消费）：两路部署的清单互不覆盖，各自 commit 消费自己的
 * 清单；他版本的清单与未列文件一并清退（暂存区以清单为单位原子消费，失败方
 * eCommitFail 可见、重传收敛）。裸名清单作为旧客户端/兼容副本回落消费（既有语义）。
 */
@Fast
public class TestCommitConsumesOwnVersionManifest {

	/** distributes/&lt;svc&gt;/ 放文件 + 按名写清单（清单名调用方给定：裸名或版本限定名）。 */
	private static void stage(Path distributeDir, List<String> files, String manifestFileName,
							  List<String> manifestLines) throws Exception {
		var svc = distributeDir.resolve("svc");
		Files.createDirectories(svc);
		for (var rel : files) {
			var p = svc.resolve(rel);
			Files.createDirectories(p.getParent());
			Files.writeString(p, "content-of-" + rel);
		}
		var lines = new StringBuilder();
		for (var line : manifestLines)
			lines.append(line).append('\n');
		Files.writeString(svc.resolve(manifestFileName), lines.toString());
	}

	/** 两路部署交错（B 的裸名清单最后落地覆盖 A 的）后 A 先 commit：必须消费 A 自己
	 * 的清单（版本限定名），不是最后写者的裸名清单。
	 * 修复前红点：按裸名清单校验+清退——a1 被删、v1 内容=b1（B 的集合），返回 0。 */
	@Test
	public void testCommitConsumesOwnVersionManifestNotLastWriter(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		// A 上传 a1.jar + 版本限定清单（.zoker-manifest.v1）；B 后到覆盖裸名清单
		stage(distributeDir, List.of("a1.jar", "b1.jar"), ".zoker-manifest.v1", List.of("svc/a1.jar"));
		Files.writeString(distributeDir.resolve("svc").resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME),
				"svc/b1.jar\n"); // B 的裸名清单最后落地（最后写者）
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		assertEquals(0, dm.commit("svc", "v1"));

		var v1 = servicesDir.resolve("svc").resolve("v1");
		assertTrue(Files.isRegularFile(v1.resolve("a1.jar")), "A 的文件不得被当残留清退");
		assertEquals("content-of-a1.jar", Files.readString(v1.resolve("a1.jar")));
		assertFalse(Files.exists(v1.resolve("b1.jar")), "清单外文件（B 的）清退：版本内容=清单精确集合");
		assertEquals("v1", Files.readString(servicesDir.resolve("svc")
				.resolve(DistributeManager.CURRENT_NAME)));
	}

	/** 他版本的限定清单与未列文件一并清退（暂存区以本次清单为单位原子消费）：
	 * A(v1) 先 commit，B(v2) 的清单与文件不随 v1 成版——B 随后 commit 得可见失败重传。
	 * 修复前红点：无裸名清单走 legacy 无清退，b1 与 .zoker-manifest.v2 混入 v1 成版。 */
	@Test
	public void testOtherVersionManifestAndFilesPruned(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		stage(distributeDir, List.of("a1.jar", "a2.jar", "b1.jar"), ".zoker-manifest.v1",
				List.of("svc/a1.jar", "svc/a2.jar"));
		Files.writeString(distributeDir.resolve("svc").resolve(".zoker-manifest.v2"), "svc/b1.jar\n");
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		assertEquals(0, dm.commit("svc", "v1"));

		var v1 = servicesDir.resolve("svc").resolve("v1");
		assertTrue(Files.isRegularFile(v1.resolve("a1.jar")));
		assertTrue(Files.isRegularFile(v1.resolve("a2.jar")));
		assertFalse(Files.exists(v1.resolve("b1.jar")), "他版本文件清退");
		assertFalse(Files.exists(v1.resolve(".zoker-manifest.v2")), "他版本清单清退");
		assertTrue(Files.isRegularFile(v1.resolve(".zoker-manifest.v1")),
				"本次版本的清单随版本目录成版");
	}

	/** 裸名清单回落（旧客户端/兼容副本形态）语义不变：无版本限定名时按裸名消费
	 * （FND26/FND29/FND30 既有测试同构，此处固化回落优先级）。 */
	@Test
	public void testBareManifestFallbackStillConsumed(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		stage(distributeDir, List.of("a1.jar"), DistributeManager.DISTRIBUTE_MANIFEST_NAME,
				List.of("svc/a1.jar"));
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		assertEquals(0, dm.commit("svc", "v1"));
		var v1 = servicesDir.resolve("svc").resolve("v1");
		assertTrue(Files.isRegularFile(v1.resolve("a1.jar")));
		assertTrue(Files.isRegularFile(v1.resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME)),
				"裸名清单随版本成版");
	}
}
