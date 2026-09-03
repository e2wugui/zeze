package Zeze.Services;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import Zeze.Net.Binary;
import Zeze.Services.ZokerImpl.FileBin;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND-S3-4 回归：FileBin 构造先对目标文件 FileInputStream 做 MD5、后以 "rw" 建文件，
 * 全新文件（含嵌套父目录）首次 OpenFile 必抛 FileNotFoundException；且 RandomAccessFile
 * 不创建父目录，嵌套路径（server/lib/x.jar）还缺 mkdirs。
 * 修复后：先建父目录与文件、再算 md5；断点续传（已有部分数据）行为不变。
 * 自包含（仅临时目录），标 @Fast。
 */
@Fast
public class TestFileBinFreshOpen {
	@Test
	public void testFreshFileWithNestedDirs(@TempDir Path tempDir) throws Exception {
		var baseDir = tempDir.resolve("distributes").toFile();
		// 嵌套路径且文件不存在：修复前 FileInputStream 必抛 FileNotFoundException
		// （即使先建文件，RandomAccessFile("rw") 也不创建父目录）。
		var fileBin = new FileBin("server/lib/x.jar", baseDir, "server/lib/x.jar");
		try {
			Assertions.assertTrue(baseDir.toPath().resolve("server/lib/x.jar").toFile().isFile());
			Assertions.assertEquals(0, fileBin.getLength());

			// 全量写入并校验 md5
			var data = "hello distribute".getBytes(StandardCharsets.UTF_8);
			fileBin.append(0, new Binary(data));
			var expect = MessageDigest.getInstance("MD5");
			expect.update(data);
			Assertions.assertArrayEquals(expect.digest(), fileBin.md5Digest());
		} finally {
			fileBin.close();
		}
	}

	@Test
	public void testResumeKeepsMd5(@TempDir Path tempDir) throws Exception {
		var baseDir = tempDir.resolve("distributes").toFile();
		var part1 = "0123456789".getBytes(StandardCharsets.UTF_8);
		// 第一段写入后关闭（模拟上次中断的断点续传场景）
		var first = new FileBin("a.jar", baseDir, "a.jar");
		first.append(0, new Binary(part1));
		first.close();

		// 重新打开：构造时对已有数据做全量 md5，续传后摘要与整体数据一致
		var resumed = new FileBin("a.jar", baseDir, "a.jar");
		try {
			var part2 = "abcdef".getBytes(StandardCharsets.UTF_8);
			resumed.append(part1.length, new Binary(part2));
			var expect = MessageDigest.getInstance("MD5");
			expect.update(part1);
			expect.update(part2);
			Assertions.assertArrayEquals(expect.digest(), resumed.md5Digest());
		} finally {
			resumed.close();
		}
	}
}
