package Zeze;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystemException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.nio.file.ExtendedOpenOption;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestLocalCacheDirectoryDeletion {
	@TempDir
	Path temp;

	@Test
	void missingDirectoryIsAlreadyDeleted() throws Exception {
		LocalCacheResources.deleteDirectory(temp.resolve("missing").toFile());
	}

	@Test
	void deletesNestedFilesAndDirectories() throws Exception {
		var root = Files.createDirectories(temp.resolve("cache/sub/child"));
		Files.writeString(root.resolve("data"), "cache");
		Files.writeString(root.getParent().resolve("other"), "cache");
		LocalCacheResources.deleteDirectory(temp.resolve("cache").toFile());
		assertFalse(Files.exists(temp.resolve("cache")));
	}

	@Test
	void deletesLinksWithoutDeletingTheirTargets() throws Exception {
		var cache = Files.createDirectory(temp.resolve("cache"));
		var outside = Files.createDirectory(temp.resolve("outside"));
		var marker = Files.writeString(outside.resolve("keep"), "keep");
		try {
			Files.createSymbolicLink(cache.resolve("link"), outside);
			Files.createSymbolicLink(cache.resolve("broken"), temp.resolve("missing"));
		} catch (IOException | UnsupportedOperationException e) {
			Assumptions.abort("symbolic links unavailable: " + e);
		}
		LocalCacheResources.deleteDirectory(cache.toFile());
		assertFalse(Files.exists(cache));
		assertEquals("keep", Files.readString(marker));
	}

	@Test
	void deletesAFileUsedAsTheRoot() throws Exception {
		var file = Files.writeString(temp.resolve("file"), "keep");
		LocalCacheResources.deleteDirectory(file.toFile());
		assertFalse(Files.exists(file));
	}

	private static SeekableByteChannel preventDeletion(Path file) throws IOException {
		return Files.newByteChannel(file, StandardOpenOption.READ, ExtendedOpenOption.NOSHARE_DELETE);
	}

	private static void awaitRetry(Thread worker) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (worker.isAlive() && worker.getState() != Thread.State.TIMED_WAITING
				&& System.nanoTime() < deadline)
			Thread.sleep(1);
		assertEquals(Thread.State.TIMED_WAITING, worker.getState(), "delete should be waiting to retry");
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void retriesAnOpenFileUntilItsHandleIsClosed() throws Exception {
		var cache = Files.createDirectory(temp.resolve("cache"));
		var file = Files.writeString(cache.resolve("locked"), "cache");
		var failure = new AtomicReference<@Nullable Throwable>();
		var worker = Thread.ofPlatform().daemon().unstarted(() -> {
			try {
				LocalCacheResources.deleteDirectory(cache.toFile());
			} catch (Throwable e) {
				failure.set(e);
			}
		});
		try (var handle = preventDeletion(file)) {
			assertThrows(FileSystemException.class, () -> Files.delete(file));
			worker.start();
			awaitRetry(worker);
		} finally {
			worker.join(5_000);
			if (worker.isAlive())
				worker.interrupt();
		}
		assertFalse(worker.isAlive());
		assertNull(failure.get());
		assertFalse(Files.exists(cache));
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void interruptStopsRetryingAndLeavesTheLockedFile() throws Exception {
		var file = Files.writeString(temp.resolve("locked"), "keep");
		var failure = new AtomicReference<@Nullable Throwable>();
		var worker = Thread.ofPlatform().daemon().unstarted(() -> {
			try {
				LocalCacheResources.deleteDirectory(file.toFile());
			} catch (Throwable e) {
				failure.set(e);
			}
		});
		try (var handle = preventDeletion(file)) {
			worker.start();
			try {
				awaitRetry(worker);
			} finally {
				worker.interrupt();
				worker.join(5_000);
			}
			assertFalse(worker.isAlive());
			assertInstanceOf(InterruptedException.class, failure.get());
			assertTrue(Files.exists(file));
		}
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void readOnlyFileFailsWithoutSleepingOrConsumingTheInterruptFlag() throws Exception {
		var file = Files.writeString(temp.resolve("readonly"), "keep");
		Files.setAttribute(file, "dos:readonly", true);
		Thread.currentThread().interrupt();
		try {
			assertThrows(AccessDeniedException.class, () -> LocalCacheResources.deleteDirectory(file.toFile()));
			assertTrue(Thread.currentThread().isInterrupted(), "permanent read-only failure must not enter sleep");
		} finally {
			Thread.interrupted();
			Files.setAttribute(file, "dos:readonly", false);
		}
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	@Timeout(20)
	void aPermanentlyOpenFileEventuallyReportsItsFilesystemError() throws Exception {
		var file = Files.writeString(temp.resolve("locked"), "keep");
		try (var handle = preventDeletion(file)) {
			var error = assertThrows(FileSystemException.class,
					() -> LocalCacheResources.deleteDirectory(file.toFile()));
			assertEquals(file.toString(), error.getFile());
			assertTrue(Files.exists(file));
		}
	}
}
