package Zeze.Services.ZokerImpl;

import harness.Extra;
import java.io.File;
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
 * 跳装分支的内容判据：同版本号重提新内容（部署侧忘递增 versionNo / 构建产物更新而
 * 版本号不变），旧判据只验"清单条目在已装版本目录<b>存在</b>"（纯存在性），升级为
 * 文件大小后又止步于大小——CloseFile 已 md5 验证的暂存区新字节与已装旧字节之间
 * 零内容比对，同尺寸异字节的 commit 回执 0 静默跳装：现役继续服务旧字节、新字节
 * 滞留 distributes（下次 commit 被当残留清退），全程无日志；legacy（无清单）形态
 * 更是零比对纯存在性即跳装。修复后跳装判据为逐文件内容一致（大小短路之上复算
 * 两侧全量 md5；legacy 形态与 distributes 源文件逐一比对）——任何不一致（含同
 * 大小不同字节）判不可收养走隔离换装，新内容实际成版；同字节重提仍快速跳装
 * 幂等（见 TestCommitBrokenVersionResidue 的 mtime 护栏）。
 */
@Fast
@Extra
public class TestCommitSameVersionDifferentContent {
	/** 暂存删除名前缀（与 DistributeManager 的常量同字面；测试内联使红态可先于实现编译）。 */
	private static final String STAGE_PREFIX = ".zoker-deleting.";

