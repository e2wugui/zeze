package Zeze.Dbh2.Master;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.GetBuckets;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Master 读路径与分桶写入（endSplit/endMove 持table.lock改buckets）的并发安全：
 * ProcessGetBucketsRequest 返回的必须是快照——bug时直接返回表引用，
 * rpc序列化遍历TreeMap与写并发会CME/脏结构（分桶完成瞬间客户端reload是常态）。
 */
@Fast
public class TestMasterGetBucketsSnapshot {

	private static BBucketMeta.Data newBucket(Binary keyFirst, Binary keyLast) {
		var bucket = new BBucketMeta.Data();
		bucket.setDatabaseName("db1");
		bucket.setTableName("t1");
		bucket.setRaftConfig("");
		bucket.setKeyFirst(keyFirst);
	 bucket.setKeyLast(keyLast);
		return bucket;
	}

	// Master构造时扫描home目录自动注册db1（预建目录），这里反射取回实例用于播种数据。
	@SuppressWarnings("unchecked")
	private static MasterDatabase getDatabase(Master master, String name) throws Exception {
		Field field = Master.class.getDeclaredField("databases");
		field.setAccessible(true);
		return ((ConcurrentHashMap<String, MasterDatabase>)field.get(master)).get(name);
	}

	@Test
	public void testSnapshotIsolation() throws Exception {
		var home = "testMasterGetBucketsSnapshot";
		LogSequence.deleteDirectory(new File(home));
		new File(home, "db1").mkdirs();
		var master = new Master(home, new Config());
		try {
			var db = getDatabase(master, "db1");
			var table = new MasterTable.Data();
			table.buckets.put(Binary.Empty, newBucket(Binary.Empty, new Binary(new byte[]{5})));
			db.getTables().put("t1", table);

			var r = new GetBuckets();
			r.Argument.setDatabase("db1");
			r.Argument.setTable("t1");
			Assertions.assertEquals(0, master.ProcessGetBucketsRequest(r));
			// 返回的必须是快照，不能共享底层可变结构（bug时Result就是table本身）。
			Assertions.assertNotSame(table, r.Result);
			Assertions.assertEquals(1, r.Result.getBuckets().size());

			// 修改原表不影响已取到的快照。
			table.buckets.put(new Binary(new byte[]{5}), newBucket(new Binary(new byte[]{5}), Binary.Empty));
			Assertions.assertEquals(1, r.Result.getBuckets().size());
		} finally {
			master.close();
			LogSequence.deleteDirectory(new File(home));
		}
	}

	@Test
	public void testConcurrentReadVsWrite() throws Exception {
		var home = "testMasterGetBucketsConcurrent";
		LogSequence.deleteDirectory(new File(home));
		new File(home, "db1").mkdirs();
		var master = new Master(home, new Config());
		var key2 = new Binary(new byte[]{5});
		try {
			var db = getDatabase(master, "db1");
			var table = new MasterTable.Data();
			table.buckets.put(Binary.Empty, newBucket(Binary.Empty, key2));
			db.getTables().put("t1", table);

			// 写线程模拟endSplit/endMove：持table.lock增删桶。
			var stop = new AtomicBoolean(false);
			var writer = Thread.ofPlatform().start(() -> {
				int i = 0;
				while (!stop.get()) {
					table.lock();
					try {
						if ((i++ & 1) == 0)
							table.buckets.put(key2, newBucket(key2, Binary.Empty));
						else
							table.buckets.remove(key2);
					} finally {
						table.unlock();
					}
				}
			});
			try {
				// 读线程走真实读路径：GetBuckets + 序列化。
				for (int i = 0; i < 20_000; ++i) {
					var r = new GetBuckets();
					r.Argument.setDatabase("db1");
					r.Argument.setTable("t1");
					Assertions.assertEquals(0, master.ProcessGetBucketsRequest(r));
					Assertions.assertTrue(r.Result.encode().size() > 0); // 模拟rpc序列化读
				}
			} finally {
				stop.set(true);
				writer.join(10_000);
			}
		} finally {
			master.close();
			LogSequence.deleteDirectory(new File(home));
		}
	}
}
