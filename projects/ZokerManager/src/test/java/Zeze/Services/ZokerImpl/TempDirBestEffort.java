package Zeze.Services.ZokerImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * @TempDir 兜底删除：子进程 cwd 所在的新建目录可被外部瞬态句柄（AV 实时扫描等）占用，
 * JUnit 的单次删除必偶发失败（test40-4 压测实证 5 红：E02×4+D01×1，全是
 * services\svc\v1 整目录被占而目录内无文件失败——纯目录句柄）。
 * {@code @AfterEach} 先行有界重试熬过扫描窗口（亚秒级），JUnit 随后的删除面对的
 * 多为已清空的根目录，假红面收敛；重试耗尽仍有残留则吞掉——收尾残留不得伪装成
 * 行为断言失败（对齐"等待式断言"假红范式）。
 */
final class TempDirBestEffort {
	private TempDirBestEffort() {
	}

	static void delete(Path root) {
		for (var attempt = 0; attempt < 5; attempt++) {
			try {
				if (Files.notExists(root))
					return;
				try (var walk = Files.walk(root)) {
					walk.sorted(Comparator.reverseOrder()).forEach(p -> {
						try {
							Files.deleteIfExists(p);
						} catch (IOException e) {
							// 本轮未删动（瞬态句柄），下轮重试
						}
					});
				}
				if (Files.notExists(root))
					return;
			} catch (IOException e) {
				// walk 自身失败（根被占），下轮重试
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}
}
