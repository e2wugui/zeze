package Zeze.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestCacheManifestWriteFailure {
	private record Value(String cacheId) implements CacheObject {
		@Override
		public void encode(ByteBuffer bb) {
		}

		@Override
		public void decode(IByteBuffer bb) {
		}
	}

	@Test
	public void retryReloadsValueWhenRetirementRegistrationFailed(@TempDir Path dir) throws Exception {
		Task.tryInitThreadPool();
		var loads = new AtomicInteger();
		var cache = new Cache(dir.toString(), 16, id -> {
			loads.incrementAndGet();
			return new Value(id);
		}, (id, bb) -> new Value(id));
		try {
			long today = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis());
			var manifest = Files.createDirectory(dir.resolve("days_" + today + ".b64"));
			var nextManifest = Files.createDirectory(dir.resolve("days_" + (today + 1) + ".b64"));
			assertThrows(IOException.class, () -> cache.get("key"));
			Files.delete(manifest);
			Files.delete(nextManifest);

			assertEquals(new Value("key"), cache.get("key"));
			assertEquals(2, loads.get());
		} finally {
			cache.close();
		}
	}
}
