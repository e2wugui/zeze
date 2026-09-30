package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import Zeze.IModule;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND19 GE-D02：commit 版本目录+current 指针布局（拍板方案A）。
 * 直构 DistributeManager（包内构造器，不依赖 Zoker 网络服务），覆盖：
 * 布局形态与current原子切换（临时文件+rename，无半内容/残留tmp）、两步各自失败注入、
 * 重试幂等（目标版本已存在=成功）、旧版本保留策略（现役永不删除）。
 */
@Fast
public class TestD02CommitVersionCurrent {
	private static final long COMMIT_FAIL = IModule.errorCode(Zoker.ModuleId, Zoker.eCommitFail);

	private static void upload(File distributeDir, String serviceName, String content) throws IOException {
		var svc = new File(distributeDir, serviceName);
		Files.createDirectories(svc.toPath());
		Files.writeString(new File(svc, "app.jar").toPath(), content);
	}

	/** services/<svc> 下除版本目录外的杂散文件（current指针、失败残留的tmp）清单。 */
	private static String[] strayFiles(File servicesDir, String serviceName) {
		var svc = new File(servicesDir, serviceName);
		var list = svc.listFiles();
		if (null == list)
			return new String[0];
		return Arrays.stream(list).filter(f -> !f.isDirectory()).map(File::getName).sorted().toArray(String[]::new);
	}

	private static String[] versionDirs(File servicesDir, String serviceName) {
		var svc = new File(servicesDir, serviceName);
		return Arrays.stream(svc.listFiles()).filter(File::isDirectory).map(File::getName).sorted()
				.toArray(String[]::new);
	}

	private static DistributeManager newManager(File distributeDir, File servicesDir) {
		return new DistributeManager(distributeDir, servicesDir);
	}

	/** 全新提交流程：distributes/<svc> → services/<svc>/<v>/（纯新增）→ current 原子切换。 */
	@Test
	public void testCommitHappyPathLayout(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir.toPath());
		upload(distributeDir, "svc", "hello-v1");

		var dm = newManager(distributeDir, servicesDir);
		assertEquals(0, dm.commit("svc", "v1"));

