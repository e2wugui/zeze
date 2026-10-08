package Zeze.Dbh2;

import harness.Extra;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.EndSplit;
import Zeze.Config;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * FND21 GA-D01 A4回归：splitting条目年龄观测（INV5）。
 * 现状：splitting条目唯一删除点是settle成功，滞留数天的条目master无从发现（无年龄、无扫描）。
 * 修复=时间戳独立rocks副表（tableName+keyFirst→创建时间，不动MasterTable.Data手写编码格式）
 * +master侧DaemonTimer周期扫描（Master.scanSplittingAges→MasterDatabase.scanSplittingAge），
 * 超龄（SplittingAgeWarnMs，默认10min量级）error告警含条目与源桶信息；无时间戳的存量条目
 * 按首次扫描起点起算（首扫只立基线不告警）。
 * **只观测不动作**：观测可时间驱动，消费必须结构驱动（A2/INV1）——超龄自动删除在途条目
 * 会让源桶settle收到eSplittingBucketNotFound而停止重试，人为重演(A)的永久读失败（方案D否决）。
 * 用例：①首扫立基线不告警；②超龄告警但条目不动（只观测）；③settle消费时年龄记录同步回收；
 * ④无记录的存量条目由首扫补基线（getSplittingAgeCreateTime可观测）。
 */
@Fast
@Extra
public class TestSplittingAgeObserve {

	private static Binary key(int i) {
		return new Binary(new byte[]{(byte)i});
	}

	private static BBucketMeta.Data newBucketMeta(Binary keyFirst, Binary keyLast, String raftConfig) {
		var bucket = new BBucketMeta.Data();
		bucket.setDatabaseName("db1");
		bucket.setTableName("t1");
		bucket.setRaftConfig(raftConfig);
		bucket.setKeyFirst(keyFirst);
		bucket.setKeyLast(keyLast);
		return bucket;
	}

	@SuppressWarnings("unchecked")
	private static MasterDatabase getDatabase(Master master) throws Exception {
		Field field = Master.class.getDeclaredField("databases");
		field.setAccessible(true);
		return ((ConcurrentHashMap<String, MasterDatabase>)field.get(master)).get("db1");
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, MasterTable.Data> getSplitting(MasterDatabase db) throws Exception {
		Field field = MasterDatabase.class.getDeclaredField("splitting");
		field.setAccessible(true);
		return (ConcurrentHashMap<String, MasterTable.Data>)field.get(db);
	}

	// —— 反射接缝（对齐TestFnd20GAC05口径）：旧基线无A4观测面（无阈值/无扫描/无年龄记录），
	// 测试对旧代码仍可编译运行，由断言消息显式判红，红因=真实行为差异（feature缺失）——

	private static void setAgeWarnMs(Master master, long value) {
		try {
			Zeze.Dbh2.Dbh2Config.class.getMethod("setSplittingAgeWarnMs", long.class)
					.invoke(master.getDbh2Config(), value);
		} catch (NoSuchMethodException e) {
			Assertions.fail("Dbh2Config.setSplittingAgeWarnMs缺失（GA-D01 A4修复不存在）："
					+ "splitting条目无年龄无扫描，滞留数天的条目master无从发现");
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static int scanSplittingAge(MasterDatabase db) {
		try {
			return (int)MasterDatabase.class.getMethod("scanSplittingAge").invoke(db);
		} catch (NoSuchMethodException e) {
			Assertions.fail("MasterDatabase.scanSplittingAge缺失（GA-D01 A4修复不存在）：无周期扫描观测");
			return -1;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static Long ageCreateTime(MasterDatabase db, String tableName, Binary keyFirst) {
		try {
			return (Long)MasterDatabase.class.getMethod("getSplittingAgeCreateTime", String.class, Binary.class)
					.invoke(db, tableName, keyFirst);
		} catch (NoSuchMethodException e) {
			Assertions.fail("MasterDatabase.getSplittingAgeCreateTime缺失（GA-D01 A4修复不存在）：无年龄记录");
			return null;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testAgeBaselineAlarmAndNoAction(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);
			table.getBuckets().put(key(2), newBucketMeta(key(2), Binary.Empty, "raftA"));
			// 直构存量条目（无时间戳——升级/直构形态）。
			var entry = newBucketMeta(key(5), Binary.Empty, "raftB");
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(key(5), entry);

			// 阈值0：任何非负年龄都超龄；但首扫必须只立基线不告警。
			setAgeWarnMs(master, 0L);

			Assertions.assertNull(ageCreateTime(db, "t1", key(5)),
					"存量条目无时间戳（首扫前）");
			Assertions.assertEquals(0, scanSplittingAge(db),
					"首扫只立基线不告警（无时间戳的存量条目按首次扫描起点起算）");
			Assertions.assertNotNull(ageCreateTime(db, "t1", key(5)),
					"首扫必须补基线");

			// 第二扫：阈值0下任何年龄（含0）都超龄——告警1条；但条目必须不动（只观测）。
			Assertions.assertEquals(1, scanSplittingAge(db),
					"超龄条目必须告警（返回告警数=error日志条数的可观测契约）");
			Assertions.assertSame(entry, getSplitting(db).get("t1").getBuckets().get(key(5)),
					"告警不得动作条目（消费必须结构驱动=A2；时间驱动自动删除会重演(A)）");

			// settle成功：条目消费且年龄记录同步回收。
			var r = new EndSplit();
			r.Argument.setFrom(newBucketMeta(key(2), key(5), "raftA"));
			r.Argument.setTo(entry);
			Assertions.assertEquals(0, db.endSplit(r), "settle必须成功");
			Assertions.assertNull(ageCreateTime(db, "t1", key(5)),
					"settle消费条目时年龄记录必须同步回收");
			Assertions.assertEquals(0, scanSplittingAge(db), "条目已消费，扫描无告警");
		} finally {
			master.close();
		}
	}

	/** 阈值内不告警：基线已立、年龄未达阈值时扫描静默（避免误报噪音）。 */
	@Test
	public void testNoAlarmBelowThreshold(@TempDir Path tempDir) throws Exception {
		Files.createDirectories(Path.of(tempDir.toString(), "db1"));
		var master = new Master(tempDir.toString(), new Config());
		try {
			var db = getDatabase(master);
			var table = new MasterTable.Data();
			db.getTables().put("t1", table);
			table.getBuckets().put(key(2), newBucketMeta(key(2), Binary.Empty, "raftA"));

			// 直构存量条目（无时间戳），首扫补基线，阈值内二次扫描静默。
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data())
					.getBuckets().put(key(5), newBucketMeta(key(5), Binary.Empty, "raftB"));
			Assertions.assertNull(ageCreateTime(db, "t1", key(5)), "未立基线前无记录");

			setAgeWarnMs(master, Long.MAX_VALUE / 2);
			Assertions.assertEquals(0, scanSplittingAge(db), "首扫立基线");
			Assertions.assertEquals(0, scanSplittingAge(db), "阈值内不告警");
			Assertions.assertNotNull(ageCreateTime(db, "t1", key(5)), "基线已立");
		} finally {
			master.close();
		}
	}
}
