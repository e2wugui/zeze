package Zeze.Raft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import Zeze.Raft.RocksRaft.Rocks;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestSnapshotZipWrite {
	@Test
	void completedZipReplacesOldSnapshot(@TempDir Path root) throws Exception {
		var source = Files.createDirectory(root.resolve("source"));
		var entry = Files.createDirectories(source.resolve("meta")).resolve("CURRENT");
		var bytes = new byte[]{1, 2, 3};
		Files.write(entry, bytes);
		var target = root.resolve("snapshot.zip");
		Files.writeString(target, "OLD");
		Rocks.createZipFromDirectory(source.toString(), target.toString());
		var extracted = root.resolve("extracted");
		Rocks.extractZipToDirectory(target.toString(), extracted.toString());
		assertArrayEquals(bytes, Files.readAllBytes(extracted.resolve("meta/CURRENT")));
		assertNoTemps(root);
	}

	@Test
	void missingSourceKeepsOldSnapshot(@TempDir Path root) throws Exception {
		var target = root.resolve("snapshot.zip");
		Files.writeString(target, "OLD");
		assertThrows(IOException.class, () ->
				Rocks.createZipFromDirectory(root.resolve("missing").toString(), target.toString()));
		assertEquals("OLD", Files.readString(target));
		assertNoTemps(root);
	}

	private static void assertNoTemps(Path root) throws IOException {
		try (var files = Files.list(root)) {
			assertFalse(files.anyMatch(file -> file.toString().endsWith(".tmp")));
		}
	}
}
