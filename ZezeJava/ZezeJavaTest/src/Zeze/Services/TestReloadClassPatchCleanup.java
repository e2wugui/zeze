package Zeze.Services;

import java.nio.file.Files;
import java.nio.file.Path;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND12 svc-02回归：ReloadClassServer.onEndRequest原只删除与新补丁同名的destFile，
 * 不同文件名的补丁（版本号/日期命名是现实运维形态）在uploadDir累积——运行期热更
 * 本身成功，但下次重启start()对files.length!=1抛"too many patch file"阻断启动链。
 * 修复=成功热更后清理目录内其他文件（start()按文件数而非扩展名判定，非zip文件
 * 同样破坏唯一补丁不变式，一并清理）。
 */
@Fast
public class TestReloadClassPatchCleanup {

	@Test
	public void testCleanupKeepsOnlyCurrentPatch(@TempDir Path dir) throws Exception {
		var old = Files.write(dir.resolve("patch_a.zip"), new byte[]{1});
		var current = Files.write(dir.resolve("patch_b.zip"), new byte[]{2});
		var stray = Files.write(dir.resolve("note.txt"), new byte[]{3});
		ReloadClassServer.cleanupOtherPatchFiles(dir.toFile(), current.toFile());
		Assertions.assertTrue(Files.exists(current), "current patch must be kept");
		Assertions.assertFalse(Files.exists(old), "old patch must be removed");
		Assertions.assertFalse(Files.exists(stray), "non-patch file must be removed (start() counts all files)");
	}
}
