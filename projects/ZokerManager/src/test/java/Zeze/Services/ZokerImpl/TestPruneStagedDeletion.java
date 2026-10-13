package Zeze.Services.ZokerImpl;

import harness.Extra;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import harness.Fast;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * prune 的暂存名删除：victim 先同容器原子改名进 {@code .zoker-deleting.<原名>.<millis>}
 * 再整树删除——deleteTree 的部分失败（Windows 外部句柄占用：AV 实时扫描/备份/从版本目录
 * 启动且句柄未释放的进程）只可能残缺暂存名，不再制造"存在但不完整"的版本名目录
 * （跳装判据"存在=完整"的构造性保证，修复前残缺版本目录可被同版本号重提的 commit
 * 跳装收养并切为现役）。暂存名排除出保留计数与候选；上一轮的暂存残留由下一轮 prune
 * 入口先清扫。
 */
@Fast
@Extra
public class TestPruneStagedDeletion {
	private static final boolean WINDOWS =
			System.getProperty("os.name", "").toLowerCase().contains("win");
	/** 暂存删除名前缀（与 DistributeManager 的常量同字面；测试内联使红态可先于实现编译）。 */
	private static final String STAGE_PREFIX = ".zoker-deleting.";

	/** svc 下摆 versions（mtime 阶梯升序=越靠后越新）+ current 指针。 */
	private static void layoutVersions(Path svcDir, String current, String... versions) throws Exception {
		Files.createDirectories(svcDir);
		var base = System.currentTimeMillis() - versions.length * 60_000L;
		for (var i = 0; i < versions.length; i++) {
			var dir = Files.createDirectories(svcDir.resolve(versions[i]));
			Files.writeString(dir.resolve("app.jar"), "body-" + versions[i]);
			Files.createDirectories(dir.resolve("lib"));
			Files.writeString(dir.resolve("lib/x.jar"), "lib-" + versions[i]);
			assertTrue(dir.toFile().setLastModified(base + i * 60_000L), "mtime 阶梯可设");
		}
		Files.writeString(svcDir.resolve(DistributeManager.CURRENT_NAME), current);
	}

	private static List<String> versionDirs(Path svcDir) throws Exception {
		try (var list = Files.list(svcDir)) {
			return list.filter(Files::isDirectory).map(p -> p.getFileName().toString())
					.filter(n -> !n.startsWith(STAGE_PREFIX)).sorted().toList();
		}
	}

	private static List<String> stageDirs(Path svcDir) throws Exception {
		try (var list = Files.list(svcDir)) {
			return list.filter(Files::isDirectory).map(p -> p.getFileName().toString())
					.filter(n -> n.startsWith(STAGE_PREFIX)).toList();
		}
	}

	/** 常规 prune（rename+deleteTree 都成功）：victim 无痕清理，无暂存名残留。 */
	@Test
	public void testPruneLeavesNoStageResidue(@TempDir Path tempDir) throws Exception {
		var servicesDir = tempDir.resolve("services");
		var svcDir = servicesDir.resolve("svc");
		layoutVersions(svcDir, "v3", "v1", "v2", "v3");

		var dm = new DistributeManager(tempDir.resolve("distributes").toFile(), servicesDir.toFile());
		dm.setKeepVersions(1);
		dm.pruneVersions(svcDir.toFile(), "v3");

		assertEquals(List.of("v3"), versionDirs(svcDir), "keep=1 只留现役");
		assertEquals(List.of(), stageDirs(svcDir), "暂存名不残留（deleteTree 成功即无痕）");
	}

	/** 下一轮 prune 入口清扫上一轮的暂存残留（deleteTree 失败遗留的暂存名目录）：
	 * 清扫不受保留策略约束（暂存名不在版本语义面内，纯垃圾回收），真实版本目录不受影响。
	 * 修复前红点：暂存名被当普通版本目录——占据 keep 名额（真实版本被挤入清理面）。 */
	@Test
	public void testPruneEntrySweepsLeftoverStageResidue(@TempDir Path tempDir) throws Exception {
		var servicesDir = tempDir.resolve("services");
		var svcDir = servicesDir.resolve("svc");
		layoutVersions(svcDir, "v3", "v1", "v2", "v3");
		var stage = Files.createDirectories(svcDir.resolve(STAGE_PREFIX + "old.123"));
		Files.writeString(stage.resolve("app.jar"), "residue");

		var dm = new DistributeManager(tempDir.resolve("distributes").toFile(), servicesDir.toFile());
		dm.setKeepVersions(3); // 无版本可清：仅入口清扫生效
		dm.pruneVersions(svcDir.toFile(), "v3");

		assertFalse(Files.exists(stage), "暂存残留被入口清扫");
		assertEquals(List.of("v1", "v2", "v3"), versionDirs(svcDir), "真实版本不受清扫影响");
	}

	/** Windows 持句柄跑 prune：victim 保持完整（rename 失败本轮跳过）或已整体改名走
	 * ——不存在"版本名目录缺文件"中间态。修复前红点：deleteTree 逐文件删，句柄文件
	 * 之前的文件已被删掉，留下缺文件的版本名目录（可被同版本号重提跳装收养的残缺形态）。 */
	@Test
	public void testHeldHandleVictimNeverPartiallyDeleted(@TempDir Path tempDir) throws Exception {
		Assumptions.assumeTrue(WINDOWS, "句柄阻塞删除/目录rename为Win32语义（本机探针实证）");
		var servicesDir = tempDir.resolve("services");
		var svcDir = servicesDir.resolve("svc");
		layoutVersions(svcDir, "v3", "v1", "v2", "v3");

		var dm = new DistributeManager(tempDir.resolve("distributes").toFile(), servicesDir.toFile());
		dm.setKeepVersions(1);
		// 句柄持有 v1/lib/x.jar（listFiles 名序在 app.jar 之后：修复前 app.jar 先被删、
		// 删到句柄文件失败返回——v1 残缺为"缺 app.jar"形态）
		try (var handle = new RandomAccessFile(svcDir.resolve("v1").resolve("lib").resolve("x.jar").toFile(), "rw")) {
			handle.write(1);
			dm.pruneVersions(svcDir.toFile(), "v3");
		}

		assertFalse(Files.isDirectory(svcDir.resolve("v2")), "无句柄 victim 照常清理");
		assertTrue(Files.isDirectory(svcDir.resolve("v3")), "现役保护");
		// 不变量：victim 或保持完整（rename 失败跳过），或已不在版本名位置（整体进暂存名）
		if (Files.isDirectory(svcDir.resolve("v1"))) {
			assertTrue(Files.isRegularFile(svcDir.resolve("v1").resolve("app.jar")),
					"版本名目录不得缺文件（残缺中间态）");
			assertTrue(Files.isRegularFile(svcDir.resolve("v1").resolve("lib").resolve("x.jar")));
		}
	}
}
