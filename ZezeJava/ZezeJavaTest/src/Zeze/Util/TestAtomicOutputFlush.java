package Zeze.Util;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestAtomicOutputFlush {
	@Test
	public void flushDrainsBufferWithoutPublishing(@TempDir Path dir) throws Exception {
		var target = dir.resolve("state.bin");
		Files.writeString(target, "OLD");
		var out = new AtomicOutputFile(target);
		try {
			var stream = out.outputStream();
			var bytes = new byte[]{1, 2, 3};
			stream.write(bytes);
			stream.flush();
			try (var files = Files.list(dir)) {
				var temp = files.filter(file -> file.toString().endsWith(".tmp")).findFirst().orElseThrow();
				assertArrayEquals(bytes, Files.readAllBytes(temp));
			}
			assertEquals("OLD", Files.readString(target));
		} finally {
			out.abort();
		}
	}

	@Test
	public void flushFailureBlocksCommit(@TempDir Path dir) throws Exception {
		var target = dir.resolve("state.bin");
		Files.writeString(target, "OLD");
		var out = new AtomicOutputFile(target);
		try {
			var stream = out.outputStream();
			stream.write(new byte[]{1, 2, 3});
			var field = AtomicOutputFile.class.getDeclaredField("channel");
			field.setAccessible(true);
			((FileChannel)field.get(out)).close();
			assertThrows(IOException.class, stream::flush);
			assertThrows(IOException.class, out::commit);
			assertEquals("OLD", Files.readString(target));
			try (var files = Files.list(dir)) {
				assertEquals(1, files.count(), "失败提交应立即清理temp");
			}
		} finally {
			out.abort();
		}
	}
}
