package Zeze.Services;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import Zeze.Net.Binary;
import Zeze.Services.ZokerImpl.FileBin;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND2-S3-1 回归：FileBin.append 读 channel.size() 前未 flush 缓冲写。
 * BufferedOutputStream 对 len&lt;8192 的写只进缓冲不落 FD：
 * ① 连续两次小块 append，第二次 offset 必大于滞后的 length 读数 → IOException("append out of range")；
 * ② 旧 offset 重试走 truncate 分支时，未 flush 的旧缓冲被重建的 os 静默丢弃，
 * 文件内容与 md5 一致地错（直到 CloseFile 的 MD5 校验才以 eMd5Mismatch 暴露）。
 * 修复后 append 先 flush 再读长度，两种序列均收敛。自包含（仅临时目录），标 @Fast。
 */
@Fast
public class TestFileBinBufferedAppend {
	@Test
	public void testConsecutiveSmallAppends(@TempDir Path tempDir) throws Exception {
		var baseDir = tempDir.resolve("distributes").toFile();
		var fileBin = new FileBin("a.jar", baseDir, "a.jar");
		try {
			var part1 = "0123456789abcdef".getBytes(StandardCharsets.UTF_8); // 16字节，仅进缓冲
			var part2 = "fedcba9876543210".getBytes(StandardCharsets.UTF_8);
			fileBin.append(0, new Binary(part1));
			// 修复前：channel.size()仍为0，offset(16)>length(0)必抛IOException
			fileBin.append(part1.length, new Binary(part2));
			Assertions.assertEquals(part1.length + part2.length, fileBin.getLength());
			var expect = MessageDigest.getInstance("MD5");
			expect.update(part1);
			expect.update(part2);
			Assertions.assertArrayEquals(expect.digest(), fileBin.md5Digest());
			Assertions.assertArrayEquals(concat(part1, part2), Files.readAllBytes(fileBin.getCanonicalFile().toPath()));
		} finally {
			fileBin.close();
		}
	}

	@Test
	public void testRetryOverwriteAfterBufferedAppend(@TempDir Path tempDir) throws Exception {
		var baseDir = tempDir.resolve("distributes").toFile();
		var fileBin = new FileBin("a.jar", baseDir, "a.jar");
		try {
			var first = "first-block-data".getBytes(StandardCharsets.UTF_8); // 15字节，仅进缓冲
			var retry = "retry-block-dat".getBytes(StandardCharsets.UTF_8);
			fileBin.append(0, new Binary(first));
			// 客户端旧offset重试：offset(0) < flush后的真实长度，走truncate分支。
			// 修复前truncate重建os丢弃未flush的first，文件变为first+retry。
			fileBin.append(0, new Binary(retry));
			Assertions.assertEquals(retry.length, fileBin.getLength());
			var expect = MessageDigest.getInstance("MD5");
			expect.update(retry);
			Assertions.assertArrayEquals(expect.digest(), fileBin.md5Digest());
			Assertions.assertArrayEquals(retry, Files.readAllBytes(fileBin.getCanonicalFile().toPath()));
		} finally {
			fileBin.close();
		}
	}

	private static byte[] concat(byte[] a, byte[] b) {
		var r = Arrays.copyOf(a, a.length + b.length);
		System.arraycopy(b, 0, r, a.length, b.length);
		return r;
	}
}
