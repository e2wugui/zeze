package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND22 GE-C01：pruneVersions 现役保护用大小写敏感 equals——Windows(Win32) 解析大小写不敏感
 * 且剥尾部点/空格，请求 versionNo（"V1"/"v1."）与盘上目录名（"v1"）分叉时：exists 跳装命中
 * 旧版本、指针写入请求原样文本、prune 保护比对面对盘上真名失效——物理现役目录落入清理面
 * 被删，current 悬空、startService 恒 eNoServiceProperties（默认 keep=3 三版本存量即 wedge）。
 * FND21 GE-C01 只折叠了保留字（current/run.pid），普通版本名的同名分叉面未闭合。
 * 修复双点：
 * <ul>
 * <li>commitLocked 指针与 prune 参数规范化为盘上实际目录名（onDiskVersionName）——分叉源头
 * 闭合（断言 current 文件内容=盘上真名）；</li>
 * <li>pruneVersions 现役保护按 foldVersionName（剥尾点/空格+忽略大小写）折叠比对——独立
 * 第二道防，直调/存量分叉指针亦闭合。</li>
 * </ul>
 * FS 前提（跨大小写 File.exists()==true、尾点解析命中）系 FND21 GE-C01 修复轮本机探针实证
 * （案卷复用），测试内仍以 assumeTrue 前置守卫：FS 行为不符时跳过而非假失败。
 * 纯文件用例（折叠保护本体）全平台可跑；commit 变体路径用例为 Windows 形态。
 */
@Fast
public class TestE01PruneProtectCaseVariant {
	private static final boolean WINDOWS =
			System.getProperty("os.name", "").toLowerCase().contains("win");

	/** 三版本存量（v1 最老），current 指向 v3；mtime 显式阶梯加宽可断言性。 */
	private static void layout3Versions(Path svcDir, String current) throws IOException {
		Files.createDirectories(svcDir);
		var base = System.currentTimeMillis() - 1_000_000;
		var versions = new String[] {"v1", "v2", "v3"};
		for (var i = 0; i < versions.length; i++) {
			var dir = Files.createDirectories(svcDir.resolve(versions[i]));
			Files.writeString(dir.resolve("app.jar"), "body-" + versions[i]);
			assertTrue(dir.toFile().setLastModified(base + i * 60_000L), "mtime 阶梯可设");
		}
		Files.writeString(svcDir.resolve(DistributeManager.CURRENT_NAME), current);
	}

	private static File emptyDistributes(Path tempDir) throws IOException {
		var dir = tempDir.resolve("distributes");
		Files.createDirectories(dir);
		return dir.toFile();
	}

	/** 折叠保护本体（全平台纯文件）：prune 参数为大小写变体时，物理现役目录（盘上真名）
	 * 必须仍受保护。修复前红点：裸 equals 下 v1 成为候选、作为最老版本被删。 */
	@Test
	public void testPruneFoldProtectsPhysicalCurrent(@TempDir Path tempDir) throws Exception {
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(servicesDir);
		var svcDir = servicesDir.resolve("svc");
		layout3Versions(svcDir, "v3");

		var dm = new DistributeManager(emptyDistributes(tempDir), servicesDir.toFile());
		dm.setKeepVersions(2);
		// 直调 prune，参数=请求原样变体文本 "V1"（盘上真名 "v1"）
		dm.pruneVersions(svcDir.toFile(), "V1");

		assertTrue(Files.isDirectory(svcDir.resolve("v1")), "物理现役目录（折叠同名）必须在保护面");
		assertFalse(Files.exists(svcDir.resolve("v2")), "prune 确已执行（最老非现役被清）");
		assertTrue(Files.isDirectory(svcDir.resolve("v3")));
	}

	/** 尾点变体同判（全平台）：参数 "v1." 折叠后命中盘上 "v1" 的保护。 */
	@Test
	public void testPruneFoldProtectsTrailingDotVariant(@TempDir Path tempDir) throws Exception {
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(servicesDir);
		var svcDir = servicesDir.resolve("svc");
		layout3Versions(svcDir, "v3");

		var dm = new DistributeManager(emptyDistributes(tempDir), servicesDir.toFile());
		dm.setKeepVersions(2);
		dm.pruneVersions(svcDir.toFile(), "v1.");

		assertTrue(Files.isDirectory(svcDir.resolve("v1")), "尾点变体折叠后同保护");
		assertFalse(Files.exists(svcDir.resolve("v2")));
	}

