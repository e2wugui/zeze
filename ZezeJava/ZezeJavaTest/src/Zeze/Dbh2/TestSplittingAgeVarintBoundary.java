package Zeze.Dbh2;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Config;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * MasterDatabase.readSplittingAge 必须按 ByteBuffer 剩余字节（bb.size() > 0）判定 varint 时间戳后
 * 是否存在源桶世代字符串，不得以固定 8 字节长度判断：
 * 1) 无 sourceRaft 的 9 字节 varint 时间戳（如负数或 >= 1L<<56）不得越界读 EOF 抛异常；
 * 2) 总编码长度 <= 8 字节的短 sourceRaft 必须保留其世代身份而不被静默丢弃为 null。
 */
@Fast
public class TestSplittingAgeVarintBoundary {

	private static Binary key(int i) {
		return new Binary(new byte[]{(byte)i});
	}

	@SuppressWarnings("unchecked")
	private static MasterDatabase getDatabase(Master master) throws Exception {
		Field field = Master.class.getDeclaredField("databases");
		field.setAccessible(true);
		return requireNonNull(((ConcurrentHashMap<String, MasterDatabase>)field.get(master)).get("db1"));
	}

	private static RocksDatabase.Table getRocksSplittingAge(MasterDatabase db) throws Exception {
		Field field = MasterDatabase.class.getDeclaredField("rocksSplittingAge");
		field.setAccessible(true);
		return (RocksDatabase.Table)field.get(db);
	}

	private static byte[] invokeSplittingAgeKey(String tableName, Binary keyFirst) throws Exception {
		Method m = MasterDatabase.class.getDeclaredMethod("splittingAgeKey", String.class, Binary.class);
		m.setAccessible(true);
		return (byte[])m.invoke(null, tableName, keyFirst);
	}

	private static @Nullable Object invokeReadSplittingAge(MasterDatabase db, String tableName, Binary keyFirst)
			throws Exception {
		Method m = MasterDatabase.class.getDeclaredMethod("readSplittingAge", String.class, Binary.class);
		m.setAccessible(true);
		return m.invoke(db, tableName, keyFirst);
	}

	@SuppressWarnings("unchecked")
	private static MasterTable.Data splittingTable(MasterDatabase db) throws Exception {
		Field field = MasterDatabase.class.getDeclaredField("splitting");
		field.setAccessible(true);
		return ((ConcurrentHashMap<String, MasterTable.Data>)field.get(db))
				.computeIfAbsent("t1", __ -> new MasterTable.Data());
	}

	private static long recordCreateTime(Object ageRecord) throws Exception {
		Method m = ageRecord.getClass().getDeclaredMethod("createTimeMs");
		m.setAccessible(true);
		return (long)m.invoke(ageRecord);
	}

	private static @Nullable String recordSourceRaft(Object ageRecord) throws Exception {
		Method m = ageRecord.getClass().getDeclaredMethod("sourceRaft");
		m.setAccessible(true);
		return (String)m.invoke(ageRecord);
	}

	@Test
	void testNineByteVarintTimestampWithoutSourceRaftDecodesCleanly(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir.resolve("db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var ageTable = getRocksSplittingAge(db);

			// 绝对值 >= (1L << 55) 的时间戳在 ByteBuffer.WriteLong 下编码为 9 字节（无 sourceRaft）。
			long[] nineByteTimestamps = new long[]{-(1L << 56), 1L << 56};
			for (int i = 0; i < nineByteTimestamps.length; ++i) {
				var k = key(i + 1);
				var rawKey = invokeSplittingAgeKey("t1", k);
				var bb = ByteBuffer.Allocate(16);
				bb.WriteLong(nineByteTimestamps[i]);
				Assertions.assertEquals(9, bb.size(), "测试前置条件：该时间戳应编码为 9 字节 varint");
				ageTable.put(rawKey, 0, rawKey.length, bb.Bytes, 0, bb.WriteIndex);

				var rec = invokeReadSplittingAge(db, "t1", k);
				Assertions.assertNotNull(rec, "9 字节无 sourceRaft 记录必须正常解码而非抛越界异常");
				var nonNullRec = requireNonNull(rec);
				Assertions.assertEquals(nineByteTimestamps[i], recordCreateTime(nonNullRec));
				Assertions.assertNull(recordSourceRaft(nonNullRec), "无 sourceRaft 字段时必须解码为 null");
				Assertions.assertEquals(nineByteTimestamps[i], db.getSplittingAgeCreateTime("t1", k));
			}

			// scanSplittingAge 遇到 9 字节基线记录不得抛出未捕获异常中断扫描。
			var main = new MasterTable.Data();
			db.getTables().put("t1", main);
			var entry = new BBucketMeta.Data();
			entry.setDatabaseName("db1");
			entry.setTableName("t1");
			entry.setKeyFirst(key(1));
			entry.setKeyLast(Binary.Empty);
			entry.setRaftConfig("<raft Name=\"r1\"><node Host=\"127.0.0.1\" Port=\"29911\"/></raft>");
			splittingTable(db).getBuckets().put(key(1), entry);
			master.getDbh2Config().setSplittingAgeWarnMs(0);
			Assertions.assertEquals(1, db.scanSplittingAge());
		} finally {
			master.close();
		}
	}

	@Test
	void testShortSourceRaftWithinEightBytesPreservesGeneration(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(tempDir.resolve("db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var ageTable = getRocksSplittingAge(db);

			// 用例 1：小时间戳（2 字节）+ 短 sourceRaft "rA"（3 字节）= 5 字节 <= 8 字节。
			var k1 = key(10);
			var rawKey1 = invokeSplittingAgeKey("t1", k1);
			var bb1 = ByteBuffer.Allocate(16);
			bb1.WriteLong(1234L);
			bb1.WriteString("rA");
			Assertions.assertTrue(bb1.size() <= 8, "测试前置条件：总编码长度 <= 8 字节");
			ageTable.put(rawKey1, 0, rawKey1.length, bb1.Bytes, 0, bb1.WriteIndex);

			var rec1 = invokeReadSplittingAge(db, "t1", k1);
			Assertions.assertNotNull(rec1);
			var nonNullRec1 = requireNonNull(rec1);
			Assertions.assertEquals(1234L, recordCreateTime(nonNullRec1));
			Assertions.assertEquals("rA", recordSourceRaft(nonNullRec1),
					"总长 <= 8 字节的短 sourceRaft 世代字段不得被丢弃为 null");

			// 用例 2：当前毫秒时间戳（6 字节）+ 单字符 sourceRaft "a"（2 字节）= 恰 8 字节。
			var k2 = key(11);
			var now = System.currentTimeMillis();
			var rawKey2 = invokeSplittingAgeKey("t1", k2);
			var bb2 = ByteBuffer.Allocate(16);
			bb2.WriteLong(now);
			bb2.WriteString("a");
			Assertions.assertEquals(8, bb2.size(), "测试前置条件：当前毫秒时间戳 + 1 字符恰为 8 字节");
			ageTable.put(rawKey2, 0, rawKey2.length, bb2.Bytes, 0, bb2.WriteIndex);

			var rec2 = invokeReadSplittingAge(db, "t1", k2);
			Assertions.assertNotNull(rec2);
			var nonNullRec2 = requireNonNull(rec2);
			Assertions.assertEquals(now, recordCreateTime(nonNullRec2));
			Assertions.assertEquals("a", recordSourceRaft(nonNullRec2),
					"恰为 8 字节的记录仍必须正确解出 sourceRaft");
		} finally {
			master.close();
		}
	}
}
