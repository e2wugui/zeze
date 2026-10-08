package Zeze.Services.ZokerImpl;

import java.io.IOException;
import java.nio.file.Path;

import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND19 GE-C03：ZokerImpl.DistributeManager对网络输入路径的包含性校验。
 * serviceName/fileName直接来自OpenFile RPC、serviceName/versionNo直接来自CommitService RPC，
 * 不校验时"../"与绝对路径可把写/截断/rename指向distributes、services目录之外（Hot侧同构缺陷28426a1c4已修）。
 */
@Fast
public class TestPathGuard {
	@Test
	public void testCheckInsideDirAcceptsNestedRelative(@TempDir Path tempDir) {
		var base = tempDir.toFile();
		assertDoesNotThrow(() -> DistributeManager.checkInsideDir(base, "svc/lib/x.jar"));
		assertDoesNotThrow(() -> DistributeManager.checkInsideDir(base, "svc"));
		// 进入base再折返仍留在base内：允许（落地位置无害）
		assertDoesNotThrow(() -> DistributeManager.checkInsideDir(base, "a/../b.jar"));
	}

	@Test
	public void testCheckInsideDirRejectsEscape(@TempDir Path tempDir) {
		var base = tempDir.resolve("distributes").toFile();
		assertThrows(IOException.class, () -> DistributeManager.checkInsideDir(base, "../escape.jar"));
		assertThrows(IOException.class, () -> DistributeManager.checkInsideDir(base, "svc/../../escape.jar"));
		// 兄弟目录前缀字符串相同也不允许：以分隔符为界
		assertThrows(IOException.class, () -> DistributeManager.checkInsideDir(base, "../distributes2/x.jar"));
	}

	@Test
	public void testCheckInsideDirRejectsAbsolute(@TempDir Path tempDir) {
		var base = tempDir.toFile();
		// 平台各自的绝对路径形态：windows根相对"/abs/..."、POSIX绝对"/abs/..."，resolve后都不再位于base内
		assertThrows(IOException.class, () -> DistributeManager.checkInsideDir(base, "/abs/escape.jar"));
	}

	@Test
	public void testIsSafePathSegment() {
		assertTrue(DistributeManager.isSafePathSegment("svc1"));
		assertTrue(DistributeManager.isSafePathSegment("v1.0.0"));

		assertFalse(DistributeManager.isSafePathSegment(null));
		assertFalse(DistributeManager.isSafePathSegment(""));
		assertFalse(DistributeManager.isSafePathSegment("."));
		assertFalse(DistributeManager.isSafePathSegment(".."));
		assertFalse(DistributeManager.isSafePathSegment("a/b"));
		assertFalse(DistributeManager.isSafePathSegment("a\\b"));
		assertFalse(DistributeManager.isSafePathSegment("C:evil"));
		// 组合逃逸："sub/../../services/x"整段拒绝（startsWith检查挡不住这种回到目标目录内的拼法）
		assertFalse(DistributeManager.isSafePathSegment("sub/../../services/x"));
	}
}
