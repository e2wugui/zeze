package Zeze.Dbh2;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Config;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND19 GA-C04回归：createSplitBucket对已存在的splitting桶必须幂等返回（对齐
 * createTable"存在即返回"），而不是eSplittingBucketExist不设Result。
 * bug链：响应丢失/日志截断后源桶重试命中已存在分支 → agent抛异常 →
 * 源桶splittingMeta恒为null走首轮分支 → 每轮都在createSplitBucket失败，分桶永久卡死
 * （master侧splitting条目唯一出口endSplit永远等不到）。
 * 一致性校验不匹配（keyLast不同等）时保留eSplittingBucketExist。
 */
@Fast
public class TestGA04SplitBucketIdempotentResume {

	private static BBucketMeta.Data newBucketMeta(byte keyFirst, byte keyLast) {
		var bucket = new BBucketMeta.Data();
		bucket.setDatabaseName("db1");
		bucket.setTableName("t1");
		bucket.setRaftConfig("existRaftConfig");
		bucket.setKeyFirst(new Binary(new byte[]{keyFirst}));
		bucket.setKeyLast(keyLast == 0 ? Binary.Empty : new Binary(new byte[]{keyLast}));
		return bucket;
	}

	@SuppressWarnings("unchecked")
	private static ConcurrentHashMap<String, MasterTable.Data> getSplitting(MasterDatabase db) throws Exception {
		Field field = MasterDatabase.class.getDeclaredField("splitting");
		field.setAccessible(true);
		return (ConcurrentHashMap<String, MasterTable.Data>)field.get(db);
	}

	@Test
	public void testExistBucketReturnedIdempotently() throws Exception {
		var home = "testFnd19GA04Resume";
		LogSequence.deleteDirectory(new File(home));
		var master = new Master(home, new Config());
		var db = new MasterDatabase(master, "db1");
		try {
			db.getTables().put("t1", new MasterTable.Data());

			// 模拟首次createSplitBucket成功落盘：splitting表已有该桶。
			var created = newBucketMeta((byte)5, (byte)0);
			getSplitting(db).computeIfAbsent("t1", __ -> new MasterTable.Data()).getBuckets()
					.put(created.getKeyFirst(), created);

			// 源桶响应丢失后重试（相同meta）：必须幂等返回已存在桶（bug时eSplittingBucketExist，
			// agent端抛异常，断点续传无入口）。
			var retry = new CreateSplitBucket();
			retry.Argument = newBucketMeta((byte)5, (byte)0);
			Assertions.assertEquals(0, db.createSplitBucket(retry));
			Assertions.assertSame(created, retry.Result, "must return the existing bucket for resume");
			Assertions.assertEquals("existRaftConfig", retry.Result.getRaftConfig());

			// meta不一致（keyLast不同）不是重试，保留原错误。
			var mismatch = new CreateSplitBucket();
			mismatch.Argument = newBucketMeta((byte)5, (byte)9);
			Assertions.assertEquals(master.errorCode(Master.eSplittingBucketExist), db.createSplitBucket(mismatch));
		} finally {
			db.close();
			master.close();
			LogSequence.deleteDirectory(new File(home));
		}
	}
}
