package UnitTest.Zeze.Util;

import harness.Fast;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import Zeze.Util.Cache;
import Zeze.Util.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
public class TestCache {
	@Test
	public void testTryRemoveAfterClose() throws Exception {
		Task.tryInitThreadPool();

		var name = "TestCache.U4";
		var cache = new Cache(name, 10, id -> null, (id, bb) -> null);
		try {
			cache.close();

			// 手工构造一个30天前的days清单文件（含一个id），触发tryRemove的删除分支
			try (var w = new FileWriter(Paths.get(name, "days_1").toFile())) {
				w.write("someid\n");
			}

			// close后清理任务不得抛NPE：调度句柄应被取消，tryRemove对已关闭状态防御。
			// 修复前：lru已置null，tryRemove(file)里lru.get直接NPE。
			Method m = Cache.class.getDeclaredMethod("tryRemove");
			m.setAccessible(true);
			Assertions.assertDoesNotThrow(() -> m.invoke(cache), "close后tryRemove不得抛NPE");
		} finally {
			// 清理测试目录（cache已close，RocksDB的LOCK已释放可删除；close本身不幂等，不再重复调用）
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
	}
}
