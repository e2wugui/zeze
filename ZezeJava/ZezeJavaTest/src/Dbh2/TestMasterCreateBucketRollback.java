package Dbh2;

import java.io.File;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.CreateSplitBucket;
import Zeze.Config;
import Zeze.Dbh2.Master.Master;
import Zeze.Dbh2.Master.MasterDatabase;
import Zeze.Dbh2.Master.MasterTable;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import Zeze.Util.OutObject;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * MasterDatabase 创建桶的失败回滚（无manager注册时choiceManagers返回空，走失败路径）：
 * createSplitBucket失败后必须能重试（不能残留脏entry让后续重试被eSplittingBucketExist拒绝，
 * 分桶卡死直到Master重启）；createTable失败不能残留半初始化bucket。
 */
@Fast
public class TestMasterCreateBucketRollback {

	@Test
	public void testCreateSplitBucketRollback() throws Exception {
		var home = "testMasterCreateSplitBucketRollback";
		LogSequence.deleteDirectory(new File(home));
		var master = new Master(home, new Config());
		var db = new MasterDatabase(master, "db1");
		try {
			db.getTables().put("t1", new MasterTable.Data());
			// 第一次：没有manager可用，返回eTooFewManager。
			Assertions.assertEquals(master.errorCode(Master.eTooFewManager), db.createSplitBucket(newRequest()));
			// 失败后重试必须还是eTooFewManager（bug时残留脏entry，返回eSplittingBucketExist，分桶永久卡死）。
			Assertions.assertEquals(master.errorCode(Master.eTooFewManager), db.createSplitBucket(newRequest()));
		} finally {
			db.close();
			master.close();
			LogSequence.deleteDirectory(new File(home));
		}
	}

	@Test
	public void testCreateTableRollback() throws Exception {
		var home = "testMasterCreateTableRollback";
		LogSequence.deleteDirectory(new File(home));
		var master = new Master(home, new Config());
		var db = new MasterDatabase(master, "db1");
		try {
			var outIsNew = new OutObject<Boolean>();
			// 没有manager可用：createTable失败返回null。
			Assertions.assertNull(db.createTable("t1", outIsNew));
			// 失败不能残留半初始化bucket。
			Assertions.assertTrue(db.getTable("t1").getBuckets().isEmpty());
		} finally {
			db.close();
			master.close();
			LogSequence.deleteDirectory(new File(home));
		}
	}

	private static CreateSplitBucket newRequest() {
		var bucket = new BBucketMeta.Data();
		bucket.setDatabaseName("db1");
		bucket.setTableName("t1");
		bucket.setKeyFirst(new Binary(new byte[]{5}));
		bucket.setKeyLast(Binary.Empty);
		var r = new CreateSplitBucket();
		r.Argument = bucket;
		return r;
	}
}