	/** distributes/&lt;svc&gt;/ 放文件（content 为文件正文）+集合清单（行含服务名首段）。 */
	private static void upload(File distributeDir, String serviceName, String fileName,
							   String content) throws Exception {
		var svc = distributeDir.toPath().resolve(serviceName);
		Files.createDirectories(svc);
		Files.writeString(svc.resolve(fileName), content);
		Files.writeString(svc.resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME),
				serviceName + "/" + fileName + "\n");
	}

	/** services/&lt;svc&gt;/ 下暂存删除名目录清单（隔离换装的中间态，应被同轮 prune 清扫）。 */
	private static List<String> stageDirs(Path svcDir) throws Exception {
		try (var list = Files.list(svcDir)) {
			return list.filter(Files::isDirectory).map(p -> p.getFileName().toString())
					.filter(n -> n.startsWith(STAGE_PREFIX)).toList();
		}
	}

	/** 同版本号重提不同内容（大小不同）：存在性判据放行=假成功静默旧内容；大小比对
	 * 判不可收养→隔离换装，新内容实际成版。修复前红点：返回 0 但 v1 仍是旧字节。 */
	@Test
	public void testDifferentSizeContentSwappedIn(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		var dm = new DistributeManager(distributeDir, servicesDir.toFile());

		upload(distributeDir, "svc", "app.jar", "old");
		assertEquals(0, dm.commit("svc", "v1"));
		var v1 = servicesDir.resolve("svc").resolve("v1");
		assertEquals("old", Files.readString(v1.resolve("app.jar")));

		upload(distributeDir, "svc", "app.jar", "new-content-of-different-size");
		assertEquals(0, dm.commit("svc", "v1"), "同版本号不同内容：换装后成功（不是跳装假成功）");

		assertEquals("new-content-of-different-size", Files.readString(v1.resolve("app.jar")),
				"新内容必须实际成版（修复前静默服务旧字节）");
		assertFalse(Files.isDirectory(distributeDir.toPath().resolve("svc")), "新内容被消费");
		assertEquals("v1", Files.readString(servicesDir.resolve("svc")
				.resolve(DistributeManager.CURRENT_NAME)));
		assertEquals(List.of(), stageDirs(servicesDir.resolve("svc")), "隔离暂存名同轮 prune 清扫");
	}

	/** 同版本号重提同大小不同内容：大小判据放行=假成功静默旧字节（上一轮修复
	 * （202133bbe）的声明残余边界，本轮按内容权威判据重估）；md5 比对判不可收养
	 * →隔离换装，新内容实际成版。修复前红点：返回 0 但 v1 仍是旧字节"aaaa"。 */
	@Test
	public void testSameSizeDifferentContentSwappedIn(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		var dm = new DistributeManager(distributeDir, servicesDir.toFile());

		upload(distributeDir, "svc", "app.jar", "aaaa");
		assertEquals(0, dm.commit("svc", "v1"));
		var v1 = servicesDir.resolve("svc").resolve("v1");

		upload(distributeDir, "svc", "app.jar", "bbbb"); // 同大小不同字节
		assertEquals(0, dm.commit("svc", "v1"), "同版本号同大小异内容：换装后成功（不是跳装假成功）");

		assertEquals("bbbb", Files.readString(v1.resolve("app.jar")),
				"新内容必须实际成版（修复前静默服务旧字节）");
		assertFalse(Files.isDirectory(distributeDir.toPath().resolve("svc")), "新内容被消费");
		assertEquals("v1", Files.readString(servicesDir.resolve("svc")
				.resolve(DistributeManager.CURRENT_NAME)));
		assertEquals(List.of(), stageDirs(servicesDir.resolve("svc")), "隔离暂存名同轮 prune 清扫");
	}

	/** 同字节重提（幂等重试语义）：快速跳装不换装——已装版本内容与 mtime 均不动，
	 * 新上传滞留 distributes（跳装既有语义，内容判据对同字节必过）。 */
	@Test
	public void testIdenticalContentResubmitStillSkips(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		var dm = new DistributeManager(distributeDir, servicesDir.toFile());

		upload(distributeDir, "svc", "app.jar", "aaaa");
		assertEquals(0, dm.commit("svc", "v1"));
		var v1 = servicesDir.resolve("svc").resolve("v1");
		var installedMtime = Files.getLastModifiedTime(v1);

		upload(distributeDir, "svc", "app.jar", "aaaa"); // 同字节重提
		assertEquals(0, dm.commit("svc", "v1"), "同内容重提幂等成功");
		assertEquals("aaaa", Files.readString(v1.resolve("app.jar")), "已装版本不动（版本纪律）");
		assertEquals(installedMtime, Files.getLastModifiedTime(v1), "快速跳装不动已装版本（无隔离改名）");
		assertTrue(Files.isDirectory(distributeDir.toPath().resolve("svc")), "跳装不消费新上传");
	}

	// ---------- legacy（无清单）形态：修复前零比对纯存在性即跳装 ----------

	/** distributes/&lt;svc&gt;/ 只放文件不写清单（外部部署工具/直构形态）。 */
	private static void uploadLegacy(File distributeDir, String serviceName, String fileName,
									 String content) throws Exception {
		var svc = distributeDir.toPath().resolve(serviceName);
		Files.createDirectories(svc);
		Files.writeString(svc.resolve(fileName), content);
	}

	/** legacy 同版本号重提同大小不同内容：修复前零比对即跳装（静默旧字节+回执 0）；
	 * 修复后与 distributes 源文件逐一 md5 比对，不符走隔离换装，新内容成版。 */
	@Test
	public void testLegacySameSizeDifferentContentSwappedIn(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		var dm = new DistributeManager(distributeDir, servicesDir.toFile());

		uploadLegacy(distributeDir, "svc", "app.jar", "aaaa");
		assertEquals(0, dm.commit("svc", "v1"));
		var v1 = servicesDir.resolve("svc").resolve("v1");

		uploadLegacy(distributeDir, "svc", "app.jar", "bbbb"); // 同大小不同字节，无清单
		assertEquals(0, dm.commit("svc", "v1"), "legacy 同版本号异内容：换装后成功（不是零比对跳装）");

		assertEquals("bbbb", Files.readString(v1.resolve("app.jar")),
				"legacy 新内容必须实际成版（修复前零比对静默服务旧字节）");
		assertFalse(Files.isDirectory(distributeDir.toPath().resolve("svc")), "新内容被消费");
		assertEquals(List.of(), stageDirs(servicesDir.resolve("svc")), "隔离暂存名同轮 prune 清扫");
	}

	/** legacy 同字节重提（无新内容的幂等重试）：跳装语义保持——已装版本不动、
	 * distributes 滞留（修复后暂存区逐一比对全过，快速跳装）。 */
	@Test
	public void testLegacyIdenticalContentResubmitStillSkips(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		var dm = new DistributeManager(distributeDir, servicesDir.toFile());

		uploadLegacy(distributeDir, "svc", "app.jar", "aaaa");
		assertEquals(0, dm.commit("svc", "v1"));
		var v1 = servicesDir.resolve("svc").resolve("v1");
		var installedMtime = Files.getLastModifiedTime(v1);

		uploadLegacy(distributeDir, "svc", "app.jar", "aaaa"); // 同字节重提，无清单
		assertEquals(0, dm.commit("svc", "v1"), "legacy 同内容重提幂等成功");
		assertEquals("aaaa", Files.readString(v1.resolve("app.jar")));
		assertEquals(installedMtime, Files.getLastModifiedTime(v1), "快速跳装不动已装版本");
		assertTrue(Files.isDirectory(distributeDir.toPath().resolve("svc")), "跳装不消费新上传");
	}
}
