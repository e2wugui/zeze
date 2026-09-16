package UnitTest.Zeze.Util;

import java.nio.file.Files;
import java.nio.file.Path;

import Zeze.Util.Cache;
import Zeze.Util.ConcurrentLruLike;
import Zeze.Util.Task;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND7-37 回归：日清单清理对 days_ 前缀文件直接 Long.parseLong(后缀)，遇非数字后缀
 * （days_.tmp、days_1.bak 等运维/崩溃残留）抛 NumberFormatException 穿透整个清理循环：
 * 毒文件位于 delete 之前永不会被删，次日同一文件同一异常复发，排在它后面的合法 days_
 * 清单从当天起永远不被退役（与"半途抛异常不会永久丢失清理"的注释意图相反，循环级中断是永久的）。
 * 修复：单文件解析包 try/catch，跳过畸形文件继续处理其余清单。
 * tryRemove 是 private 且每日 6:30 调度，测试经反射直接触发（等价调度线程执行体）。
 */
@Fast
public class TestFnd737CachePoisonManifest {

	@Test
	public void testPoisonManifestSkippedNotFatal(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var dir = tempDir.resolve("fnd737-cache").toString();
		var cache = newCache(dir, "v1");
		Assertions.assertNotNull(cache.get("key1")); // 登记当天清单并落 RocksDb（v1）

		var nowDays = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
		// 毒文件：days_ 前缀 + 非数字后缀（触发 parseLong 抛 NFE）。
		// 命名让它字典序排在数字清单之前（'!' < '0'），最大化暴露修复前的循环中断。
		Files.writeString(tempDir.resolve("fnd737-cache").resolve("days_!.tmp"), "garbage\n");
		// 合法过期清单（40天前）：列出 key1，必须被退役（文件删除、db 记录删除）。
		var oldDays = nowDays - 40;
		Files.writeString(tempDir.resolve("fnd737-cache").resolve("days_" + oldDays), "key1\n");
		// 清理筛选跳过"当前使用中"（Lru 命中）的条目，先摘除让 key1 可退役。
		lruOf(cache).remove("key1");

		// 修复前：对 days_!.tmp 的 parseLong 抛 NFE，以 InvocationTargetException 逃出，
		// 其后的合法清单永远不被处理。
		Assertions.assertDoesNotThrow(() -> invokeTryRemove(cache),
				"畸形清单文件必须被跳过，不得中断整个日清任务（FND7-37）");

		// 合法过期清单被正常退役：文件删除。
		Assertions.assertFalse(Files.exists(tempDir.resolve("fnd737-cache").resolve("days_" + oldDays)),
				"毒文件存在时，过期的合法清单仍必须被退役");
		// 毒文件保留（跳过处理但不删除），当天清单完好。
		Assertions.assertTrue(Files.exists(tempDir.resolve("fnd737-cache").resolve("days_!.tmp")),
				"畸形文件跳过处理但不删除（留给运维）");
		cache.close();

		// 退役后 key 从 RocksDb 消失：新实例（loader v2）get 未命中 db、重走 loader 装载 v2。
		var cache2 = newCache(dir, "v2");
		var obj = (UnitTest.Zeze.Util.TestCacheAppendTodayResume.Obj)cache2.get("key1");
		Assertions.assertEquals("v2", obj.value, "退役清单中的 key 必须已从 RocksDb 删除，重行走 loader");
		cache2.close();
	}

	/** 反射触发 private tryRemove()（等价 6:30 调度线程执行体）。 */
	private static void invokeTryRemove(Cache cache) throws Exception {
		var m = Cache.class.getDeclaredMethod("tryRemove");
		m.setAccessible(true);
		m.invoke(cache);
	}

	private static Cache newCache(String dir, String version) throws Exception {
		return new Cache(dir, 16, id -> new UnitTest.Zeze.Util.TestCacheAppendTodayResume.Obj(id, version),
				(id, bb) -> {
					var o = new UnitTest.Zeze.Util.TestCacheAppendTodayResume.Obj();
					o.decode(bb);
					return o;
				});
	}

	private static ConcurrentLruLike<String, ?> lruOf(Cache cache) throws Exception {
		var f = Cache.class.getDeclaredField("lru");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		var lru = (ConcurrentLruLike<String, ?>)f.get(cache);
		return lru;
	}
}
