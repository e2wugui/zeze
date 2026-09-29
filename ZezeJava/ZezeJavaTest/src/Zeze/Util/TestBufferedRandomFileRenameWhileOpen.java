package Zeze.Util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import harness.Fast;

/**
 * FND26 log4j-02 回归：读句柄不得阻断写方的 rename 型轮转。
 * RandomAccessFile 在 Windows 打开文件不带 FILE_SHARE_DELETE——查询/buildIndex 的分钟级
 * 读持有窗口内，写方（log4j2 rename 轮转）对 active 的 rename 恒败（sharing violation），
 * active 无界增长。修复=BufferedRandomFile 改 NIO FileChannel 打开（默认全共享，
 * Windows 含 FILE_SHARE_DELETE，Linux O_RDONLY 语义不变）。
 * 本用例在 Windows 上对修复前形态红（Files.move 抛 FileSystemException）；
 * Linux 上 rename 本就不受读句柄限制，恒绿（平台容忍，不构成假红）。
 */
@Fast
public class TestBufferedRandomFileRenameWhileOpen {
	private static final String Content = "2026-09-29 00:00:00.000 line-for-share-check\n";

	@Test
	public void testRenameWhileOpenSucceeds() throws Exception {
		var file = Path.of("TestBufferedRandomFileRenameWhileOpen.tmp");
		var moved = Path.of("TestBufferedRandomFileRenameWhileOpen.moved");
		Files.deleteIfExists(file);
		Files.deleteIfExists(moved);
		try {
			Files.writeString(file, Content, StandardCharsets.UTF_8);
			try (var f = new BufferedRandomFile(file.toFile(), StandardCharsets.UTF_8)) {
				assertEquals("2026-09-29 00:00:00.000 line-for-share-check", f.readLine(),
						"打开后先读一行，确保句柄真实持有文件");

				// 写方视角的 rename 型轮转：读句柄存活期间必须成功（修复前 Windows 必败）。
				Files.move(file, moved);
				assertTrue(Files.exists(moved), "rename 后目标存在");
				assertTrue(!Files.exists(file), "rename 后源消失");
			}
		} finally {
			Files.deleteIfExists(file);
			Files.deleteIfExists(moved);
		}
	}

	@Test
	public void testReadAfterRenameSeesContent() throws Exception {
		// rename 后已打开句柄仍指向原内容（FILE_SHARE_DELETE 语义：句柄跟踪文件对象而非路径），
		// 读侧不因写方轮转而损坏——这是查询正确性在轮转窗口内的基础前提。
		var file = Path.of("TestBufferedRandomFileRenameWhileOpen2.tmp");
		var moved = Path.of("TestBufferedRandomFileRenameWhileOpen2.moved");
		Files.deleteIfExists(file);
		Files.deleteIfExists(moved);
		try {
			Files.writeString(file, Content, StandardCharsets.UTF_8);
			try (var f = new BufferedRandomFile(file.toFile(), StandardCharsets.UTF_8)) {
				Files.move(file, moved);
				assertEquals(Content.stripTrailing(), f.readLine(), "rename 后原句柄仍可读出原内容");
				assertEquals(-1, f.read(new byte[1], 0, 1), "读尽即 EOF");
			}
		} finally {
			Files.deleteIfExists(file);
			Files.deleteIfExists(moved);
		}
	}

	@Test
	public void testMissingFileKeepsFileNotFoundExceptionContract() throws Exception {
		// FileNotFoundException 契约：调用方（Log4jFileSession 构造失败回收链、manager 装载）
		// 按该类型捕获轮转竞态的条目消失；FileChannel.open 原生抛 NoSuchFileException
		//（非 FileNotFoundException 子类），构造器必须适配回契约类型。
		var absent = Path.of("TestBufferedRandomFileRenameWhileOpen.absent").toAbsolutePath();
		Files.deleteIfExists(absent);
		var caught = new FileNotFoundException[1];
		try {
			new BufferedRandomFile(absent.toFile(), StandardCharsets.UTF_8);
		} catch (FileNotFoundException e) {
			caught[0] = e;
		}
		assertTrue(null != caught[0], "缺失文件必须抛 FileNotFoundException（契约类型，非 NoSuchFileException）");
	}
}
