package UnitTest.Zeze.Trans;

import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseMemory;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND2-T3-8：DatabaseMemory 的 Operates(schemas 版本记录)曾是进程级静态 Map 且不按库实例分区，
 * 同一进程两个 Memory 库（不同 DatabaseUrl）互相串扰：库 A 保存的版本记录让库 B 读到 A 的
 * data/version，B 的 schemas 兼容检查基于错误前像进行；clear() 也不清理该数据，测试间残留。
 *
 * 验证：版本记录按 DatabaseUrl 隔离（对齐 KV 表 databaseTables 的 url 分桶）；clear() 一并清理。
 */
@Fast
public class TestDatabaseMemoryOperatesPartition {
	private static DatabaseMemory newMemoryDb(String url) {
		var conf = new Config.DatabaseConf();
		conf.setDatabaseUrl(url);
		return new DatabaseMemory(null, conf); // 默认 DisableOperates=false，Operates 即库自身
	}

	private static ByteBuffer newBb(String s) {
		var bb = ByteBuffer.Allocate();
		bb.WriteString(s);
		return bb;
	}

	@Test
	public void testPartitionByDatabaseUrl() {
		var key = newBb("zeze.Schemas.V4.0");
		var dbA = newMemoryDb("mem_operates_partition_a");
		var dbB = newMemoryDb("mem_operates_partition_b");
		try {
			// A 库首次保存（version=0 新记录，Memory 实现存原值并返回 0）。
			var rcA = dbA.saveDataWithSameVersion(key, newBb("a1"), 0);
			Assertions.assertTrue(rcA.getValue());

			// B 库同 key 必须读不到 A 的记录：修复前共享静态 Map，B 会直接读到 A 的 data/version。
			Assertions.assertNull(dbB.getDataWithVersion(key));

			var rcB = dbB.saveDataWithSameVersion(key, newBb("b1"), 0);
			Assertions.assertTrue(rcB.getValue());

			// 各分区独立推进版本、互不覆盖数据。
			var rcA2 = dbA.saveDataWithSameVersion(key, newBb("a2"), rcA.getKey());
			Assertions.assertTrue(rcA2.getValue());
			Assertions.assertEquals(1L, rcA2.getKey().longValue()); // Memory 实现：第二次保存 ++version

			// 版本失配仍要被本分区检出（conflict 检测不被分区破坏）。
			Assertions.assertFalse(dbA.saveDataWithSameVersion(key, newBb("a3"), 99).getValue());

			var dvA = dbA.getDataWithVersion(key);
			var dvB = dbB.getDataWithVersion(key);
			Assertions.assertNotNull(dvA);
			Assertions.assertNotNull(dvB);
			Assertions.assertEquals(1, dvA.version);
			Assertions.assertEquals(0, dvB.version);
			Assertions.assertEquals("a2", dvA.data.ReadString());
			Assertions.assertEquals("b1", dvB.data.ReadString());
		} finally {
			dbA.close();
			dbB.close();
			DatabaseMemory.clear();
		}
	}

	@Test
	public void testClearAlsoCleansOperatesData() {
		var key = newBb("zeze.Schemas.V4.0");
		var db = newMemoryDb("mem_operates_partition_clear");
		try {
			Assertions.assertTrue(db.saveDataWithSameVersion(key, newBb("x"), 0).getValue());
			Assertions.assertNotNull(db.getDataWithVersion(key));
			DatabaseMemory.clear();
			// clear() 原本只清 KV 表，Operates 版本记录残留会让下个测试读到脏前像。
			Assertions.assertNull(db.getDataWithVersion(key));
		} finally {
			db.close();
			DatabaseMemory.clear();
		}
	}
}
