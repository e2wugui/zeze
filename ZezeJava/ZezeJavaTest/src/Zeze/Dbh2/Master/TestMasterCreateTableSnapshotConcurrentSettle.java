package Zeze.Dbh2.Master;

import harness.Extra;
import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Builtin.Dbh2.Master.CreateTable;
import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Raft.LogSequence;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * CreateTable 应答与分桶结算的并发安全（dbh2-02）：ProcessCreateTableRequest 返回的
 * 必须是快照——bug 时把 live 主表对象（MasterTable.Data，含 live TreeMap）直接放进
 * 应答，rpc 发送线程同步编码遍历与 endSplit/endMove 持表锁的结构性 put/remove 并发
 * 触发 CME/应答失败。GetBuckets 处理器对同一数据已有 table.snapshot() 先例，
 * CreateTable 漏掉了同样的快照。形制对齐 TestMasterGetBucketsSnapshot（Master 直构
 * 不触网，反射取回播种实例）。
 */
@Fast
@Extra
public class TestMasterCreateTableSnapshotConcurrentSettle {

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
	public void testCreateTableResultSnapshotUnderConcurrentSettle() throws Exception {
		var home = "testMasterCreateTableSnapshot";
		LogSequence.deleteDirectory(new File(home));
		new File(home, "db1").mkdirs();
		var master = new Master(home, new Config());
		try {
			var db = getDatabase(master, "db1");
			var key2 = new Binary(new byte[]{5});
			var table = new MasterTable.Data();
			table.created = true; // 已建表：createTable 走已存在路径返回 map 内 live 实例
			table.buckets.put(Binary.Empty, newBucket(Binary.Empty, key2));
			db.getTables().put("t1", table);

			// 写线程模拟 endSplit/endMove 结算：持 table.lock 对 buckets 结构性 put/remove。
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
				// 读线程走真实处理器路径：CreateTable + 同步编码（rpc SendResult 的派发线程形态）。
				// bug 时编码遍历 live TreeMap 与写并发，高迭代概率 CME。
				for (int i = 0; i < 20_000; ++i) {
					var r = new CreateTable();
					r.Argument.setDatabase("db1");
					r.Argument.setTable("t1");
					Assertions.assertEquals(0, master.ProcessCreateTableRequest(r));
					Assertions.assertTrue(r.Result.encode().size() > 0); // 模拟rpc序列化读
					Assertions.assertNotSame(table, r.Result, "应答必须是快照，不能是live主表对象");
				}
			} finally {
				stop.set(true);
				writer.join(10_000);
			}

			// 快照确定性断言：处理器返回后持锁改表，再编码应答不受影响（bug 时编码出新增桶）。
			var r = new CreateTable();
			r.Argument.setDatabase("db1");
			r.Argument.setTable("t1");
			Assertions.assertEquals(0, master.ProcessCreateTableRequest(r));
			var encodedBefore = r.Result.encode();
			table.lock();
			try {
				table.buckets.put(key2, newBucket(key2, Binary.Empty));
			} finally {
				table.unlock();
			}
			Assertions.assertArrayEquals(encodedBefore.Bytes, r.Result.encode().Bytes,
					"快照应答不受处理器返回后的表修改影响");
		} finally {
			master.close();
			LogSequence.deleteDirectory(new File(home));
		}
	}
}
