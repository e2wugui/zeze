package Zeze.Hot;

import java.io.IOException;
import java.nio.file.Path;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND-G1-3 回归：DistributeManager.open 的网络入参 fileName 必须被限制在 distributeDir 之内。
 * 这里直接测试包级校验方法 checkFileNameInsideDir（open 在校验通过后才创建 FileBin）。
 * 自包含（仅临时目录与纯路径计算），标 @Fast。
 */
@Fast
public class TestDistributeManagerPathGuard {
	@Test
	public void testRejectEscape(@TempDir Path tempDir) {
		var distributeDir = tempDir.resolve("distributes").toString();
		// "../" 逐级逃逸
		Assertions.assertThrows(IOException.class,
				() -> DistributeManager.checkFileNameInsideDir(distributeDir, "../evil.jar"));
		// 子目录内"../"逃逸（server 上一级再一级即出 distributeDir；注意两级 ../ 从 lib
		// 只回到 distributeDir 根，属目录内合法形态，见 testAcceptInside）
		Assertions.assertThrows(IOException.class,
				() -> DistributeManager.checkFileNameInsideDir(distributeDir, "server/../../evil.jar"));
		// 平台分隔符的逃逸（windows 下为反斜杠形态）
		Assertions.assertThrows(IOException.class,
				() -> DistributeManager.checkFileNameInsideDir(distributeDir,
						".." + java.io.File.separator + "evil.jar"));
		// 绝对路径：Path.resolve 对绝对路径直接返回自身，必越界
		var absolute = Path.of(tempDir.toAbsolutePath().getParent().toString(), "outside.jar").toString();
		Assertions.assertThrows(IOException.class,
				() -> DistributeManager.checkFileNameInsideDir(distributeDir, absolute));
	}

	@Test
	public void testAcceptInside(@TempDir Path tempDir) throws IOException {
		var distributeDir = tempDir.resolve("distributes").toString();
		// 常规发布文件名（服务名前缀+子目录）
		var target = DistributeManager.checkFileNameInsideDir(distributeDir, "server/lib/x.jar");
		Assertions.assertTrue(target.startsWith(Path.of(distributeDir).toAbsolutePath().normalize()));
		// "a/../b" 规范化后仍在目录内，应放行
		var target2 = DistributeManager.checkFileNameInsideDir(distributeDir, "a/../b.jar");
		Assertions.assertEquals(Path.of(distributeDir).toAbsolutePath().normalize().resolve("b.jar"), target2);
	}
}
