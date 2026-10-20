package Benchmark;

import java.nio.file.Files;
import java.nio.file.Path;
import harness.Fast;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

@Fast
@NullMarked
public class TestBenchRocksDbCleanup {
	@TempDir
	Path temp;

	@Test
	void renamesAndDeletesOnlyTheBenchmarkDirectory() throws Exception {
		var cache = Files.createDirectory(temp.resolve("benchmark"));
		Files.writeString(cache.resolve("data"), "cache");
		var existing = Files.createDirectory(temp.resolve("benchmark.del"));
		var keep = Files.writeString(existing.resolve("keep"), "keep");
		BenchRocksDb.renameAndDeleteDirectory(cache.toFile());
		assertFalse(Files.exists(cache));
		assertEquals("keep", Files.readString(keep));
		try (var children = Files.list(temp)) {
			assertEquals(1, children.count());
		}
	}

	@Test
	void missingDirectoryIsAlreadyClean() throws Exception {
		BenchRocksDb.renameAndDeleteDirectory(temp.resolve("missing").toFile());
	}
}
