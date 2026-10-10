package Zeze.Util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.List;

import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 原子写失败后不得发布：两道防线各自的裁决范围。
 * <p>
 * close-as-commit原语义下，写入前缀后失败（或调用方主体异常）再走try-with-resources
 * 的close，会把残缺内容原子rename成新版——调用方收到失败异常、旧目标却被换掉，
 * old-or-new契约破坏。两道防线：
 * ①流自身防线：write曾抛IOException即记writeFailed，close遇此标记走失败清理
 *   （删temp、抛出）不再发布——覆盖"流写失败"；
 * ②作用域防线：writeAtomically以writer正常返回为发布条件，任何Throwable丢弃temp
 *   ——覆盖"主体异常但流写入全部成功"（如zip条目复制抛出、底层流无失败记录）。
 * 修复前红：①close照样发布前缀；②writer抛出后TWR的close发布残缺内容。
 */
@Fast
public class TestAtomicScopedWrite {
	private Path dir;

	@BeforeEach
	public void setUp() throws Exception {
		dir = Files.createTempDirectory("atomic_scoped_test");
	}

	@AfterEach
	public void tearDown() throws Exception {
		deleteRecursively(dir);
	}

	/**
	 * 防线①：写入前缀成功后底层channel被关闭（模拟真实写失败），后续大块write越过
	 * 缓冲触达channel抛出；try-with-resources的close不得发布残缺前缀——旧目标保持、
	 * temp清理、close显式抛出writeFailed错误。
	 */
	@Test
	public void testWriteFailureBlocksClosePublish() throws Exception {
		var target = dir.resolve("defense.bin");
		Files.writeString(target, "GOOD-OLD");

		var channelField = AtomicOutputFile.class.getDeclaredField("channel");
		channelField.setAccessible(true);
		var closeFailure = new java.util.concurrent.atomic.AtomicReference<IOException>();
		try (var out = AtomicFileWriter.openOutput(target)) {
			out.write("NE".getBytes(java.nio.charset.StandardCharsets.UTF_8)); // 前缀成功（进缓冲）
			// 真实注入：关掉底层channel，大块write越过缓冲区必然IOException。
			((java.nio.channels.FileChannel)channelField.get(out)).close();
			Assertions.assertThrows(ClosedChannelException.class,
					() -> out.write(new byte[64 * 1024]), "底层channel已关，write必须失败");
		} catch (IOException e) {
			closeFailure.set(e); // TWR退出时的close触发writeFailed防线，必须显式抛出
		}
		Assertions.assertNotNull(closeFailure.get(), "写失败后的close必须显式抛出（不得静默装作成功后发布）");
		Assertions.assertTrue(closeFailure.get().getMessage().contains("write failed"),
				"close失败必须是writeFailed防线（实际: " + closeFailure.get().getMessage() + "）");
		Assertions.assertEquals("GOOD-OLD", Files.readString(target), "写失败后close不得发布残缺前缀，旧目标保持");
		assertNoTmpResidue();
	}

	/** 防线②：writer正常返回——照常换版发布。 */
	@Test
	public void testScopedWriteSuccessReplaces() throws Exception {
		var target = dir.resolve("scoped.bin");
		Files.writeString(target, "OLD");
		AtomicFileWriter.writeAtomically(target, out -> out.write("NEW".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		Assertions.assertEquals("NEW", Files.readString(target));
		assertNoTmpResidue();
	}

	/** 防线②：writer写了一半抛RuntimeException——旧目标保持、temp清理、异常原样上抛。 */
	@Test
	public void testScopedWriterThrowsKeepsOld() throws Exception {
		var target = dir.resolve("scoped_throw.bin");
		Files.writeString(target, "GOOD-OLD");
		Assertions.assertThrows(IllegalStateException.class, () ->
				AtomicFileWriter.writeAtomically(target, out -> {
					out.write("INCOMPLETE".getBytes(java.nio.charset.StandardCharsets.UTF_8));
					throw new IllegalStateException("writer body failed mid-way");
				}));
		Assertions.assertEquals("GOOD-OLD", Files.readString(target), "writer抛出时不得发布半成品，旧目标保持");
		assertNoTmpResidue();
	}

	/** 防线②：writer抛checked IOException——同样丢弃不发布。 */
	@Test
	public void testScopedWriterCheckedThrowKeepsOld() throws Exception {
		var target = dir.resolve("scoped_io_throw.bin");
		Files.writeString(target, "GOOD-OLD");
		Assertions.assertThrows(IOException.class, () ->
				AtomicFileWriter.writeAtomically(target, out -> {
					out.write("PREFIX".getBytes(java.nio.charset.StandardCharsets.UTF_8));
					throw new IOException("simulated read-source failure");
				}));
		Assertions.assertEquals("GOOD-OLD", Files.readString(target));
		assertNoTmpResidue();
	}

	private void assertNoTmpResidue() throws IOException {
		Assertions.assertTrue(listTmpFiles().isEmpty(), "不得残留tmp: " + listTmpFiles());
	}

	private List<Path> listTmpFiles() throws IOException {
		var result = new ArrayList<Path>();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.tmp")) {
			ds.forEach(result::add);
		}
		return result;
	}

	private static void deleteRecursively(Path path) throws IOException {
		if (!Files.exists(path))
			return;
		try (var stream = Files.walk(path)) {
			for (var p : stream.sorted(java.util.Comparator.reverseOrder()).toList())
				Files.deleteIfExists(p);
		}
	}
}
