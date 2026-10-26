package Zeze.Util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestPersistentAtomicLongLargeBatch {
	@Test
	@Timeout(10)
	public void largestBatchReservesPositiveWatermark() throws Exception {
		var dir = Files.createTempDirectory(Path.of("."), "pal-large-batch-");
		var name = dir.resolve("large-batch").toString();
		var fileName = name + ".zeze.pal";
		var pal = PersistentAtomicLong.getOrAdd(name);
		try {
			assertEquals(Integer.MAX_VALUE, pal.next(Integer.MAX_VALUE));
			assertEquals((long)Integer.MAX_VALUE + 1, pal.next());
			var watermark = Long.parseLong(Files.readString(Path.of(fileName)).trim());
			assertTrue(watermark > Integer.MAX_VALUE);
		} finally {
			closePal(name, fileName);
			Files.deleteIfExists(Path.of(fileName));
			Files.deleteIfExists(dir);
		}
	}

	@SuppressWarnings("unchecked")
	private static void closePal(String name, String fileName) throws Exception {
		var filesField = PersistentAtomicLong.class.getDeclaredField("allocFiles");
		filesField.setAccessible(true);
		var files = (ConcurrentHashMap<String, PersistentAtomicLong.FileWithLock>)filesField.get(null);
		var file = files.remove(fileName);
		if (file != null)
			file.close();
		var palsField = PersistentAtomicLong.class.getDeclaredField("pals");
		palsField.setAccessible(true);
		((ConcurrentHashMap<String, PersistentAtomicLong>)palsField.get(null)).remove(name);
	}
}
