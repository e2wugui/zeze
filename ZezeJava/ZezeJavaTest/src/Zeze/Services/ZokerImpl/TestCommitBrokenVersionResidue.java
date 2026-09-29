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
 * commit 跳装分支的完整性判据：版本名位置的目录"存在"不再无条件等价于"完整"。
 * 修复前 {@code versionTo.exists()} 命中即整段跳过安装——prune 的 deleteTree 部分失败
 * （Windows 外部句柄占用，warn 后接受残缺）制造的"存在但不完整"版本目录被跳装收养：
 * 复用同版本号重提的新上传内容被静默忽略、current 切到残缺目录并返回 0（假成功），
 * start 恒 eNoServiceProperties，且残缺目录获现役保护被 prune 永久豁免（无自愈）。
 * 修复后三面闭合：prune/隔离换装先原子改名进暂存删除名再清树（版本名位置不再出现
 * 残缺目录）；跳装前校验（自身清单=安装完成标志 / legacy 地板=至少一个常规文件 /
 * 新清单条目须全部已在盘上）；判不可收养时有新内容→隔离换装（残缺目录原子改名腾位，
 * 新内容落正常安装分支），无新内容→eCommitFail（假成功变可见失败）。
 */
@Fast
public class TestCommitBrokenVersionResidue {
	private static final long COMMIT_FAIL = IModule.errorCode(Zoker.ModuleId, Zoker.eCommitFail);
	/** 暂存删除名前缀（与 DistributeManager 的常量同字面；测试内联使红态可先于实现编译）。 */
	private static final String STAGE_PREFIX = ".zoker-deleting.";

