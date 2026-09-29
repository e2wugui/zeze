package Zeze.Services.ZokerImpl;

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
 * 版本号不变），修复前判据只验"清单条目在已装版本目录<b>存在</b>"（纯存在性）——
 * CloseFile 已 md5 验证的暂存区新字节与已装旧字节之间零比对，commit 回执 0 静默跳装：
 * 现役继续服务旧字节、新字节滞留 distributes（下次 commit 被当残留清退），全程无日志。
 * 修复后跳装判据增<b>文件大小比对</b>（暂存区新字节 vs 已装版本目录字节）：清单行
 * 无摘要字段（FND26 格式=纯路径），哈希需清单格式与部署工具协同演进，大小是存在性
 * 之上的最小实质升级；大小不符判不可收养走隔离换装，新内容实际成版。同大小不同
 * 字节仍跳装（接受残余，见 testSameSizeDifferentContentStillSkips 的边界固化）。
 */
@Fast
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

	/** 边界固化：同大小不同字节仍快速跳装（幂等重试语义优先；清单无摘要字段，大小比对
	 * 之上的收口需清单行携带 md5 的格式演进，接受残余）。 */
	@Test
	public void testSameSizeDifferentContentStillSkips(@TempDir Path tempDir) throws Exception {
		var distributeDir = tempDir.resolve("distributes").toFile();
		var servicesDir = tempDir.resolve("services");
		Files.createDirectories(distributeDir.toPath());
		Files.createDirectories(servicesDir);
		var dm = new DistributeManager(distributeDir, servicesDir.toFile());

		upload(distributeDir, "svc", "app.jar", "aaaa");
		assertEquals(0, dm.commit("svc", "v1"));
		var v1 = servicesDir.resolve("svc").resolve("v1");

		upload(distributeDir, "svc", "app.jar", "bbbb"); // 同大小不同字节
		assertEquals(0, dm.commit("svc", "v1"), "同大小内容仍走快速跳装幂等分支");
		assertEquals("aaaa", Files.readString(v1.resolve("app.jar")), "已装版本不动（版本纪律）");
		assertTrue(Files.isDirectory(distributeDir.toPath().resolve("svc")), "跳装不消费新上传");
	}
}
