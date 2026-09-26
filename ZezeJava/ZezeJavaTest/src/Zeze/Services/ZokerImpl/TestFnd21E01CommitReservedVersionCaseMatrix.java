package Zeze.Services.ZokerImpl;

import java.io.IOException;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND21 GE-C01：versionNo 保留字排除的大小写/Win32 规范化逃逸（FND20 GE-C02 修复的逃逸）。
 * Windows(NTFS) 路径解析大小写不敏感且剥尾部点/空格，原 {@code CURRENT_NAME.equals(versionNo)}
 * 精确比较只挡逐字节的 "current"：
 * <ul>
 * <li>形态A（指针不存在，首次部署）："Current" 安装成功占据指针固有位置 → switchCurrent 恒
 * AccessDenied → 此后一切合法 commit 卡死（本机 JDK 探针实证：跨大小写 exists==true、
 * ATOMIC_MOVE 对目录目标 AccessDeniedException、重试 exists 仍命中）；</li>
 * <li>形态B（指针已存在）："CURRENT" 的 exists 命中指针文件跳过安装 → switchCurrent 覆盖指针
 * → 返回 0 的<b>假成功</b>，currentVersionDir 恒 null（探针实证 move 覆盖指针文件成功）；</li>
 * <li>尾部点（案卷未测形态，修复时补的矩阵项）："current." 的 renameTo 落盘名就是 current
 * （Win32 规范化剥点，本机探针实证），同形态A链路。</li>
 * </ul>
 * 修复：{@code isReservedVersionName}——剥尾部点/空格后忽略大小写比较，Linux 上 "Current" 本是
 * 合法版本名也一并排除（零成本、跨平台同裁决）。直构 DistributeManager（包内构造器），纯文件逻辑。
 */
@Fast
public class TestFnd21E01CommitReservedVersionCaseMatrix {
	private static final long COMMIT_FAIL = IModule.errorCode(Zoker.ModuleId, Zoker.eCommitFail);

	/** 逃逸矩阵：大小写变体 + Win32 规范化剥尾部点/空格后的碰撞变体。 */
	private static final List<String> ESCAPES = List.of(
			"Current", "CURRENT", "cUrReNt",
			"current.", "current..", "current ", "current .");

	private static void upload(Path distributeDir, String serviceName, String content) throws IOException {
		var svc = distributeDir.resolve(serviceName);
		Files.createDirectories(svc);
		Files.writeString(svc.resolve("app.jar"), content);
	}

	/** 矩阵全量拒绝：错误码 + 现场零残留（distributes 未消费、services 容器不创建）。
	 * 修复前红点：形态B变体在指针已存在时返回 0（假成功），形态A变体消费 distributes。 */
	@Test
	public void testEscapeMatrixRejectedNoFootprint(@TempDir Path tempDir) throws Exception {
		for (var versionNo : ESCAPES) {
			var distributeDir = tempDir.resolve("distributes-" + Integer.toHexString(versionNo.hashCode()));
			var servicesDir = tempDir.resolve("services-" + Integer.toHexString(versionNo.hashCode()));
			Files.createDirectories(distributeDir);
			Files.createDirectories(servicesDir);
			upload(distributeDir, "svc", "evil-" + versionNo);
			var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

			assertEquals(COMMIT_FAIL, dm.commit("svc", versionNo),
					"保留字变体必须被拒: '" + versionNo + "'");
			// 拒绝发生在一切副作用之前（修复前形态A已把 distributes 消费成 services/svc/Current）
			assertTrue(Files.isRegularFile(distributeDir.resolve("svc").resolve("app.jar")),
					"distributes 未被消费: '" + versionNo + "'");
			assertFalse(Files.exists(servicesDir.resolve("svc")),
					"services 容器不得创建: '" + versionNo + "'");
		}
	}

	/** 形态A卡死链断链：变体被拒后同服务的正常 commit 必须成功（判别核心，镜像 FND20 E02）。
	 * 修复前红点："Current" 安装占据 current 位置（探针实证落盘目录名即该位置），
	 * 随后 commit("svc","v1") 的 switchCurrent 恒 AccessDenied，服务容器报废。 */
	@Test
	public void testNormalCommitRecoverableAfterCaseEscapeRejected(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		Files.createDirectories(servicesDir);
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		upload(distributeDir, "svc", "x");
		assertEquals(COMMIT_FAIL, dm.commit("svc", "Current"));
		assertFalse(Files.isDirectory(servicesDir.resolve("svc").resolve(DistributeManager.CURRENT_NAME)),
				"current 位置不得被大小写变体的版本目录占据");

		upload(distributeDir, "svc", "hello-v1");
		assertEquals(0, dm.commit("svc", "v1"), "被拒后正常 commit 必须成功（卡死链已断）");
		assertEquals("v1", Files.readString(servicesDir.resolve("svc").resolve(DistributeManager.CURRENT_NAME)));
		assertEquals("hello-v1", Files.readString(servicesDir.resolve("svc").resolve("v1").resolve("app.jar")));
	}

	/** 形态B假成功：指针已存在的存量服务上 "CURRENT" 必须被拒，指针与现役解析不受污染。
	 * 修复前红点：返回 0（假成功）且指针内容被改写成 "CURRENT"（指向指针文件自身非目录），
	 * currentVersionDir 恒 null，重试同参数永远走同一假成功路径（exists 恒命中指针文件）。 */
	@Test
	public void testExistingPointerNotHijackedByCaseEscape(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes");
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir);
		Files.createDirectories(servicesDir);
		// 存量服务：现役 v0 + 指针
		var svc = servicesDir.resolve("svc");
		Files.createDirectories(svc.resolve("v0"));
		Files.writeString(svc.resolve("v0").resolve("app.jar"), "v0-body");
		Files.writeString(svc.resolve(DistributeManager.CURRENT_NAME), "v0");
		upload(distributeDir, "svc", "evil-upper");
		var dm = new DistributeManager(distributeDir.toFile(), servicesDir.toFile());

		assertEquals(COMMIT_FAIL, dm.commit("svc", "CURRENT"), "指针已存在时大小写变体必须被拒（不得假成功）");
		assertEquals("v0", Files.readString(svc.resolve(DistributeManager.CURRENT_NAME)), "指针内容不得被污染");
		assertNotNull(DistributeManager.currentVersionDir(svc.toFile()), "现役解析不受影响");
		assertTrue(Files.isRegularFile(distributeDir.resolve("svc").resolve("app.jar")), "distributes 未被消费");

		// 拒绝后合法 commit 仍正常收敛
		upload(distributeDir, "svc", "hello-v1");
		assertEquals(0, dm.commit("svc", "v1"));
		assertEquals("hello-v1", Files.readString(DistributeManager.currentVersionDir(svc.toFile()).toPath().resolve("app.jar")));
	}
}
