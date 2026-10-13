package Zeze.Services.ZokerImpl;

import harness.Extra;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import Zeze.IModule;
import Zeze.Services.Zoker;
import harness.Fast;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * commit 对裸名清单（.zoker-manifest）回落的归属防线：版本限定清单
 * （.zoker-manifest.&lt;versionNo&gt;）按部署归属互不覆盖，但清退幸免面只有裸名与
 * 本次限定名——另一并发分发会话的数据文件与限定清单被当残留删除后，该会话的
 * commit 经裸名回落消费到<b>对方</b>的清单副本：校验对方条目、安装对方文件、以
 * 自己的 versionNo 成版切 current 返回 0——静默错版成现役。
 * 修复后：本次限定名缺失而暂存区存在他版本限定清单时拒绝裸名回落（eCommitFail
 * 响亮失败，重传补齐自己的限定清单后正常消费）；无任何限定清单的 legacy 形态
 * 回落语义不变。
 */
@Fast
@Extra
public class TestCommitForeignManifestFallbackRefused {
	private static final long COMMIT_FAIL = IModule.errorCode(Zoker.ModuleId, Zoker.eCommitFail);
	private static final boolean WINDOWS =
			System.getProperty("os.name", "").toLowerCase().contains("win");

	/** distributes/svc/ 放数据文件（内容=body-<名>）+ 按名写清单（行含服务名首段）。 */
	private static void stage(Path distributeDir, String[] files, String[] manifests,
							  String[] manifestLines) throws Exception {
		var svc = distributeDir.resolve("svc");
		Files.createDirectories(svc);
		for (var rel : files)
			Files.writeString(svc.resolve(rel), "body-of-" + rel);
		for (var manifest : manifests) {
			var lines = new StringBuilder();
			for (var line : manifestLines)
				lines.append(line).append('\n');
			Files.writeString(svc.resolve(manifest), lines.toString());
		}
	}

	/** 裸名回落消费他方清单必须拒绝：暂存区呈现"先到 commit 清退后"的形态（B 会话的
	 * 数据与 .zoker-manifest.v2 已被 A 的 commit 删除、A 的安装失败遗留暂存区），
	 * B 的 commit 不得拿 A 的裸名清单成版 v2。
	 * 修复前红点：回落消费 A 的条目全过——v2 内容=A 的文件、current=v2、返回 0。 */
	@Test
	public void testBareFallbackRefusedWhenOtherVersionManifestPresent(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		// A(v1) 会话遗留：数据文件 + 自己的限定清单 + 裸名兼容副本（内容=A 的集合）
		stage(distributeDir, new String[]{"a1.jar", "a2.jar"},
				new String[]{".zoker-manifest.v1", ".zoker-manifest"},
				new String[]{"svc/a1.jar", "svc/a2.jar"});
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		assertEquals(COMMIT_FAIL, dm.commit("svc", "v2"),
				"限定清单缺失而他版本限定清单在场：裸名内容无法证明归属本次部署，拒绝");

		assertFalse(Files.exists(servicesDir.resolve("svc").resolve("v2")), "不得错版成版");
		assertTrue(Files.isRegularFile(distributeDir.resolve("svc").resolve("a1.jar")),
				"拒绝发生在清退/安装之前，暂存区原样保留（可重传收敛）");
	}

	/** 完整交错叙事（Windows）：双会话并发，A 先 commit 且安装失败（外部句柄钉住
	 * distributes）——A 的清退已删除 B 的数据与限定清单；B 后 commit 不得经裸名回落
	 * 静默错版成现役；B 重传自己的集合后正确成版。
	 * 修复前红点：B 的 commit 返回 0 且 v2 内容=A 的文件（错版切 current）。 */
	@Test
	public void testConcurrentInterleaveNeverSilentWrongVersion(@TempDir Path tempDir) throws Exception {
		Assumptions.assumeTrue(WINDOWS, "句柄阻塞目录 rename 为 Win32 语义");
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		// A(v1) 与 B(v2) 双会话上传完毕；裸名最后写者=A（清单内容=A 的集合）
		stage(distributeDir, new String[]{"a1.jar", "a2.jar", "b1.jar"},
				new String[]{".zoker-manifest.v1", ".zoker-manifest.v2", ".zoker-manifest"},
				new String[]{"svc/a1.jar", "svc/a2.jar"});
		Files.writeString(distributeDir.resolve("svc").resolve(".zoker-manifest.v2"), "svc/b1.jar\n");
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		// A 先 commit：清单校验/清退通过（B 的 b1 与 .zoker-manifest.v2 被清退），
		// 安装 rename 被钉住失败——A 可见失败，暂存区遗留 A 的集合与裸名副本
		try (var pinned = new RandomAccessFile(
				distributeDir.resolve("svc").resolve("a1.jar").toFile(), "rw")) {
			assertEquals(COMMIT_FAIL, dm.commit("svc", "v1"), "A 的安装失败可见");
		}

		// B 后 commit：自己的限定清单已被 A 的清退删除，裸名=A 的清单——拒绝回落
		assertEquals(COMMIT_FAIL, dm.commit("svc", "v2"),
				"不得消费他方裸名清单静默错版（修复前返回 0 且 v2=A 的内容）");

		// B 重传自己的集合（数据+限定清单+裸名副本）后正确成版
		Files.writeString(distributeDir.resolve("svc").resolve("b1.jar"), "body-of-b1.jar");
		Files.writeString(distributeDir.resolve("svc").resolve(".zoker-manifest.v2"), "svc/b1.jar\n");
		Files.writeString(distributeDir.resolve("svc").resolve(".zoker-manifest"), "svc/b1.jar\n");
		assertEquals(0, dm.commit("svc", "v2"), "重传补齐自己的限定清单后正常消费");

		var v2 = servicesDir.resolve("svc").resolve("v2");
		assertEquals("body-of-b1.jar", Files.readString(v2.resolve("b1.jar")), "v2 内容=B 的集合");
		assertFalse(Files.exists(v2.resolve("a1.jar")), "A 的遗留数据不混入 B 的版本");
		assertEquals("v2", Files.readString(servicesDir.resolve("svc")
				.resolve(DistributeManager.CURRENT_NAME)));
	}

	/** legacy 形态（无任何限定清单）的裸名回落语义不变：旧客户端/兼容副本照常消费。 */
	@Test
	public void testBareFallbackStillWorksWhenNoVersionedManifests(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		stage(distributeDir, new String[]{"a1.jar"},
				new String[]{".zoker-manifest"}, new String[]{"svc/a1.jar"});
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		assertEquals(0, dm.commit("svc", "v9"), "无他版本限定清单在场：裸名回落照常（legacy）");
		var v9 = servicesDir.resolve("svc").resolve("v9");
		assertEquals("body-of-a1.jar", Files.readString(v9.resolve("a1.jar")));
		assertTrue(Files.isRegularFile(v9.resolve(".zoker-manifest")), "裸名清单随版本成版");
	}
}
