package Zeze.Raft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import Zeze.Raft.RocksRaft.Rocks;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestSnapshotExtractionPaths {
	private static Path archive(Path root, String entry) throws Exception {
		var zip = root.resolve("snapshot.zip");
		try (var output = new ZipOutputStream(Files.newOutputStream(zip))) {
			output.putNextEntry(new ZipEntry(entry));
			output.write(new byte[]{1, 2, 3});
			output.closeEntry();
		}
		return zip;
	}

	@Test
	void currentDirectorySegmentDoesNotRejectValidSnapshot(@TempDir Path root) throws Exception {
		var zip = archive(root, "meta/CURRENT");
		var target = root.resolve(".").resolve("backup");
		Rocks.extractZipToDirectory(zip.toString(), target.toString());
		assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(root.resolve("backup/meta/CURRENT")));
	}

	@Test
	void parentDirectorySegmentDoesNotRejectValidSnapshot(@TempDir Path root) throws Exception {
		var zip = archive(root, "meta/CURRENT");
		Files.createDirectory(root.resolve("staging"));
		var target = root.resolve("staging").resolve("..").resolve("backup");
		Rocks.extractZipToDirectory(zip.toString(), target.toString());
		assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(root.resolve("backup/meta/CURRENT")));
	}

	@Test
	void archiveEntryCannotEscapeNormalizedDestination(@TempDir Path root) throws Exception {
		var zip = archive(root, "../escaped");
		var target = root.resolve(".").resolve("backup");
		assertThrows(IllegalStateException.class,
				() -> Rocks.extractZipToDirectory(zip.toString(), target.toString()));
		assertFalse(Files.exists(root.resolve("escaped")));
	}
}
