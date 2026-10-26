package Zeze.Util;

import java.io.IOException;
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
 * ①流自身防线：write/flush曾抛出即记writeFailed，commit遇此标记走失败清理
 *   （删temp、抛出）不再发布——覆盖"流写失败但调用方吞掉异常"；
 * ②作用域防线：writeAtomically以writer正常返回为发布条件，任何Throwable丢弃temp
 *   ——覆盖"主体异常但流写入全部成功"（如zip条目复制抛出、底层流无失败记录）。
 * 包装流close只有刷新权限；句柄close默认放弃，发布必须显式commit。
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
	 * 缓冲触达channel抛出；调用方吞掉写失败后仍不得commit——旧目标保持、
	 * temp清理、commit显式抛出writeFailed错误。
	 */
	@Test
	public void testWriteFailureBlocksCommit() throws Exception {
		var target = dir.resolve("defense.bin");
		Files.writeString(target, "GOOD-OLD");

		var channelField = AtomicOutputFile.class.getDeclaredField("channel");
		channelField.setAccessible(true);
		try (var out = new AtomicOutputFile(target)) {
			var stream = out.outputStream();
			stream.write("NE".getBytes(java.nio.charset.StandardCharsets.UTF_8)); // 前缀成功（进缓冲）
			// 真实注入：关掉底层channel，大块write越过缓冲区必然IOException。
			((java.nio.channels.FileChannel)channelField.get(out)).close();
			Assertions.assertThrows(ClosedChannelException.class,
					() -> stream.write(new byte[64 * 1024]), "底层channel已关，write必须失败");
			var commitFailure = Assertions.assertThrows(IOException.class, out::commit);
			Assertions.assertTrue(commitFailure.getMessage().contains("write failed"),
					"commit必须因先前写失败而拒绝发布");
			Assertions.assertThrows(IOException.class, out::commit, "失败提交后不得再次提交");
			assertNoTmpResidue();
		}
		Assertions.assertEquals("GOOD-OLD", Files.readString(target), "写失败后commit不得发布残缺前缀");
		assertNoTmpResidue();
	}

	/** 防线②：writer正常返回——照常换版发布。 */
	@Test
	public void testScopedWriteSuccessReplaces() throws Exception {
		var target = dir.resolve("scoped.bin");
		Files.writeString(target, "OLD");
		AtomicOutputFile.writeAtomically(target, out -> out.write("NEW".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		Assertions.assertEquals("NEW", Files.readString(target));
		assertNoTmpResidue();
	}

	/** 防线②：writer写了一半抛RuntimeException——旧目标保持、temp清理、异常原样上抛。 */
	@Test
	public void testScopedWriterThrowsKeepsOld() throws Exception {
		var target = dir.resolve("scoped_throw.bin");
		Files.writeString(target, "GOOD-OLD");
		Assertions.assertThrows(IllegalStateException.class, () ->
				AtomicOutputFile.writeAtomically(target, out -> {
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
				AtomicOutputFile.writeAtomically(target, out -> {
					out.write("PREFIX".getBytes(java.nio.charset.StandardCharsets.UTF_8));
					throw new IOException("simulated read-source failure");
				}));
		Assertions.assertEquals("GOOD-OLD", Files.readString(target));
		assertNoTmpResidue();
	}

	private void assertNoTmpResidue() throws IOException {
		Assertions.assertTrue(listTmpFiles().isEmpty(), "不得残留tmp: " + listTmpFiles());
	}

	@Test
	public void testClosedWrapperCannotPublishFailedCallback() throws Exception {
		var target = dir.resolve("scoped_wrapper.bin");
		Files.writeString(target, "GOOD-OLD");
		var failure = new IOException("source failed inside zip");
		var thrown = Assertions.assertThrows(IOException.class, () ->
				AtomicOutputFile.writeAtomically(target, out -> {
					try (var zip = new java.util.zip.ZipOutputStream(out)) {
						zip.putNextEntry(new java.util.zip.ZipEntry("entry"));
						zip.write(new byte[]{1, 2, 3});
						throw failure;
					}
				}));
		Assertions.assertSame(failure, thrown);
		Assertions.assertArrayEquals("GOOD-OLD".getBytes(java.nio.charset.StandardCharsets.UTF_8),
				Files.readAllBytes(target));
		assertNoTmpResidue();
	}

	@Test
	public void testCallbackCloseDoesNotPublishBeforeReturn() throws Exception {
		var target = dir.resolve("scoped_close.bin");
		Files.writeString(target, "OLD");
		AtomicOutputFile.writeAtomically(target, out -> {
			try (var writer = new java.io.OutputStreamWriter(out, java.nio.charset.StandardCharsets.UTF_8)) {
				writer.write("NEW");
			}
			Assertions.assertEquals("OLD", Files.readString(target), "回调正常结束前不得发布");
		});
		Assertions.assertEquals("NEW", Files.readString(target));
		assertNoTmpResidue();
	}

	@Test
	public void testWrapperCloseFailureKeepsOld() throws Exception {
		var target = dir.resolve("scoped_close_failure.bin");
		Files.writeString(target, "OLD");
		var failure = new IOException("zip close failed");
		var thrown = Assertions.assertThrows(IOException.class, () ->
				AtomicOutputFile.writeAtomically(target, out -> {
					try (var zip = new java.util.zip.ZipOutputStream(out) {
						@Override
						public void close() throws IOException {
							super.close();
							throw failure;
						}
					}) {
						zip.putNextEntry(new java.util.zip.ZipEntry("entry"));
						zip.write(new byte[]{1, 2, 3});
					}
				}));
		Assertions.assertSame(failure, thrown);
		Assertions.assertEquals("OLD", Files.readString(target));
		assertNoTmpResidue();
	}

	@Test
	public void testScopedWriterErrorKeepsOld() throws Exception {
		var target = dir.resolve("scoped_error.bin");
		Files.writeString(target, "OLD");
		var failure = new AssertionError("writer failed");
		var thrown = Assertions.assertThrows(AssertionError.class, () ->
				AtomicOutputFile.writeAtomically(target, out -> {
					out.write(new byte[]{1, 2, 3});
					throw failure;
				}));
		Assertions.assertSame(failure, thrown);
		Assertions.assertEquals("OLD", Files.readString(target));
		assertNoTmpResidue();
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
