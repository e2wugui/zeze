package Zeze.Util;

import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestBufferedRandomFileClosedReads {
	@Test
	public void closedReaderRejectsReadsEvenWithBufferedData(@TempDir Path directory) throws Exception {
		var source = Files.writeString(directory.resolve("lines.txt"), "first\nsecond\n");
		try (var file = new BufferedRandomFile(source.toFile(), StandardCharsets.UTF_8)) {
			assertEquals("first", file.readLine());
			file.close();
			assertThrows(ClosedChannelException.class, () -> file.read(new byte[1], 0, 1));
			assertThrows(ClosedChannelException.class, file::readLine);
			assertEquals(6, file.getPosition());
		}
	}
}
