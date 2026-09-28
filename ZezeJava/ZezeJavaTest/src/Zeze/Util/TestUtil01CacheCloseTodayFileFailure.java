package Zeze.Util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import Zeze.Util.Cache;
import Zeze.Util.CacheObject;
import Zeze.Util.Task;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

/**
 * FND13 util-01：Cache.close() 首段关 todayFile 抛 IOException 时无兜底，后续 db.close()/lru.close()
 * 段被整体跳过——RocksDB 句柄与目录锁、Lru 两个常驻周期任务级联滞留（停机路径不可自愈）。
 * 修复后首段关流失败只记日志，释放链必达。
 * <p>
 * 构造性场景：真实 RocksDb 目录装载一次（使 todayFile 建立）→ 反射替换为 close() 必抛的子类流 →
 * close() 不得抛出且 db 字段已置 null（修复前 IOException 直接冒泡、db 残留非 null，本用例红）。
 */
@Fast
public class TestUtil01CacheCloseTodayFileFailure {

	private static final class Value implements CacheObject {
		@Override
		public String cacheId() {
			return "fnd13-util01";
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteString(cacheId());
		}

		@Override
		public void decode(IByteBuffer bb) {
			throw new UnsupportedOperationException();
		}
	}

	@BeforeAll
	public static void setUp() {
		Task.tryInitThreadPool(); // Cache构造注册Lru周期任务依赖调度池
	}

	@Test
	public final void testTodayFileCloseFailureStillReleasesDbAndLru() throws Exception {
		var dir = Files.createTempDirectory("fnd13_util01_cache");
		var cache = new Cache(dir.toString(), 4, id -> new Value(), (id, bb) -> new Value());
		try {
			Assertions.assertNotNull(cache.get("fnd13-util01")); // loader落库+登记当天清单，todayFile建立

			var throwing = new FileOutputStream(Files.createTempFile("fnd13_util01_stream", ".tmp").toFile()) {
				@Override
				public void close() throws IOException {
					throw new IOException("synthetic close failure (FND13 util-01)");
				}
			};
			setTodayFile(cache, throwing);

			// 修复前：IOException 从 close() 冒泡，db/lru 段被整体跳过（本行直接红）。
			Assertions.assertDoesNotThrow(cache::close);
			Assertions.assertNull(dbFieldOf(cache), "db必须已释放置null（首段失败不得中断后续释放段）");
		} finally {
			// 红路径下 db 未被 close() 释放，这里兜底释放，避免 Windows 目录锁残留影响后续用例。
			var db = dbFieldOf(cache);
			if (db instanceof AutoCloseable ac)
				ac.close();
			Files.walk(dir).map(Path::toString).sorted((a, b) -> b.length() - a.length()).forEach(p -> {
				//noinspection ResultOfMethodCallIgnored
				new java.io.File(p).delete();
			});
		}
	}

	private static void setTodayFile(Cache cache, FileOutputStream stream) throws Exception {
		Field field = Cache.class.getDeclaredField("todayFile");
		field.setAccessible(true);
		field.set(cache, stream);
	}

	private static Object dbFieldOf(Cache cache) throws Exception {
		Field field = Cache.class.getDeclaredField("db");
		field.setAccessible(true);
		return field.get(cache);
	}
}
