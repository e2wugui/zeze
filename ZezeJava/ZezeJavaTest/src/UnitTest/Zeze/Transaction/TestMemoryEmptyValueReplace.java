package UnitTest.Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseMemory;
import harness.Fast;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * T1-F1 回归：bean 全默认值的编码结果是 0 字节，DatabaseMemory.MemTrans.replace 存
 * value.Copy()——size==0 时返回共享单例 ByteBuffer.Empty，与 removed 哨兵同引用，
 * commit 以引用相等判删会把"写入空编码值"当成 remove，已提交记录静默丢失。
 * 修复：removed 哨兵改为私有 new byte[0]，切断与公共常量的别名。
 */
@Fast
public class TestMemoryEmptyValueReplace {
	private DatabaseMemory db;

	@BeforeEach
	public void setUp() {
		var conf = new Config.DatabaseConf();
		conf.setDatabaseType(Config.DbType.Memory);
		conf.setDatabaseUrl("t1_f1_empty_value_replace");
		db = new DatabaseMemory(null, conf);
	}

	@AfterEach
	public void tearDown() {
		db.close();
		// 不调 DatabaseMemory.clear()：类并行下它会清掉其他类正在使用的静态表，
		// 固定 key 的 replace 语义幂等，无需全局清理。
	}

	@Test
	public void testEmptyEncodedValueNotRemoved() throws Exception {
		var table = (DatabaseMemory.TableMemory)db.openTable("t1", 0);
		var key = ByteBuffer.Wrap(new byte[] {1});

		// 空编码值（snapshotValue 为 0 字节）的 replace 不得被当成 remove。
		var txn = db.beginTransaction();
		table.replace(txn, key, ByteBuffer.Wrap(ByteBuffer.Empty));
		txn.commit();
		txn.close();

		var found = table.find(key);
		assertNotNull(found, "空编码值写入提交后必须可查（不得被误判为remove）");
		assertEquals(0, found.size());
		assertEquals(1, table.getSize());

		// remove 语义不受影响：仍能正常删除。
		var txn2 = db.beginTransaction();
		table.remove(txn2, key);
		txn2.commit();
		txn2.close();

		assertNull(table.find(key));
		assertEquals(0, table.getSize());
	}

	@Test
	public void testRemoveThenReplaceSameBatch() throws Exception {
		// 同一事务内先 remove 后 replace（batch 覆盖），最终语义为写入。
		var table = (DatabaseMemory.TableMemory)db.openTable("t1", 0);
		var key = ByteBuffer.Wrap(new byte[] {2});

		var txn = db.beginTransaction();
		table.replace(txn, key, ByteBuffer.Wrap(new byte[] {9}));
		txn.commit();
		txn.close();

		var txn2 = db.beginTransaction();
		table.remove(txn2, key);
		table.replace(txn2, key, ByteBuffer.Wrap(ByteBuffer.Empty)); // 覆盖回空值
		txn2.commit();
		txn2.close();

		assertNotNull(table.find(key), "同事务先remove后replace空值，最终必须是写入");
	}
}
