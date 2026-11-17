package Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import Zeze.Net.Binary;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestFileBinTruncateDigest {
	@TempDir
	Path directory;

	@Test
	public void truncateThenAppendHashesActualContent() throws Exception {
		var bin = new FileBin("payload", directory.toFile(), "payload");
		try {
			bin.append(0, new Binary("abcdef".getBytes(StandardCharsets.UTF_8)));
			bin.truncate(2);
			bin.append(2, new Binary("XY".getBytes(StandardCharsets.UTF_8)));
		} finally {
			bin.close();
		}
		assertArrayEquals("abXY".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(directory.resolve("payload")));
		assertArrayEquals(MessageDigest.getInstance("MD5").digest(Files.readAllBytes(directory.resolve("payload"))),
				bin.md5Digest());
	}

	@Test
	public void truncateAloneHashesRetainedPrefix() throws Exception {
		var bin = new FileBin("prefix", directory.toFile(), "prefix");
		try {
			bin.append(0, new Binary("abcdef".getBytes(StandardCharsets.UTF_8)));
			bin.truncate(3);
		} finally {
			bin.close();
		}
		assertEquals(3, Files.size(directory.resolve("prefix")));
		assertArrayEquals(MessageDigest.getInstance("MD5").digest("abc".getBytes(StandardCharsets.UTF_8)), bin.md5Digest());
	}
}
