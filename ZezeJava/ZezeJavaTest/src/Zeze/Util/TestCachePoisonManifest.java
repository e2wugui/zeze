package Zeze.Util;

import static java.util.Objects.requireNonNull;

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
public class TestCachePoisonManifest {

	@Test
	public void testPoisonManifestSkippedNotFatal(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var dir = tempDir.resolve("poison-cache").toString();
		var cache = newCache(dir, "v1");
		Assertions.assertNotNull(cache.get("key1")); // 登记当天清单并落 RocksDb（v1）

		var nowDays = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
		// 毒文件：days_ 前缀 + 非数字后缀（触发 parseLong 抛 NFE）。
		// 命名让它字典序排在数字清单之前（'!' < '0'），最大化暴露修复前的循环中断。
		Files.writeString(tempDir.resolve("poison-cache").resolve("days_!.tmp"), "garbage\n");
		// 合法过期清单（40天前）：列出 key1，必须被退役（文件删除、db 记录删除）。
		var oldDays = nowDays - 40;
		Files.writeString(tempDir.resolve("poison-cache").resolve("days_" + oldDays), "key1\n");
		// 清理筛选跳过"当前使用中"（Lru 命中）的条目，先摘除让 key1 可退役。
		lruOf(cache).remove("key1");

		// 修复前：对 days_!.tmp 的 parseLong 抛 NFE，以 InvocationTargetException 逃出，
		// 其后的合法清单永远不被处理。
		Assertions.assertDoesNotThrow(() -> invokeTryRemove(cache),
				"畸形清单文件必须被跳过，不得中断整个日清任务（FND7-37）");

		// 合法过期清单被正常退役：文件删除。
		Assertions.assertFalse(Files.exists(tempDir.resolve("poison-cache").resolve("days_" + oldDays)),
				"毒文件存在时，过期的合法清单仍必须被退役");
		// 毒文件保留（跳过处理但不删除），当天清单完好。
		Assertions.assertTrue(Files.exists(tempDir.resolve("poison-cache").resolve("days_!.tmp")),
				"畸形文件跳过处理但不删除（留给运维）");
		cache.close();

		// 退役后 key 从 RocksDb 消失：新实例（loader v2）get 未命中 db、重走 loader 装载 v2。
		var cache2 = newCache(dir, "v2");
		var obj = (Zeze.Util.TestCacheAppendTodayResume.Obj)cache2.get("key1");
		Assertions.assertEquals("v2", requireNonNull(obj).value, "退役清单中的 key 必须已从 RocksDb 删除，重行走 loader");
		cache2.close();
	}

	@Test
	public void testCorruptedBase64ManifestSkippedAndOtherManifestsRetired(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var dir = tempDir.resolve("corrupted-b64-cache").toString();
		var cache = newCache(dir, "v1");
		try {
			Assertions.assertNotNull(cache.get("key1"));
			var nowDays = System.currentTimeMillis() / (24 * 60 * 60 * 1000);
			var badDays = nowDays - 50;
			var goodDays = nowDays - 40;
			// 损坏的 .b64 清单（非法 Base64 行），文件名按字典序排在合法清单之前
			Files.writeString(tempDir.resolve("corrupted-b64-cache").resolve("days_" + badDays + ".b64"),
				"!!!not-valid-base64!!!\n");
			Files.writeString(tempDir.resolve("corrupted-b64-cache").resolve("days_" + goodDays), "key1\n");
			lruOf(cache).remove("key1");

			Assertions.assertDoesNotThrow(() -> invokeTryRemove(cache),
				"单个损坏 .b64 清单必须跳过，不得阻塞后续合法清单退役");
			Assertions.assertFalse(Files.exists(tempDir.resolve("corrupted-b64-cache").resolve("days_" + goodDays)),
				"合法过期清单必须被正常退役删除");
			Assertions.assertTrue(Files.exists(tempDir.resolve("corrupted-b64-cache").resolve("days_" + badDays + ".b64")),
				"损坏清单保留不删");
		} finally {
			cache.close();
		}

		var cache2 = newCache(dir, "v2");
		try {
			var obj = (Zeze.Util.TestCacheAppendTodayResume.Obj)cache2.get("key1");
			Assertions.assertEquals("v2", requireNonNull(obj).value, "合法清单退役后 key 必须从 RocksDb 删除");
		} finally {
			cache2.close();
		}
	}

	@Test
	public void testTryRemoveDoesNotPromoteColdLruEntryToHot(@TempDir Path tempDir) throws Exception {
		Task.tryInitThreadPool();
		var dir = tempDir.resolve("cold-lru-cache").toString();
		var cache = newCache(dir, "v1");
		try {
			Assertions.assertNotNull(cache.get("coldKey"));
			var lru = lruOf(cache);
			// 手动轮转热点使 coldKey 进入非热点老节点
			var newLruHot = ConcurrentLruLike.class.getDeclaredMethod("newLruHot");
			newLruHot.setAccessible(true);
			newLruHot.invoke(lru);
			var hotBefore = TestConcurrentLruLike.lruHotOf(lru);
			Assertions.assertFalse(hotBefore.containsKey("coldKey"), "轮转后 coldKey 应不在当前热点");

			var oldDays = System.currentTimeMillis() / (24 * 60 * 60 * 1000) - 40;
			Files.writeString(tempDir.resolve("cold-lru-cache").resolve("days_" + oldDays), "coldKey\n");

			invokeTryRemove(cache);

			var hotAfter = TestConcurrentLruLike.lruHotOf(lru);
			Assertions.assertFalse(hotAfter.containsKey("coldKey"),
				"后台退役检查必须只读探查 LRU，不得把已冷却条目重新拉回 lruHot");
			Assertions.assertNotNull(lru.get("coldKey", false), "内存中仍在用的条目不应被删除");
		} finally {
			cache.close();
		}
	}

	/** 反射触发 private tryRemove()（等价 6:30 调度线程执行体）。 */
	private static void invokeTryRemove(Cache cache) throws Exception {
		var m = Cache.class.getDeclaredMethod("tryRemove");
		m.setAccessible(true);
		m.invoke(cache);
	}

	private static Cache newCache(String dir, String version) throws Exception {
		return new Cache(dir, 16, id -> new Zeze.Util.TestCacheAppendTodayResume.Obj(id, version),
				(id, bb) -> {
					var o = new Zeze.Util.TestCacheAppendTodayResume.Obj();
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