		// 版本目录承接服务文件，distributes被消费
		assertEquals("hello-v1", Files.readString(Path.of(servicesDir.getPath(), "svc", "v1", "app.jar")));
		assertFalse(new File(distributeDir, "svc").exists());
		// current 指针：小文件、内容=版本号
		assertEquals("v1", Files.readString(Path.of(servicesDir.getPath(), "svc", DistributeManager.CURRENT_NAME)));
		// 读路径（startService用）：current 解析到版本目录
		assertEquals(new File(servicesDir, "svc").toPath().resolve("v1"),
				DistributeManager.currentVersionDir(new File(servicesDir, "svc")).toPath());
		// 临时文件+rename 形态：成功切换后无 current.*.tmp 残留
		assertArrayEquals(new String[]{DistributeManager.CURRENT_NAME}, strayFiles(servicesDir, "svc"));
	}

	/** step1 失败（distributes/<svc> 缺失——重复提交/超时重试的典型情形）：无任何副作用。 */
	@Test
	public void testStep1FailNoSideEffect(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir.toPath());

		var dm = newManager(distributeDir, servicesDir);
		assertEquals(COMMIT_FAIL, dm.commit("svc", "v1"));
		// 连 services/<svc> 容器目录也不创建
		assertFalse(new File(servicesDir, "svc").exists());
	}

	/**
	 * 切换失败（step2）注入：current 被"非空目录"占位，move 必失败。
	 * 断言：现役未动（占位物原样）、新版本目录已装好、无 tmp 残留；重试同参数收敛成功。
	 */
	@Test
	public void testSwitchFailKeepsCurrentAndRetryConverges(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir.toPath());
		upload(distributeDir, "svc", "hello-v1");
		var dm = newManager(distributeDir, servicesDir);
		assertEquals(0, dm.commit("svc", "v1"));

		// 现役指针被破坏现场（占位的非空目录）+ 新版本内容已上传
		upload(distributeDir, "svc", "hello-v2");
		var current = Path.of(servicesDir.getPath(), "svc", DistributeManager.CURRENT_NAME);
		Files.delete(current);
		Files.createDirectories(current);
		Files.writeString(current.resolve("blocker"), "x");

		assertEquals(COMMIT_FAIL, dm.commit("svc", "v2"));
		// 新版本已安装（step1成功），占位的current原样保留（切换失败不动现役侧）
		assertEquals("hello-v2", Files.readString(Path.of(servicesDir.getPath(), "svc", "v2", "app.jar")));
		assertTrue(Files.isDirectory(current));
		assertEquals("x", Files.readString(current.resolve("blocker")));
		// 失败路径的 tmp 已清场
		assertArrayEquals(new String[0], strayFiles(servicesDir, "svc"));

		// 现场修复后重试同参数：目标版本已存在=直接进入切换，收敛成功（幂等）
		deleteTree(current.toFile());
		assertEquals(0, dm.commit("svc", "v2"));
		assertEquals("v2", Files.readString(current));
	}

	/**
	 * 重试幂等：成功后同参数重试=成功；step1已成功但切换未做的中间态（版本目录已存在）
	 * 重试=成功。真实重试=重发同一包：跳装的内容判据（逐文件 md5，legacy 形态与
	 * distributes 源文件比对）对同字节必过，已装版本不被覆盖；重发内容与已装版本
	 * 不一致时的处置（换装新内容，不再零比对跳装旧字节）由
	 * TestCommitSameVersionDifferentContent 固化。
	 */
	@Test
	public void testRetryIdempotent(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir.toPath());

		var dm = newManager(distributeDir, servicesDir);
		upload(distributeDir, "svc", "hello-v1");
		assertEquals(0, dm.commit("svc", "v1"));
		// 同参数重试：distributes已被消费（目标版本存在），跳过安装直接切换，仍成功
		assertEquals(0, dm.commit("svc", "v1"));
		assertEquals("v1", Files.readString(Path.of(servicesDir.getPath(), "svc", "current")));
		assertArrayEquals(new String[]{"v1"}, versionDirs(servicesDir, "svc"));

		// 中间态重试形态：模拟"step1成功后中断"——版本目录已存在但current未指，
		// 重试上传与已装内容同字节（部署方重发同一包）
		Files.createDirectories(Path.of(servicesDir.getPath(), "svc", "v3"));
		Files.writeString(Path.of(servicesDir.getPath(), "svc", "v3", "app.jar"), "same-package");
		upload(distributeDir, "svc", "same-package");
		assertEquals(0, dm.commit("svc", "v3"));
		assertEquals("v3", Files.readString(Path.of(servicesDir.getPath(), "svc", "current")));
		// 版本纪律：同字节重试不覆盖已装版本现场（内容判据对同字节必过，快速跳装）
		assertEquals("same-package", Files.readString(Path.of(servicesDir.getPath(), "svc", "v3", "app.jar")));
	}

	/** current 切换是整文件替换（写tmp+rename），非追加：升级后内容恰为新版本号，旧版本留作回滚点。 */
	@Test
	public void testSwitchIsFullReplacement(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir.toPath());
		var dm = newManager(distributeDir, servicesDir);

		upload(distributeDir, "svc", "v1-body");
		assertEquals(0, dm.commit("svc", "v1"));
		upload(distributeDir, "svc", "v2-body");
		assertEquals(0, dm.commit("svc", "v2"));

		assertEquals("v2", Files.readString(Path.of(servicesDir.getPath(), "svc", "current")));
		// 默认保留策略（3版）：旧版本目录留作回滚点
		assertArrayEquals(new String[]{"v1", "v2"}, versionDirs(servicesDir, "svc"));
	}

	/** 保留策略：按安装时间保留最近N版（含现役），最老的删；<=0 全保留。 */
	@Test
	public void testRetentionPolicy(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir.toPath());
		var dm = newManager(distributeDir, servicesDir);

		dm.setKeepVersions(2);
		for (var v : new String[]{"v1", "v2", "v3"}) {
			upload(distributeDir, "svc", v);
			assertEquals(0, dm.commit("svc", v));
		}
		assertArrayEquals(new String[]{"v2", "v3"}, versionDirs(servicesDir, "svc"));

		dm.setKeepVersions(1);
		upload(distributeDir, "svc", "v4");
		assertEquals(0, dm.commit("svc", "v4"));
		assertArrayEquals(new String[]{"v4"}, versionDirs(servicesDir, "svc"));
		assertEquals("v4", Files.readString(Path.of(servicesDir.getPath(), "svc", "current")));

		// 0=全保留
		dm.setKeepVersions(0);
		for (var v : new String[]{"v5", "v6"}) {
			upload(distributeDir, "svc", v);
			assertEquals(0, dm.commit("svc", v));
		}
		assertArrayEquals(new String[]{"v4", "v5", "v6"}, versionDirs(servicesDir, "svc"));
	}

	/** 现役版本永不进入清理面：即使其目录mtime最老（比对非现役版本），prune也只删非现役。 */
	@Test
	public void testPruneNeverDeletesCurrent(@TempDir Path tempDir) throws Exception {
		var servicesDir = tempDir.resolve("services").toFile();
		var svcDir = Path.of(servicesDir.getPath(), "svc");
		var old = Files.createDirectories(svcDir.resolve("old"));
		var recent = Files.createDirectories(svcDir.resolve("recent"));
		Files.writeString(svcDir.resolve("current"), "old");
		// 现役old的mtime最老，非现役recent最新
		var now = System.currentTimeMillis();
		assertTrue(old.toFile().setLastModified(now - 100_000));
		assertTrue(recent.toFile().setLastModified(now));

		var dm = newManager(tempDir.resolve("distributes").toFile(), servicesDir);
		dm.setKeepVersions(1);
		dm.pruneVersions(svcDir.toFile(), "old");
		assertTrue(old.toFile().isDirectory(), "现役版本目录必须保留");
		assertFalse(recent.toFile().exists(), "keep=1时非现役最老（唯一）候选被清理");
	}

	/** unsafe 路径段守卫（GE-C03回归）：serviceName/versionNo 非单段直接拒绝。 */
	@Test
	public void testUnsafeSegmentRejected(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services").toFile();
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir.toPath());
		upload(distributeDir, "svc", "x");
		var dm = newManager(distributeDir, servicesDir);

		assertEquals(COMMIT_FAIL, dm.commit("../evil", "v1"));
		assertEquals(COMMIT_FAIL, dm.commit("svc", "a/b"));
		assertEquals(COMMIT_FAIL, dm.commit("svc", ".."));
		assertFalse(new File(servicesDir, "evil").exists());
	}

	/** current 解析的读路径防御：指针缺失/内容非法/指向不存在版本 → null。 */
	@Test
	public void testCurrentVersionDirResolve(@TempDir Path tempDir) throws Exception {
		var servicesDir = tempDir.resolve("services").toFile();
		var svcDir = Files.createDirectories(Path.of(servicesDir.getPath(), "svc"));

		assertNull(DistributeManager.currentVersionDir(svcDir.toFile()), "无指针");
		Files.createDirectories(svcDir.resolve("v1"));
		Files.writeString(svcDir.resolve("current"), "v1");
		assertEquals(svcDir.resolve("v1"), DistributeManager.currentVersionDir(svcDir.toFile()).toPath());
		// 指针带空白（手工编辑现场）仍解析
		Files.writeString(svcDir.resolve("current"), " v1 \n");
		assertEquals(svcDir.resolve("v1"), DistributeManager.currentVersionDir(svcDir.toFile()).toPath());
		// 指向不存在的版本
		Files.writeString(svcDir.resolve("current"), "v9");
		assertNull(DistributeManager.currentVersionDir(svcDir.toFile()));
		// 非法内容拒绝解析出容器之外
		Files.writeString(svcDir.resolve("current"), "../evil");
		assertNull(DistributeManager.currentVersionDir(svcDir.toFile()));
	}

	private static void deleteTree(File dir) {
		var list = dir.listFiles();
		if (null != list)
			for (var f : list) {
				if (f.isDirectory())
					deleteTree(f);
				else
					assertTrue(f.delete());
			}
		assertTrue(dir.delete());
	}
}
