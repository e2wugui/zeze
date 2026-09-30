package Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.RocksDB;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestCacheManifestEncoding {
	@Test
	public void newlineIdsRetireWithoutDeletingTheirFragmentsAndLegacyManifestsStillWork(
			@TempDir Path directory) throws Exception {
		Task.tryInitThreadPool();
		RocksDB.loadLibrary();
		String name = directory.resolve("cache").toString();
		var loads = new AtomicInteger();
		var original = new Cache(name, 16, id -> new TestCacheAppendTodayResume.Obj(id, "old"),
				(id, bb) -> new TestCacheAppendTodayResume.Obj(id, bb.ReadString()));
		try {
			for (var id : new String[] {"a\nb", "a", "b", "legacy"})
				assertNotNull(original.get(id));
		} finally {
			original.close();
		}
		long oldDay = System.currentTimeMillis() / 86_400_000 - 31;
		String encoded = Base64.getEncoder().encodeToString("a\nb".getBytes(StandardCharsets.UTF_8));
		Files.writeString(Path.of(name, "days_" + oldDay + ".b64"), encoded + "\n");
		Files.writeString(Path.of(name, "days_" + (oldDay - 1)), "legacy\n");
		var cache = new Cache(name, 16, id -> {
			loads.incrementAndGet();
			return new TestCacheAppendTodayResume.Obj(id, "new");
		}, (id, bb) -> new TestCacheAppendTodayResume.Obj(id, bb.ReadString()));
		try {
			var clean = Cache.class.getDeclaredMethod("tryRemove");
			clean.setAccessible(true);
			clean.invoke(cache);
			assertEquals("old", ((TestCacheAppendTodayResume.Obj)cache.get("a")).value);
			assertEquals("old", ((TestCacheAppendTodayResume.Obj)cache.get("b")).value);
			assertEquals(0, loads.get());
			assertEquals("new", ((TestCacheAppendTodayResume.Obj)cache.get("a\nb")).value);
			assertEquals("new", ((TestCacheAppendTodayResume.Obj)cache.get("legacy")).value);
			assertEquals(2, loads.get());
		} finally {
			cache.close();
		}
	}
}
