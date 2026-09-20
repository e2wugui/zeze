package UnitTest.Zeze.Util;

import Zeze.Util.AtomicFileWriter;
import harness.Fast;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RFD1-03：AtomicFileWriter状态构造测试——old-or-new的两种可观测磁盘状态全用
 * 状态构造覆盖（零生产侵入）；真实进程死亡由TestAtomicFileWriterCrash覆盖。
 */
@Fast
public class TestAtomicFileWriterStates {

	/** close换版生效 / abort保留旧版且幂等不留temp / tmp残留被清扫后旧版完好。 */
	@Test
	public void testCloseReplacesAbortKeepsOldIdempotent() throws Exception {
		var dir = Files.createTempDirectory("atomic-states-");
		var target = dir.resolve("state.bin");
		var oldBytes = "old-complete-version".getBytes();
		var newBytes = "new-complete-version".getBytes();

		AtomicFileWriter.replace(target, oldBytes);
		// 模拟"写途中死亡"的等价磁盘状态：temp残留、目标未动
		Files.write(dir.resolve("state.bin.12345.tmp"), newBytes);
		assertTrue(Files.exists(dir.resolve("state.bin.12345.tmp")));
		assertArrayEquals(oldBytes, Files.readAllBytes(target));
		// tmp是垃圾：直接删除（生产中由调用方启动清扫）
		Files.deleteIfExists(dir.resolve("state.bin.12345.tmp"));
		assertArrayEquals(oldBytes, Files.readAllBytes(target));

		// close = 换版生效
		try (var out = AtomicFileWriter.openOutput(target)) {
			out.write(newBytes);
		}
		assertArrayEquals(newBytes, Files.readAllBytes(target));

		// abort = 换版没有发生；幂等（abort后再abort/try-with-resources的close为no-op）
		try (var out = AtomicFileWriter.openOutput(target)) {
			out.write("discarded".getBytes());
			out.abort();
			out.abort();
		}
		assertArrayEquals(newBytes, Files.readAllBytes(target));
		assertEquals(0, countSuffix(dir, ".tmp"));
	}

	/** 失败路径（非崩溃）：move失败可见、目标原样、temp即清（Windows只读目标必失败，POSIX跳过）。 */
	@Test
	public void testMoveFailureThrowsAndNoTmpResidue() throws Exception {
		Assumptions.assumeTrue(System.getProperty("os.name").toLowerCase().contains("win"),
				"只读目标拒replace是Windows语义");
		var dir = Files.createTempDirectory("atomic-fail-");
		var target = dir.resolve("ro.conf");
		AtomicFileWriter.replace(target, "old".getBytes());
		assertTrue(target.toFile().setReadOnly(), "置目标只读");
		try {
			assertThrows(IOException.class, () -> AtomicFileWriter.replace(target, "new".getBytes()),
					"目标不可替换时必须失败（不得静默）");
			assertArrayEquals("old".getBytes(), Files.readAllBytes(target));
			assertEquals(0, countSuffix(dir, ".tmp"), "失败路径不得残留*.tmp");
		} finally {
			target.toFile().setWritable(true);
		}
	}

	private static int countSuffix(Path dir, String suffix) throws IOException {
		try (var list = Files.list(dir)) {
			return (int) list.filter(p -> p.getFileName().toString().contains(suffix)).count();
		}
	}
}
