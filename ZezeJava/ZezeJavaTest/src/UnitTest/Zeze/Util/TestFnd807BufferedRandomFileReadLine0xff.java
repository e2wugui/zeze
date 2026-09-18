package UnitTest.Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Util.BufferedRandomFile;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-07回归：私有read()返回有符号byte，数据字节0xFF得到-1与fillBuffer失败（真EOF）
 * 无法区分——readLine把0xFF当行结束，0xFF在行首时返回null被Log4jFileSession解释为
 * 文件结束，其后全部日志在查询结果与时间索引中不可见。修复：read()/peek()返回
 * 无符号（&0xff），-1唯一表示eof（对齐RandomAccessFile.readLine契约）。
 */
@Fast
public class TestFnd807BufferedRandomFileReadLine0xff {

	@Test
	public void testDataByte0xffIsNotEof() throws Exception {
		var file = Path.of("TestFnd807BufferedRandomFileReadLine0xff.tmp");
		try {
			// 0xFF在行中、行首两种位置都覆盖；ISO-8859-1下0xFF即字符ÿ
			Files.write(file, new byte[]{'a', '\n', (byte)0xFF, 't', 'a', 'i', 'l', '\n',
					'b', '\n', (byte)0xFF, '\r', '\n', (byte)0xFF, (byte)0xFF});
			try (var f = new BufferedRandomFile(file.toFile(), StandardCharsets.ISO_8859_1)) {
				Assertions.assertEquals("a", f.readLine());
				// 修复前红：行中0xFF截断为""（0xFF被吞），行首0xFF返回null（假EOF）
				Assertions.assertEquals("\u00FFtail", f.readLine());
				Assertions.assertEquals("b", f.readLine());
				Assertions.assertEquals("\u00FF", f.readLine()); // 0xFF+\r\n
				Assertions.assertEquals("\u00FF\u00FF", f.readLine()); // 无换行尾行
				Assertions.assertNull(f.readLine()); // 真eof仍为null
				Assertions.assertNull(f.readLine()); // eof稳定
			}
		} finally {
			Files.deleteIfExists(file);
		}
	}

	@Test
	public void testEofAndPositionIntact() throws Exception {
		var file = Path.of("TestFnd807BufferedRandomFileReadLine0xff.tmp2");
		try {
			Files.writeString(file, "first\nsecond\r\n", StandardCharsets.UTF_8);
			try (var f = new BufferedRandomFile(file.toFile(), StandardCharsets.UTF_8)) {
				Assertions.assertEquals("first", f.readLine());
				Assertions.assertEquals("second", f.readLine());
				Assertions.assertNull(f.readLine());
				Assertions.assertEquals(file.toFile().length(), f.getPosition());
			}
		} finally {
			Files.deleteIfExists(file);
		}
	}
}
