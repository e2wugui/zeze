package Zeze.Util;

import harness.Fast;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import Zeze.Util.Cache;
import Zeze.Util.CacheObject;
import Zeze.Util.ConcurrentLruLike;
import Zeze.Util.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND15 util-01 契约收口：getOrAdd要求Factory&lt;@NotNull V&gt;并运行时强制（requireNonNull
 * fail-fast且违约条目不登记），decoder产null在Cache层哨兵化（NullCache负缓存，与loader路径
 * 对称）——@NotNull从谎言变成自我强制的承诺，调用方null自卫不再需要，IDEA always-false
 * 推断与运行时行为永久一致（FND14→FND15的"删防卫"循环从根上闭合）。
 */
@Fast
public class TestGetOrAddContract {
	static {
		Task.tryInitThreadPool();
	}

	// 违约factory：requireNonNull立即fail-fast，且违约条目不得登记进LRU。
	// 故意的契约违例（钉运行时强制）——勿被IDEA告警清理"修正"成非null，那会拆掉本钉板。
	@SuppressWarnings("NullableProblems")
	@Test
	public void testNullFactoryFailsFastNoRegistration() {
		var lru = new ConcurrentLruLike<String, String>("TestLru.Fnd15Util01Contract", 4);
		try {
			Assertions.assertThrows(NullPointerException.class, () -> lru.getOrAdd("k", () -> null),
					"null产出factory必须立即fail-fast");
			Assertions.assertNull(lru.get("k"), "违约条目不得登记进LRU");
		} finally {
			lru.close();
		}
	}

	// Cache端到端：db命中+decoder产null——get两次均null，驻留条目必须是NullCache哨兵
	// 而非字面null（字面null无负缓存窗口语义、且绕过LRU快路径每次重读RocksDB）。
	@Test
	public void testCacheDecoderNullResidentSentinel() throws Exception {
		Task.tryInitThreadPool();

		var name = "TestCache.Fnd15Util01";
		// 先以有效loader播种RocksDB，重开后同一id走db命中+decoder路径
		var seed = new Cache(name, 10, TestGetOrAddContract::newStub, (id, bb) -> null);
		try {
			Assertions.assertNotNull(seed.get("existId"), "播种有效数据");
		} finally {
			seed.close(); // 释放RocksDB目录锁，同一目录可重开
		}

		var decodeCount = new int[1];
		var cache = new Cache(name, 10, id -> null, (id, bb) -> {
			decodeCount[0]++;
			return null;
		});
		try {
			Assertions.assertNull(cache.get("existId"), "decoder产null必须返回null");
			Assertions.assertNull(cache.get("existId"), "窗口内重复get同样返回null");
			Assertions.assertEquals(1, decodeCount[0], "驻留占位短路，decoder不得重复执行");

			var lruField = Cache.class.getDeclaredField("lru");
			lruField.setAccessible(true);
			@SuppressWarnings("unchecked")
			var lru = (ConcurrentLruLike<String, CacheObject>)lruField.get(cache);
			Assertions.assertTrue(lru.get("existId") instanceof CacheObject.NullCache,
					"decoder产null必须驻留NullCache哨兵而非字面null");
		} finally {
			cache.close();
			deleteDir(name);
		}
	}

	private static void deleteDir(String name) {
		try (var walk = Files.walk(Paths.get(name))) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (Exception ignore) {
				}
			});
		} catch (Exception ignore) {
		}
	}

	private static CacheObjectStub newStub(String id) {
		return new CacheObjectStub(id);
	}

	/** 最小CacheObject实现：cacheId即id，encode/decode空实现（本用例只断言引用与cacheId）。 */
	private static final class CacheObjectStub implements CacheObject {
		private final String id;

		CacheObjectStub(String id) {
			this.id = id;
		}

		@Override
		public String cacheId() {
			return id;
		}

		@Override
		public void encode(Zeze.Serialize.ByteBuffer bb) {
		}

		@Override
		public void decode(Zeze.Serialize.IByteBuffer bb) {
		}
	}
}
