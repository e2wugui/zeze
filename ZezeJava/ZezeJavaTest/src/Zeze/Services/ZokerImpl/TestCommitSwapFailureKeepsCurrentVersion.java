package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import Zeze.IModule;
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
 * 隔离换装的失败次序：新内容的全部验货（清单校验/清退、源与空目录判别）必须先于
 * 对既有版本目录的腾位，腾位后的安装失败必须回滚（暂存名改回版本名）——否则同版本号
 * 重提不同内容（部署侧忘递增 versionNo）触发换装时，停止态现役（current 指向、
 * 无 run.pid 保护）先被改名进暂存删除名，随后的验货/安装任一失败直接返回：
 * current 悬空（startService 恒 eNoServiceProperties）、旧内容待下轮暂存清扫灭失，
 * 违反"装版本失败无副作用（现役未动）"不变式。
 */
@Fast
public class TestCommitSwapFailureKeepsCurrentVersion {
	private static final long COMMIT_FAIL = IModule.errorCode(Zoker.ModuleId, Zoker.eCommitFail);
	/** 暂存删除名前缀（与 DistributeManager 的常量同字面；测试内联使红态可先于实现编译）。 */
	private static final String STAGE_PREFIX = ".zoker-deleting.";
	private static final boolean WINDOWS =
			System.getProperty("os.name", "").toLowerCase().contains("win");

	/** distributes/&lt;svc&gt;/ 放文件+裸名集合清单（行含服务名首段，与 ZokerAgent 同构）；
	 * manifestExtraLine 非 null 时追加（制造"清单列了但文件缺失"的坏清单形态）。 */
	private static void upload(File distributeDir, String serviceName, String fileName,
							   String content, String manifestExtraLine) throws Exception {
		var svc = distributeDir.toPath().resolve(serviceName);
		Files.createDirectories(svc);
		Files.writeString(svc.resolve(fileName), content);
		var manifest = new StringBuilder(serviceName).append('/').append(fileName).append('\n');
		if (null != manifestExtraLine)
			manifest.append(manifestExtraLine).append('\n');
		Files.writeString(svc.resolve(DistributeManager.DISTRIBUTE_MANIFEST_NAME), manifest.toString());
	}

	private static List<String> stageDirs(Path svcDir) throws Exception {
		try (var list = Files.list(svcDir)) {
			return list.filter(Files::isDirectory).map(p -> p.getFileName().toString())
					.filter(n -> n.startsWith(STAGE_PREFIX)).toList();
		}
	}

	/** 坏清单（条目缺文件=上传中断形态）在腾位之前被拒：现役完好，重传后收敛。
	 * 修复前红点：腾位先于验货——返回 eCommitFail 但 v1 已进暂存删除名、
	 * currentVersionDir 为 null（current 悬空）。 */
	@Test
	public void testManifestValidationFailureHappensBeforeQuarantine(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		var dm = new DistributeManager(distributeDir, servicesDir.toFile());
		var svcDir = servicesDir.resolve("svc");
		var v1 = svcDir.resolve("v1");

		upload(distributeDir, "svc", "app.jar", "old-body", null);
		assertEquals(0, dm.commit("svc", "v1"));
		assertNotNull(DistributeManager.currentVersionDir(svcDir.toFile()), "前置：current 解析到 v1");

		// 同版本号重提不同大小内容（触发隔离换装）+ 坏清单
		upload(distributeDir, "svc", "app.jar", "new-body-of-different-size", "svc/missing.jar");
		assertEquals(COMMIT_FAIL, dm.commit("svc", "v1"), "坏清单必须可见失败");

		assertNotNull(DistributeManager.currentVersionDir(svcDir.toFile()),
				"失败后现役未动：current 仍可解析（修复前悬空为 null）");
		assertEquals(v1, DistributeManager.currentVersionDir(svcDir.toFile()).toPath());
		assertEquals("old-body", Files.readString(v1.resolve("app.jar")), "旧内容原地未动");
		assertEquals(List.of(), stageDirs(svcDir), "未发生腾位（修复前 v1 已进暂存删除名）");

		// 重传正确内容后重试收敛：换装成功
		upload(distributeDir, "svc", "app.jar", "new-body-of-different-size", null);
		assertEquals(0, dm.commit("svc", "v1"));
		assertEquals("new-body-of-different-size", Files.readString(v1.resolve("app.jar")));
		assertEquals("v1", Files.readString(svcDir.resolve(DistributeManager.CURRENT_NAME)));
		assertEquals(List.of(), stageDirs(svcDir), "成功轮的暂存名被同轮 prune 清扫");
	}

	/** 安装 rename 失败（Windows 外部句柄钉住 distributes 源目录内文件）回滚腾位：
	 * 暂存名改回版本名，现役恢复；句柄释放后重试成功。
	 * 修复前红点：腾位无回滚——返回 eCommitFail 且 currentVersionDir 为 null。 */
	@Test
	public void testInstallRenameFailureRollsBackQuarantine(@TempDir Path tempDir) throws Exception {
		Assumptions.assumeTrue(WINDOWS, "句柄阻塞目录 rename 为 Win32 语义");
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		var dm = new DistributeManager(distributeDir, servicesDir.toFile());
		var svcDir = servicesDir.resolve("svc");
		var v1 = svcDir.resolve("v1");

		upload(distributeDir, "svc", "app.jar", "old-body", null);
		assertEquals(0, dm.commit("svc", "v1"));

		upload(distributeDir, "svc", "app.jar", "new-body-of-different-size", null);
		try (var pinned = new RandomAccessFile(
				distributeDir.toPath().resolve("svc").resolve("app.jar").toFile(), "rw")) {
			// 仅持句柄不写：句柄本身阻塞目录 rename，写入会污染暂存的新内容
			assertEquals(COMMIT_FAIL, dm.commit("svc", "v1"), "安装 rename 失败必须可见失败");
		}

		assertNotNull(DistributeManager.currentVersionDir(svcDir.toFile()),
				"回滚后现役恢复（修复前悬空为 null）");
		assertEquals(v1, DistributeManager.currentVersionDir(svcDir.toFile()).toPath());
		assertEquals("old-body", Files.readString(v1.resolve("app.jar")));
		assertEquals(List.of(), stageDirs(svcDir), "回滚清掉暂存名（修复前残留待下轮清扫灭失）");

		assertEquals(0, dm.commit("svc", "v1"), "句柄释放后重试成功（可重试收敛）");
		assertEquals("new-body-of-different-size", Files.readString(v1.resolve("app.jar")));
		assertTrue(Files.isDirectory(v1), "换装后新内容成版");
		assertFalse(Files.exists(distributeDir.toPath().resolve("svc")), "新内容被消费");
	}
}
