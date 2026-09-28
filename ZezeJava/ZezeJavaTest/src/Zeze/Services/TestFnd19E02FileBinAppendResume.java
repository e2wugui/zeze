package Zeze.Services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;

import Zeze.Net.Binary;
import Zeze.Services.ZokerImpl.FileBin;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FND19 GE-C02：FileBin断点续传等值分支（offset==length）必须seek到offset再写。
 * 新建FileBin的共享FD指针停留在0，不seek时续传chunk从文件头覆写——多chunk场景第二个chunk
 * 因offset&gt;length抛IOException永久卡死；单chunk场景两端md5都按"旧内容+新chunk"计算，
 * 损坏文件通过校验上线（audit-FND19/raw/FdPositionTest.java实证脚本的转正）。
 */
@Fast
public class TestFnd19E02FileBinAppendResume {
	private static byte[] filled(int len, byte b) {
		var data = new byte[len];
		Arrays.fill(data, b);
		return data;
	}

	private static void append(FileBin fileBin, long offset, byte[] data) throws Exception {
		fileBin.append(offset, new Binary(data));
	}

	/** 断点续传核心场景：文件已有N字节 + 新建FileBin + append(N)。 */
	@Test
	public void testAppendAtLengthWritesAtTail(@TempDir Path tempDir) throws Exception {
		var pre = filled(100, (byte)'A');
		Files.write(tempDir.resolve("svc.jar"), pre);

		var fileBin = new FileBin("svc.jar", tempDir.toFile(), "svc.jar");
		try {
			assertEquals(100, fileBin.getLength());
			var tail = filled(10, (byte)'B');
			append(fileBin, 100, tail);

			assertEquals(110, fileBin.getLength(), "等值续传后长度必须是110");
			var disk = Files.readAllBytes(fileBin.getCanonicalFile().toPath());
			assertEquals(110, disk.length);
			assertArrayEquals(pre, Arrays.copyOfRange(disk, 0, 100), "已存在的100字节必须原样保留（修复前被覆写）");

			// 服务端md5记账也必须与磁盘实际内容一致（单chunk静默损坏路径）
			var expected = MessageDigest.getInstance("MD5");
			expected.update(pre);
			expected.update(tail);
			assertArrayEquals(expected.digest(), fileBin.md5Digest());
		} finally {
			fileBin.close();
		}
	}

	/** 多chunk续传：等值分支连续命中，修复前第二个chunk必offset&gt;length抛异常。 */
	@Test
	public void testMultiChunkResume(@TempDir Path tempDir) throws Exception {
		Files.write(tempDir.resolve("svc.jar"), filled(100, (byte)'A'));

		var fileBin = new FileBin("svc.jar", tempDir.toFile(), "svc.jar");
		try {
			var chunk1 = filled(16 * 1024, (byte)'1');
			var chunk2 = filled(2 * 1024, (byte)'2');
			append(fileBin, 100, chunk1);
			append(fileBin, 100 + chunk1.length, chunk2);

			var disk = Files.readAllBytes(fileBin.getCanonicalFile().toPath());
			assertEquals(100 + chunk1.length + chunk2.length, disk.length);
			assertArrayEquals(filled(100, (byte)'A'), Arrays.copyOfRange(disk, 0, 100));
			assertArrayEquals(chunk1, Arrays.copyOfRange(disk, 100, 100 + chunk1.length));
			assertArrayEquals(chunk2, Arrays.copyOfRange(disk, 100 + chunk1.length, disk.length));
		} finally {
			fileBin.close();
		}
	}

	/** 全新文件从0续传与truncate分支回归。 */
	@Test
	public void testFreshFileAndTruncateBranch(@TempDir Path tempDir) throws Exception {
		var fileBin = new FileBin("new.jar", tempDir.toFile(), "new.jar");
		try {
			append(fileBin, 0, filled(10, (byte)'X'));
			assertEquals(10, fileBin.getLength());
		} finally {
			fileBin.close();
		}
		assertArrayEquals(filled(10, (byte)'X'), Files.readAllBytes(tempDir.resolve("new.jar")));

		Files.write(tempDir.resolve("t.jar"), filled(100, (byte)'A'));
		var trunc = new FileBin("t.jar", tempDir.toFile(), "t.jar");
		try {
			append(trunc, 50, filled(10, (byte)'Y')); // offset<length走truncate分支
			assertEquals(60, trunc.getLength());
		} finally {
			trunc.close();
		}
		var expected = new byte[60];
		System.arraycopy(filled(50, (byte)'A'), 0, expected, 0, 50);
		System.arraycopy(filled(10, (byte)'Y'), 0, expected, 50, 10);
		assertArrayEquals(expected, Files.readAllBytes(tempDir.resolve("t.jar")));
	}

	/** offset&gt;length仍然必须拒绝（越界防御不变）。 */
	@Test
	public void testAppendOutOfRangeRejected(@TempDir Path tempDir) throws Exception {
		Files.write(tempDir.resolve("svc.jar"), filled(100, (byte)'A'));
		var fileBin = new FileBin("svc.jar", tempDir.toFile(), "svc.jar");
		try {
			assertThrows(IOException.class, () -> append(fileBin, 101, filled(10, (byte)'B')));
		} finally {
			fileBin.close();
		}
	}
}
