package Zeze.Util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestAtomicOutputLifecycle {
	@Test
	public void writesAfterCommitAreRejected(@TempDir Path dir) throws Exception {
		var target = dir.resolve("commit.bin");
		try (var out = new AtomicOutputFile(target)) {
			var stream = out.outputStream();
			stream.write(1);
			out.commit();
			assertStreamFinished(stream);
			out.commit();
			out.abort();
		}
		assertEquals(1, Files.size(target));
		assertNoTemps(dir);
	}

	@Test
	public void writesAfterAbortAreRejected(@TempDir Path dir) throws Exception {
		var target = dir.resolve("abort.bin");
		Files.writeString(target, "OLD");
		try (var out = new AtomicOutputFile(target)) {
			var stream = out.outputStream();
			stream.write(1);
			out.abort();
			assertStreamFinished(stream);
			assertThrows(IOException.class, out::commit);
			out.abort();
		}
		assertEquals("OLD", Files.readString(target));
		assertNoTemps(dir);
	}

	@Test
	public void closeDiscardsWithoutCommit(@TempDir Path dir) throws Exception {
		for (var existing : new boolean[]{false, true}) {
			var target = dir.resolve(existing ? "existing.bin" : "missing.bin");
			if (existing)
				Files.writeString(target, "OLD");
			try (var file = new AtomicOutputFile(target)) {
				var stream = file.outputStream();
				stream.write("NEW".getBytes(StandardCharsets.UTF_8));
				file.close();
				assertStreamFinished(stream);
				assertThrows(IOException.class, file::commit);
				file.close();
			}
			if (existing)
				assertEquals("OLD", Files.readString(target));
			else
				assertFalse(Files.exists(target));
			assertNoTemps(dir);
		}
	}

	@Test
	public void exceptionalScopeDiscardsWithoutCommit(@TempDir Path dir) throws Exception {
		var target = dir.resolve("exception.bin");
		Files.writeString(target, "OLD");
		var failure = new IOException("source failed");
		var thrown = assertThrows(IOException.class, () -> {
			try (var file = new AtomicOutputFile(target)) {
				file.outputStream().write("NEW".getBytes(StandardCharsets.UTF_8));
				throw failure;
			}
		});
		assertSame(failure, thrown);
		assertEquals("OLD", Files.readString(target));
		assertNoTemps(dir);
	}

	@Test
	public void closedZipWaitsForExplicitCommit(@TempDir Path dir) throws Exception {
		var target = dir.resolve("archive.zip");
		Files.writeString(target, "OLD");
		try (var file = new AtomicOutputFile(target)) {
			try (var zip = new ZipOutputStream(file.outputStream())) {
				zip.putNextEntry(new ZipEntry("entry"));
				zip.write("NEW".getBytes(StandardCharsets.UTF_8));
			}
			assertEquals("OLD", Files.readString(target));
			file.commit();
		}
		try (var zip = new ZipFile(target.toFile());
			 var entry = zip.getInputStream(zip.getEntry("entry"))) {
			assertEquals("NEW", new String(entry.readAllBytes(), StandardCharsets.UTF_8));
		}
		assertNoTemps(dir);
	}

	private static void assertStreamFinished(OutputStream stream) {
		assertThrows(IOException.class, () -> stream.write(2));
		assertThrows(IOException.class, () -> stream.write(new byte[]{3}, 0, 1));
		assertThrows(IOException.class, stream::flush);
	}

	private static void assertNoTemps(Path dir) throws IOException {
		try (var files = Files.list(dir)) {
			assertFalse(files.anyMatch(file -> file.toString().endsWith(".tmp")));
		}
	}
}
