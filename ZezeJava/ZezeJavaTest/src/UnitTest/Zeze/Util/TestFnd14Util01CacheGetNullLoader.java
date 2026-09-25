package UnitTest.Zeze.Util;

import harness.Fast;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import Zeze.Util.Cache;
import Zeze.Util.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND14 util-01回归：Cache.get对"数据不存在"返回两种互斥表示——首次miss经getOrAdd把
 * NullCache哨兵实例裸返调用方（cacheId()==""、encode()抛UnsupportedOperationException），
 * 5分钟窗口内重复get却翻译为null。同函数另一分支已确立"NullCache必须翻译为null"，
 * 修复：getOrAdd返回值做isNull翻译（loader与decoder两路径）。
 * 负缓存语义不变式：防穿透靠占位留在LRU，与返回值无关——修复后首次与窗口内重复get同为null。
 */
@Fast
public class TestFnd14Util01CacheGetNullLoader {
	@Test
	public void testFirstMissReturnsNullNotSentinel() throws Exception {
		Task.tryInitThreadPool();

		var name = "TestCache.Fnd14Util01";
		var cache = new Cache(name, 10, id -> null, (id, bb) -> null);
		try {
			// 修复前红：首次get返回NullCache实例（非null），调用方把哨兵当有效CacheObject使用
			var first = cache.get("notExist");
			Assertions.assertNull(first, "首次miss必须返回null，不得裸返NullCache哨兵实例");

			// 窗口内重复get行为不变：仍为null（负缓存占位短路）
			var second = cache.get("notExist");
			Assertions.assertNull(second, "5分钟窗口内重复get必须同样返回null");

			// 正常数据路径不受翻译影响：loader返回有效对象原样返回
			var cache2 = new Cache(name + "2", 10, id -> new CacheObjectStub(id), (id, bb) -> null);
			try {
				var hit = cache2.get("existId");
				Assertions.assertNotNull(hit, "有效loader返回不得被翻译吞掉");
				Assertions.assertEquals("existId", hit.cacheId(), "有效对象原样返回");
			} finally {
				cache2.close();
				deleteDir(name + "2");
			}
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

	/** 最小CacheObject实现：cacheId即id，encode/decode空实现（本用例只断言引用与cacheId）。 */
	private static final class CacheObjectStub implements Zeze.Util.CacheObject {
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
