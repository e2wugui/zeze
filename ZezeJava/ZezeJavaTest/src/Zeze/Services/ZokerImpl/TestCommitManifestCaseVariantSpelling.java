package Zeze.Services.ZokerImpl;

import harness.Extra;
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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * commit 清单残留清理的变体拼写判同：清单行=上传侧拼写，walk相对路径=提交侧服务名+
 * 盘上实际名。Win32 路径规范化下变体拼写（Svc/svc.、X.JAR/x.jar）指向同一物理文件——
 * 判同不过段级折叠时已列文件整体落入清理面被删，空壳版本（仅剩清单）假成功成版切current。
 */
@Fast
@Extra
public class TestCommitManifestCaseVariantSpelling {
	private static final boolean WINDOWS =
			System.getProperty("os.name", "").toLowerCase().contains("win");

	/** 物理创建 distributes/&lt;物理svc&gt;/ 下文件；清单行用调用方给的拼写前缀拼出。 */
	private static void uploadWithVariantManifest(Path distributeDir, String physicalService,
												  List<String> files, List<String> manifestLines,
												  String manifestServiceSpelling) throws Exception {
		var svc = distributeDir.resolve(physicalService);
		Files.createDirectories(svc);
		for (var rel : files) {
			var p = svc.resolve(rel);
			Files.createDirectories(p.getParent());
			Files.writeString(p, "content-of-" + rel);
		}
		var lines = new StringBuilder();
		for (var line : manifestLines)
			lines.append(manifestServiceSpelling).append('/').append(line).append('\n');
		Files.writeString(svc.resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME), lines.toString());
	}

	/**
	 * 变体拼写的清单行+变体文件名段：清单声明的文件不得落入清理面。
	 * 修复前：裸 contains 失配——全部服务文件被当残留删除，版本目录只剩清单。
	 */
	@Test
	public void testVariantSpelledManifestKeepsFiles(@TempDir Path tempDir) throws Exception {
		assumeTrue(WINDOWS, "变体拼写命中同一物理实体依赖Win32路径规范化");
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		// 上传侧物理名 svc/…（含子目录文件）；清单=另一操作者拼写：首段 Svc、文件段 X.JAR
		uploadWithVariantManifest(distributeDir, "svc",
				List.of("app.jar", "lib/x.jar"),
				List.of("APP.JAR", "lib/X.JAR"),
				"Svc");
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		assertEquals(0, dm.commit("svc", "v1"), "变体拼写的清单校验应通过（Win32 同物理实体）");
		var version = servicesDir.resolve("svc").resolve("v1");
		assertTrue(Files.isRegularFile(version.resolve("app.jar")),
				"清单内文件（首段变体）不得被当残留删除");
		assertTrue(Files.isRegularFile(version.resolve("lib/x.jar")),
				"清单内文件（文件名段变体）不得被当残留删除");
		assertTrue(Files.isRegularFile(version.resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME)),
				"清单自身随版本目录保留");
		assertEquals("v1", Files.readString(servicesDir.resolve("svc").resolve("current")));
	}

	/** 折叠命中保已列文件的同时，真正未列的残留仍清退（清退语义不因折叠放宽而消失）。 */
	@Test
	public void testVariantResidueFoldedAsListedKept(@TempDir Path tempDir) throws Exception {
		assumeTrue(WINDOWS, "变体拼写判同依赖Win32路径规范化");
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		uploadWithVariantManifest(distributeDir, "svc",
				List.of("app.jar", "stale-old.jar"),
				List.of("APP.JAR"),
				"Svc");
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		assertEquals(0, dm.commit("svc", "v1"));
		var version = servicesDir.resolve("svc").resolve("v1");
		assertTrue(Files.isRegularFile(version.resolve("app.jar")),
				"变体命中的已列文件保留");
		assertFalse(Files.exists(version.resolve("stale-old.jar")),
				"清单外残留（无任何拼写命中）仍清退");
	}
}