	/** distributes/&lt;svc&gt; 放文件（manifest 非 null 时补集合清单，行含服务名首段，与ZokerAgent同构）。 */
	private static void upload(File distributeDir, String serviceName, List<String> files,
							   List<String> manifestLines) throws Exception {
		var svc = distributeDir.toPath().resolve(serviceName);
		Files.createDirectories(svc);
		for (var rel : files) {
			var p = svc.resolve(rel);
			Files.createDirectories(p.getParent());
			Files.writeString(p, "new-" + rel);
		}
		if (null != manifestLines) {
			var lines = new StringBuilder();
			for (var line : manifestLines)
				lines.append(serviceName).append('/').append(line).append('\n');
			Files.writeString(svc.resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME), lines.toString());
		}
	}

	/** services/&lt;svc&gt;/&lt;version&gt; 直接摆版本目录现场（manifestLines 可列 files 之外的
	 * 名字——制造"清单列了但文件被削掉"的残缺形态）。 */
	private static void layoutVersion(Path servicesDir, String serviceName, String version,
									  List<String> files, List<String> manifestLines) throws Exception {
		var dir = servicesDir.resolve(serviceName).resolve(version);
		Files.createDirectories(dir);
		for (var rel : files) {
			var p = dir.resolve(rel);
			Files.createDirectories(p.getParent());
			Files.writeString(p, "old-" + rel);
		}
		if (null != manifestLines) {
			var lines = new StringBuilder();
			for (var line : manifestLines)
				lines.append(serviceName).append('/').append(line).append('\n');
			Files.writeString(dir.resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME), lines.toString());
		}
	}

	private static List<String> stageDirs(Path svcDir) throws Exception {
		try (var list = Files.list(svcDir)) {
			return list.filter(Files::isDirectory).map(p -> p.getFileName().toString())
					.filter(n -> n.startsWith(STAGE_PREFIX)).toList();
		}
	}

	/** 残缺（自带清单列了 lib/x.jar 但文件缺失=prune 部分删除残留形态）+ 新内容带清单：
	 * 残缺不可收养——隔离换装，commit 返回 0 且 v1 内容=新内容、暂存名同轮 prune 清扫。
	 * 修复前红点：返回 0 但 v1 残缺保留、新内容滞留 distributes（假成功）。 */
	@Test
	public void testBrokenSelfManifestResidueSwappedWithNewContent(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		layoutVersion(servicesDir, "svc", "v1", List.of("app.jar"), List.of("app.jar", "lib/x.jar"));
		upload(distributeDir, "svc", List.of("app.jar", "lib/x.jar"), List.of("app.jar", "lib/x.jar"));

		var dm = new DistributeManager(distributeDir, servicesDir.toFile());
		assertEquals(0, dm.commit("svc", "v1"), "隔离换装后 commit 成功");

		var v1 = servicesDir.resolve("svc").resolve("v1");
		assertEquals("new-app.jar", Files.readString(v1.resolve("app.jar")), "v1 内容=新内容（残缺不被收养）");
		assertEquals("new-lib/x.jar", Files.readString(v1.resolve("lib/x.jar")));
		assertFalse(Files.exists(distributeDir.toPath().resolve("svc")), "新内容被消费");
		assertEquals(List.of(), stageDirs(servicesDir.resolve("svc")), "隔离暂存名本轮 prune 已清扫");
		assertEquals("v1", Files.readString(servicesDir.resolve("svc").resolve(DistributeManager.CURRENT_NAME)));
	}

	/** 残缺（空版本目录=deleteTree 清空但目录删除失败的残留形态）且无新内容：
	 * eCommitFail。修复前红点：返回 0 且 current 切到残缺目录（start 恒 eNoServiceProperties）。 */
	@Test
	public void testBrokenResidueWithoutDistributeRejected(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		Files.createDirectories(servicesDir.resolve("svc").resolve("v1"));

		var dm = new DistributeManager(distributeDir, servicesDir.toFile());
		assertEquals(COMMIT_FAIL, dm.commit("svc", "v1"), "残缺无新内容可换装：可见失败而非假成功");
		// 无副作用：current 不被切到残缺目录
		assertFalse(Files.exists(servicesDir.resolve("svc").resolve(DistributeManager.CURRENT_NAME)));
	}

	/** legacy 地板：空版本目录 + 新内容无清单 → 隔离换装成功（地板=目录树内至少一个
	 * 常规文件才可收养，完全空壳不可收养）。修复前红点：空壳被跳装收养。 */
	@Test
	public void testEmptyLegacyResidueSwappedWithNewContent(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		Files.createDirectories(servicesDir.resolve("svc").resolve("v1"));
		upload(distributeDir, "svc", List.of("app.jar"), null);

		var dm = new DistributeManager(distributeDir, servicesDir.toFile());
		assertEquals(0, dm.commit("svc", "v1"), "空壳不可收养，隔离换装新内容");

		var v1 = servicesDir.resolve("svc").resolve("v1");
		assertTrue(Files.isRegularFile(v1.resolve("app.jar")), "新内容落版本目录");
		assertEquals("new-app.jar", Files.readString(v1.resolve("app.jar")));
		assertEquals("v1", Files.readString(servicesDir.resolve("svc").resolve(DistributeManager.CURRENT_NAME)));
		assertEquals(List.of(), stageDirs(servicesDir.resolve("svc")));
	}

	/** 幂等护栏（绿）：同清单同内容重提——清单条目全部已在盘上版本目录，快速跳装：
	 * 不发生隔离改名（v1 内容与 mtime 均不变），新上传内容按跳装既有语义滞留 distributes。 */
	@Test
	public void testSameManifestResubmitSkipsInstallWithoutQuarantine(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		var files = List.of("app.jar", "lib/x.jar");
		upload(distributeDir, "svc", files, files);
		var dm = new DistributeManager(distributeDir, servicesDir.toFile());
		assertEquals(0, dm.commit("svc", "v1"));

		var v1 = servicesDir.resolve("svc").resolve("v1");
		var installedMtime = Files.getLastModifiedTime(v1);
		upload(distributeDir, "svc", files, files); // 同内容重提
		assertEquals(0, dm.commit("svc", "v1"), "同集合重提幂等成功");

		assertEquals("new-app.jar", Files.readString(v1.resolve("app.jar")), "已装版本内容不变");
		assertEquals(installedMtime, Files.getLastModifiedTime(v1), "快速跳装不动已装版本（无隔离改名）");
		assertEquals(List.of(), stageDirs(servicesDir.resolve("svc")));
		assertTrue(Files.isDirectory(distributeDir.toPath().resolve("svc")), "跳装不消费新上传（幂等语义）");
	}

	/** 暂存删除名族不得用作版本号：折叠后以前缀开头的 versionNo 按保留字拒绝——
	 * 否则 exists 跳装/指针可收养一个 deleteTree 半途残缺的暂存目录。
	 * 修复前红点：isReservedVersionName 不含该族，commit 直接成版。 */
	@Test
	public void testDeletingStagePrefixRejectedAsVersionName(@TempDir Path tempDir) throws Exception {
		assertTrue(DistributeManager.isReservedVersionName(".zoker-deleting.x"));
		assertTrue(DistributeManager.isReservedVersionName(".ZOKER-DELETING.v1.123"), "折叠（小写）判同");
		assertFalse(DistributeManager.isReservedVersionName("v1"));
		assertFalse(DistributeManager.isReservedVersionName(".zoker-deleting"),
				"无后续内容的字面名不在暂存名族内（暂存名恒为前缀+原名+毫秒）");

		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		upload(distributeDir, "svc", List.of("app.jar"), null);
		var dm = new DistributeManager(distributeDir, servicesDir.toFile());
		assertEquals(COMMIT_FAIL, dm.commit("svc", ".zoker-deleting.x"), "暂存删除名形态的版本号必须拒绝");
		assertFalse(Files.exists(servicesDir.resolve("svc")), "拒绝无副作用");
	}
}