	/** commit 全链（Windows，FS 前提守卫）：存量三版本上变体 commit("svc","V1")——exists 跨
	 * 大小写命中 v1 跳装、指针原样写 "V1" 的旧形态使 v1 落入清理面被删（wedge）。修复后：
	 * 指针规范化为盘上真名，current 解析非空、v1 存活。 */
	@Test
	public void testCommitCaseVariantKeepsPhysicalCurrent(@TempDir Path tempDir) throws Exception {
		Assumptions.assumeTrue(WINDOWS, "跨大小写 exists 命中为 Win32 解析前提（FND21 探针实证）");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(servicesDir);
		var svcDir = servicesDir.resolve("svc");
		layout3Versions(svcDir, "v3");
		Assumptions.assumeTrue(new File(svcDir.toFile(), "V1").exists(),
				"FS 前提守卫：跨大小写 exists 命中（不符则本用例平台形态不成立，跳过）");

		var dm = new DistributeManager(emptyDistributes(tempDir), servicesDir.toFile());
		assertEquals(0, dm.commit("svc", "V1"), "变体 commit（回滚语义本身合法）应成功");

		// 修复主断言一：指针内容=盘上实际目录名（分叉源头闭合）
		var pointer = Files.readString(svcDir.resolve(DistributeManager.CURRENT_NAME)).trim();
		assertEquals("v1", pointer, "指针必须写盘上实际目录名，不得写请求原样文本");
		// 修复主断言二：物理现役存活且 current 解析非空（修复前：prune 删 v1 → 悬空 → 恒 null）
		assertTrue(Files.isDirectory(svcDir.resolve("v1")), "物理现役 v1 不得落入清理面");
		var currentDir = DistributeManager.currentVersionDir(svcDir.toFile());
		assertNotNull(currentDir, "commit 返回 0 后 current 不得悬空");
		assertEquals("v1", currentDir.getName());
	}

	/** commit 全链尾点变体（Windows）："v1." 跳装命中 v1（探针实证脱点解析），修复前 prune
	 * 保护比对 "v1" != "v1." 失效、v1 作为最老版本被删 → current 悬空。 */
	@Test
	public void testCommitTrailingDotVariantKeepsPhysicalCurrent(@TempDir Path tempDir) throws Exception {
		Assumptions.assumeTrue(WINDOWS, "尾点解析命中为 Win32 规范化前提（FND21 探针实证）");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(servicesDir);
		var svcDir = servicesDir.resolve("svc");
		layout3Versions(svcDir, "v3");
		Assumptions.assumeTrue(new File(svcDir.toFile(), "v1.").exists(),
				"FS 前提守卫：尾点解析命中（不符则本用例平台形态不成立，跳过）");

		var dm = new DistributeManager(emptyDistributes(tempDir), servicesDir.toFile());
		assertEquals(0, dm.commit("svc", "v1."));

		assertEquals("v1", Files.readString(svcDir.resolve(DistributeManager.CURRENT_NAME)).trim(),
				"指针规范化为落盘真名（renameTo 脱点占位）");
		assertTrue(Files.isDirectory(svcDir.resolve("v1")));
		assertNotNull(DistributeManager.currentVersionDir(svcDir.toFile()), "current 不得悬空");
	}

	/** keep=1 现役自身变体（Windows，案卷最简 wedge 形态）：commit("svc","V3") 跳装+指针改写，
	 * 修复前 prune 删除全部候选（含物理现役 v3）。 */
	@Test
	public void testCommitSelfVariantKeepOneKeepsCurrent(@TempDir Path tempDir) throws Exception {
		Assumptions.assumeTrue(WINDOWS, "跨大小写 exists 命中为 Win32 解析前提");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(servicesDir);
		var svcDir = servicesDir.resolve("svc");
		layout3Versions(svcDir, "v3");
		Assumptions.assumeTrue(new File(svcDir.toFile(), "V3").exists(), "FS 前提守卫");

		var dm = new DistributeManager(emptyDistributes(tempDir), servicesDir.toFile());
		dm.setKeepVersions(1);
		assertEquals(0, dm.commit("svc", "V3"));

		assertTrue(Files.isDirectory(svcDir.resolve("v3")), "现役自身变体不得使物理现役入清理面");
		assertFalse(Files.exists(svcDir.resolve("v1")), "keep=1 下非现役照常清理（语义不变）");
		assertFalse(Files.exists(svcDir.resolve("v2")));
		var currentDir = DistributeManager.currentVersionDir(svcDir.toFile());
		assertNotNull(currentDir, "keep=1 现役自身变体后 current 不得悬空");
		assertEquals("v3", currentDir.getName());
	}

	/** 正常名回归钉（全平台）：规范化和折叠不得改变正常 commit 的指针/清理语义。 */
	@Test
	public void testCommitNormalNameUnchanged(@TempDir Path tempDir) throws Exception {
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(servicesDir);
		var svcDir = servicesDir.resolve("svc");
		layout3Versions(svcDir, "v3");
		// 正常新版本安装路径：distributes 内容 rename 落盘
		var staging = Files.createDirectories(tempDir.resolve("distributes").resolve("svc"));
		Files.writeString(staging.resolve("app.jar"), "new-v4");

		var dm = new DistributeManager(tempDir.resolve("distributes").toFile(), servicesDir.toFile());
		assertEquals(0, dm.commit("svc", "v4"));

		assertEquals("v4", Files.readString(svcDir.resolve(DistributeManager.CURRENT_NAME)).trim());
		var currentDir = DistributeManager.currentVersionDir(svcDir.toFile());
		assertNotNull(currentDir);
		assertEquals("v4", currentDir.getName());
		assertTrue(Files.isDirectory(svcDir.resolve("v3")), "keep=3：现役+两个回滚点");
		assertTrue(Files.isDirectory(svcDir.resolve("v2")));
		assertFalse(Files.exists(svcDir.resolve("v1")), "最老版本照常清理");
	}
}
