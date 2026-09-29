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
 * commit 集合清单的行级判据：清单行是数据不是可信输入，合法形态只有一种——
 * 相对 distributeDir 根、首段=本服务名的多段相对路径（参考客户端 ZokerAgent 生成的
 * 形态）。校验（verifyDistributeManifest）/清退（pruneUnlistedFiles）/跳装收养
 *（manifestEntriesAllPresent）三侧判据同源于同一解析器：
 * <ul>
 * <li>跨服务引用行（首段非本服务名，文件实际位于 distributes/&lt;other&gt;/）不得通过：
 *   校验侧只看"界内+文件存在"会放行，而 commit 只 rename distributes/&lt;svc&gt;——
 *   成版的版本目录缺清单自 declare 的文件，回执 0 的假成功。</li>
 * <li>".."等变体行（lexical normalize 后仍在界内、OS 路径解析可命中真实文件）不得通过：
 *   校验侧按解析命中放行、清退侧按原始行折叠比对永不命中——已列且在盘的文件被当
 *   未列残留删除，成版残缺版本。</li>
 * <li>已装版本目录自带的清单（跳装收养判据）同源校验：跨服务引用行的版本目录不可收养。</li>
 * </ul>
 */
@Fast
public class TestCommitManifestLineGuard {
	private static final long COMMIT_FAIL = IModule.errorCode(Zoker.ModuleId, Zoker.eCommitFail);

	/** 在 distributes/&lt;物理svc&gt;/ 放文件并写清单（清单行=调用方原样拼写）。 */
	private static void stage(Path distributeDir, String physicalService, List<String> files,
							  List<String> manifestLines) throws Exception {
		var svc = distributeDir.resolve(physicalService);
		Files.createDirectories(svc);
		for (var rel : files) {
			var p = svc.resolve(rel);
			Files.createDirectories(p.getParent());
			Files.writeString(p, "content-of-" + rel);
		}
		var lines = new StringBuilder();
		for (var line : manifestLines)
			lines.append(line).append('\n');
		Files.writeString(svc.resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME), lines.toString());
	}

	/** 跨服务引用行：清单 declare 的文件不在本服务暂存区（不会被 rename 搬运），
	 * 必须响亮拒绝而非假成功成版缺文件的版本。修复前红点：界内+isFile 双过，返回 0。 */
	@Test
	public void testCrossServiceLineRejected(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.resolve("other")); // 他服务的文件真实在盘
		Files.writeString(distributeDir.resolve("other").resolve("x.jar"), "foreign");
		stage(distributeDir, "svc", List.of("app.jar"), List.of("svc/app.jar", "other/x.jar"));
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		assertEquals(COMMIT_FAIL, dm.commit("svc", "v1"), "跨服务引用行必须拒绝");
		assertFalse(Files.exists(servicesDir.resolve("svc")), "失败无副作用");
		assertTrue(Files.isRegularFile(distributeDir.resolve("other").resolve("x.jar")),
				"他服务文件不受牵连");
	}

	/** ".."变体行：行内 dot 段拒绝而非消解——校验侧（解析命中）与清退侧（原始行折叠
	 * 比对永失配）判据不同源时，已列且在盘的文件会被当未列残留删除。
	 * 修复前红点：返回 0 且 svc/x.jar 被清退、成版残缺版本。 */
	@Test
	public void testDotSegmentLineRejectedWithoutPruningListedFile(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		stage(distributeDir, "svc", List.of("x.jar"), List.of("svc/sub/../../svc/x.jar"));
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		assertEquals(COMMIT_FAIL, dm.commit("svc", "v1"), "含 dot 段的变体行必须拒绝");
		assertFalse(Files.exists(servicesDir.resolve("svc")), "失败无副作用");
		assertTrue(Files.isRegularFile(distributeDir.resolve("svc").resolve("x.jar")),
				"拒绝路径不得清退已上传文件（清退只在全部行校验通过后发生）");
	}

	/** 已装版本目录自带清单含跨服务引用行（存量/外部制造的残缺形态）：跳装收养判据
	 * 同源拒绝——不可收养，有新内容则隔离换装。修复前红点：首段被无校验剥除，
	 * 判健康+子集成立快速跳装，v1 保留旧内容且新内容滞留 distributes（假成功）。 */
	@Test
	public void testCrossServiceLineInInstalledManifestNotAdopted(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		// 盘上版本目录：清单行首段写别的服务名（文件本身在目录内）
		var v1 = Files.createDirectories(servicesDir.resolve("svc").resolve("v1"));
		Files.writeString(v1.resolve("app.jar"), "old-app");
		Files.writeString(v1.resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME), "other/app.jar\n");
		// 同版本号重提的新内容（正常形态清单）
		stage(distributeDir, "svc", List.of("app.jar"), List.of("svc/app.jar"));
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		assertEquals(0, dm.commit("svc", "v1"), "不可收养但有新内容：隔离换装后成功");
		assertEquals("content-of-app.jar", Files.readString(v1.resolve("app.jar")),
				"v1 内容=新内容（跨服务行的清单不被收养）");
		assertFalse(Files.isDirectory(distributeDir.resolve("svc")), "新内容被消费");
		assertEquals("v1", Files.readString(servicesDir.resolve("svc")
				.resolve(DistributeManager.CURRENT_NAME)));
	}
}
