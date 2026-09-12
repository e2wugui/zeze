package UnitTest.Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Util.Cache;
import Zeze.Util.CacheObject;
import harness.Fast;

/**
 * FND4-10：当天清单（days_N）是退役机制的唯一台账（重启后RocksDb命中路径不再登记）。
 * 曾经以截断模式打开：同日重启（新实例）清空前次运行的条目，对应RocksDb记录永远无人清理。
 * 修复为追加模式：重启续写当天清单。场景=同目录先后两个Cache实例（等价进程重启）。
 */
@Fast
public class TestCacheAppendTodayResume {

	public static final class Obj implements CacheObject {
		public String id;
		public String value;

		public Obj() {
		}

		public Obj(String id, String value) {
			this.id = id;
			this.value = value;
		}

		@Override
		public String cacheId() {
			return id;
		}

		@Override
		public void encode(ByteBuffer bb) {
			bb.WriteString(value);
		}

		@Override
		public void decode(IByteBuffer bb) {
			value = bb.ReadString();
		}
	}

	@Test
	public void testSameDayRestartAppends(@TempDir Path tempDir) throws Exception {
		Zeze.Util.Task.tryInitThreadPool();
		var dir = tempDir.resolve("cache-test").toString();

		Obj o1;
		Obj o2;
		var cache = new Cache(dir, 16, id -> new Obj(id, "v1"), (id, bb) -> {
			var o = new Obj();
			o.decode(bb);
			return o;
		});
		o1 = (Obj)cache.get("key1");
		o2 = (Obj)cache.get("key2");
		Assertions.assertNotNull(o1);
		Assertions.assertNotNull(o2);
		cache.close(); // 进程停止（同日重启前的状态）

		// 同日"重启"：新实例get新key，appendToday打开已存在的days_N文件。
		var cache2 = new Cache(dir, 16, id -> new Obj(id, "v2"), (id, bb) -> {
			var o = new Obj();
			o.decode(bb);
			return o;
		});
		Assertions.assertNotNull(cache2.get("key3"));
		cache2.close();

		// 台账必须累积：重启前的key1/key2与重启后的key3都在当天清单里。
		var daysFiles = Files.list(tempDir.resolve("cache-test"))
				.filter(p -> p.getFileName().toString().startsWith("days_"))
				.toList();
		Assertions.assertEquals(1, daysFiles.size(), "同日重启共用一个days文件");
		List<String> lines = Files.readAllLines(daysFiles.get(0), StandardCharsets.UTF_8);
		Assertions.assertTrue(lines.contains("key1"), "重启前登记的key1必须保留，实际: " + lines);
		Assertions.assertTrue(lines.contains("key2"), "重启前登记的key2必须保留，实际: " + lines);
		Assertions.assertTrue(lines.contains("key3"), "重启后登记的key3必须在，实际: " + lines);
		Assertions.assertEquals(3, lines.size(), "台账恰好三条，实际: " + lines);
	}
}
