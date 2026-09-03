package UnitTest.Zeze.Util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import Zeze.Raft.LogSequence;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * CARRY-ROCKS-CLEAR 回归：RocksDatabase.Table.clear() 必须真正清空列族。
 * 修复前：clear() 把未定位的迭代器直接传给 deleteToEnd，其开头的
 * it.isValid() 对未 seek 的迭代器恒 false，deleteRange 从不执行，
 * clear 对任何列族都是 no-op（依赖它的清理调用点全部静默失效）。
 */
@Fast
public class TestRocksDatabaseTableClear {
	@Test
	public void testClearDeletesAllKeys() throws Exception {
		var path = Path.of("TestRocksDatabaseTableClear");
		LogSequence.deleteDirectory(path.toFile());
		try (var db = new RocksDatabase(path.toString())) {
			var table = db.getOrAddTable("t_clear");
			// 不同长度的key：覆盖 deleteToEnd 首尾定位与 last 扩 1 字节边界的技巧。
			table.put(b("a"), b("v1"));
			table.put(b("ab"), b("v2"));
			table.put(b("b"), b("v3"));
			table.put(b("zzzz"), b("v4"));
			Assertions.assertEquals(4, count(table));

			table.clear();

			// 修复前 clear 是 no-op：count 仍为 4，get 仍返回旧值，以下断言全红。
			Assertions.assertEquals(0, count(table), "clear后列族必须为空");
			Assertions.assertNull(table.get(b("a")));
			Assertions.assertNull(table.get(b("ab")));
			Assertions.assertNull(table.get(b("b")));
			Assertions.assertNull(table.get(b("zzzz")));

			// clear 后列族可继续读写。
			table.put(b("new"), b("v5"));
			Assertions.assertArrayEquals(b("v5"), table.get(b("new")));
			Assertions.assertEquals(1, count(table));

			// 空表再次 clear 幂等，不抛异常。
			table.clear();
			Assertions.assertEquals(0, count(table));
		} finally {
			LogSequence.deleteDirectory(path.toFile());
		}
	}

	private static byte[] b(String s) {
		return s.getBytes(StandardCharsets.UTF_8);
	}

	private static int count(RocksDatabase.Table table) {
		int n = 0;
		try (var it = table.iterator()) {
			for (it.seekToFirst(); it.isValid(); it.next())
				n++;
		}
		return n;
	}
}
