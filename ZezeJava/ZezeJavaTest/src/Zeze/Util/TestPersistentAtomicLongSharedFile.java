package Zeze.Util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestPersistentAtomicLongSharedFile {
	@Test
	public void interleavedProcessesReserveDisjointBatches() throws Exception {
		var dir = Files.createTempDirectory(Path.of("."), "pal-shared-");
		var name = dir.resolve("counter").toString();
		var pal = PersistentAtomicLong.getOrAdd(name);
		try {
			assertEquals(32, pal.next(32));
			var output = dir.resolve("child-output.txt");
			var child = new ProcessBuilder(ProcessHandle.current().info().command().orElseThrow(),
					"-cp", childClasspath(), Allocator.class.getName(), name)
					.redirectErrorStream(true).redirectOutput(output.toFile()).start();
			try {
				assertTrue(child.waitFor(10, TimeUnit.SECONDS), "分配子进程应在期限内退出");
				assertEquals(0, child.exitValue(), () -> readOutput(output));
				var childEnd = Long.parseLong(Files.readString(output).trim());
				assertTrue(childEnd >= 64);
				var nextEnd = pal.next(32);
				assertTrue(nextEnd - 32 >= childEnd,
						"本进程新号段必须从其他进程的已保留水位之后开始: " + nextEnd);
			} finally {
				child.destroyForcibly();
				assertTrue(child.waitFor(10, TimeUnit.SECONDS));
				Files.deleteIfExists(output);
			}
		} finally {
			closePal(name);
			Files.deleteIfExists(Path.of(name + ".zeze.pal"));
			Files.deleteIfExists(dir);
		}
	}

	@Test
	public void concurrentBatchRangesNeverOverlap() throws Exception {
		var dir = Files.createTempDirectory(Path.of("."), "pal-batches-");
		var name = dir.resolve("counter").toString();
		var pal = PersistentAtomicLong.getOrAdd(name);
		try {
			var ranges = new ArrayList<long[]>();
			try (var pool = Executors.newFixedThreadPool(4)) {
				var jobs = new ArrayList<java.util.concurrent.Future<ArrayList<long[]>>>();
				for (int worker = 0; worker < 4; worker++) {
					jobs.add(pool.submit(() -> {
						var allocated = new ArrayList<long[]>();
						for (int i = 0; i < 200; i++) {
							var count = i % 3 == 0 ? 257 : 5;
							var end = pal.next(count);
							allocated.add(new long[]{end - count, end});
						}
						return allocated;
					}));
				}
				for (var job : jobs)
					ranges.addAll(job.get(10, TimeUnit.SECONDS));
			}
			ranges.sort(Comparator.comparingLong(range -> range[0]));
			long previousEnd = 0;
			for (var range : ranges) {
				assertTrue(range[0] >= previousEnd, "并发分配的号段不得重叠");
				previousEnd = range[1];
			}
		} finally {
			closePal(name);
			Files.deleteIfExists(Path.of(name + ".zeze.pal"));
			Files.deleteIfExists(dir);
		}
	}

	public static final class Allocator {
		public static void main(String[] args) {
			System.out.println(PersistentAtomicLong.getOrAdd(args[0]).next(32));
		}
	}

	private static String readOutput(Path output) {
		try {
			return Files.readString(output);
		} catch (java.io.IOException e) {
			return e.toString();
		}
	}

	private static String childClasspath() throws Exception {
		var parts = new ArrayList<String>();
		for (var type : new Class<?>[]{Allocator.class, PersistentAtomicLong.class,
				org.apache.logging.log4j.LogManager.class})
			parts.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toString());
		return String.join(File.pathSeparator, parts);
	}

	@SuppressWarnings("unchecked")
	private static void closePal(String name) throws Exception {
		var filesField = PersistentAtomicLong.class.getDeclaredField("allocFiles");
		filesField.setAccessible(true);
		var files = (ConcurrentHashMap<String, PersistentAtomicLong.FileWithLock>)filesField.get(null);
		var file = files.remove(name + ".zeze.pal");
		if (file != null)
			file.close();
		var palsField = PersistentAtomicLong.class.getDeclaredField("pals");
		palsField.setAccessible(true);
		((ConcurrentHashMap<String, PersistentAtomicLong>)palsField.get(null)).remove(name);
	}
}
