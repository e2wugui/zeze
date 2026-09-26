package harness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * 测试目录清理。不用@TempDir：LogIndex的mmap在Windows下持有索引文件句柄，
 * 目录删不掉会让JUnit清理阶段失败，故walk+逆序尽力删，残余留给系统临时目录清理。
 */
public final class DirCleanup {
	private DirCleanup() {
	}

	public static void deleteBestEffort(Path dir) {
		try (var walk = Files.walk(dir)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (IOException e) {
					// 尽力删除，留給系统临时目录清理。
				}
			});
		} catch (IOException e) {
			// ignore
		}
	}
}
