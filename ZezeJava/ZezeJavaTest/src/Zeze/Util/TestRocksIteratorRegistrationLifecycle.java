package Zeze.Util;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRocksIteratorRegistrationLifecycle {
	@TempDir
	Path directory;

	@Test
	public void closedScansReleaseRegistration() throws Exception {
		try (var db = new RocksDatabase(directory.resolve("scans").toString())) {
			var table = db.getOrAddTable("scan");
			table.put(new byte[]{1}, new byte[]{2});
			for (int i = 0; i < 100; i++) {
				try (var iterator = table.iterator()) {
					iterator.seekToFirst();
					assertTrue(iterator.isValid());
					assertArrayEquals(new byte[]{1}, iterator.key());
					assertArrayEquals(new byte[]{2}, iterator.value());
				}
			}
			assertEquals(0, registrations(db), "completed scans must not accumulate until database shutdown");
		}
	}

	@Test
	public void databaseCloseWaitsForOpenIterator() throws Exception {
		try (var db = new RocksDatabase(directory.resolve("drain").toString())) {
			var iterator = db.getOrAddTable("drain").iterator();
			var executor = Executors.newSingleThreadExecutor();
			try {
				assertEquals(1, registrations(db));
				var closed = executor.submit(db::close);
				assertThrows(java.util.concurrent.TimeoutException.class, () -> closed.get(100, TimeUnit.MILLISECONDS));
				iterator.close();
				iterator.close(); // 重复关闭不得重复释放 native 句柄。
				closed.get(5, TimeUnit.SECONDS);
				assertEquals(0, registrations(db));
			} finally {
				iterator.close();
				executor.shutdownNow();
			}
		}
	}

	private static int registrations(RocksDatabase db) throws ReflectiveOperationException {
		var field = RocksDatabase.class.getDeclaredField("openIterators");
		field.setAccessible(true);
		return ((Set<?>)field.get(db)).size();
	}
}
