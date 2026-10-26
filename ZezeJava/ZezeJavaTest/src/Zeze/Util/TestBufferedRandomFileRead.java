package Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestBufferedRandomFileRead {
	@Test
	public void invalidSliceDoesNotConsumeFileData(@TempDir Path dir) throws Exception {
		var content = new byte[40_000];
		for (int i = 0; i < content.length; i++)
			content[i] = (byte)(i * 31);
		var path = dir.resolve("input.bin");
		Files.write(path, content);
		try (var file = new BufferedRandomFile(path.toFile(), StandardCharsets.UTF_8)) {
			assertThrows(IndexOutOfBoundsException.class, () -> file.read(new byte[20_000], 0, 20_001));
			assertEquals(0, file.getPosition());
			var actual = new byte[content.length];
			assertEquals(content.length, file.read(actual, 0, actual.length));
			assertArrayEquals(content, actual);
		}
	}

	@Test
	public void emptyReadValidatesBoundsAndKeepsPosition(@TempDir Path dir) throws Exception {
		var path = dir.resolve("empty.bin");
		Files.write(path, new byte[0]);
		try (var file = new BufferedRandomFile(path.toFile(), StandardCharsets.UTF_8)) {
			var bytes = new byte[1];
			assertThrows(IndexOutOfBoundsException.class, () -> file.read(bytes, 0, -1));
			assertThrows(IndexOutOfBoundsException.class, () -> file.read(bytes, 2, 0));
			assertEquals(0, file.read(bytes, 1, 0));
			assertEquals(-1, file.read(bytes, 0, 1));
			assertEquals(0, file.getPosition());
		}
	}
}
