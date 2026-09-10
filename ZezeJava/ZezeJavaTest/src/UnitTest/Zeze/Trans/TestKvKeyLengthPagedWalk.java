package UnitTest.Zeze.Trans;

import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseMemory;
import demo.Module1.tAutoKeyRandom;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND3-02 回归：带游标（exclusiveStartKey）的遍历必须在统一入口检查 key 长度预算。
 * 修复前 MySql/SqlServer 的 walkKey/walkDesc/walkKeyDesc 分页变体跳过检查，
 * DatabaseMemory 则全文件一处检查都没有——超长游标 key 要么落库报不含表名的原生错误，
 * 要么在无限制后端上静默无结果。修复后检查收敛到 AbstractKVTable 的 typed 分页门面
 * 与 TableX.walkDatabaseRaw，这里用 Memory 后端 + 未开表的 tAutoKeyRandom（binary key）
 * 逐一验证 12 个入口（检查发生在触及 storage/后端实现之前）。
 */
@Fast
public class TestKvKeyLengthPagedWalk {

	private static Database.Table rawTable() {
		return new DatabaseMemory(null, new Config.DatabaseConf()).openTable("test_kv_key_length", 1);
	}

	@Test
	public void testTypedPagedWalks() throws Exception {
		var rawTable = rawTable();
		var t = new tAutoKeyRandom();
		var bigKey = new Binary(new byte[Database.eMaxKeyLength + 1]);

		Assertions.assertThrows(IllegalArgumentException.class,
				() -> rawTable.walk(t, bigKey, 1, (k, v) -> true));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> rawTable.walkDesc(t, bigKey, 1, (k, v) -> true));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> rawTable.walkKey(t, bigKey, 1, k -> true));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> rawTable.walkKeyDesc(t, bigKey, 1, k -> true));

		Assertions.assertThrows(IllegalArgumentException.class,
				() -> rawTable.walkDatabase(t, bigKey, 1, (k, v) -> true));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> rawTable.walkDatabaseDesc(t, bigKey, 1, (k, v) -> true));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> rawTable.walkDatabaseKey(t, bigKey, 1, k -> true));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> rawTable.walkDatabaseKeyDesc(t, bigKey, 1, k -> true));
	}

	@Test
	public void testRawPagedWalks() {
		var t = new tAutoKeyRandom();
		var bigKey = ByteBuffer.Wrap(new byte[Database.eMaxKeyLength + 1]);

		Assertions.assertThrows(IllegalArgumentException.class,
				() -> t.walkDatabaseRaw(bigKey, 1, (k, v) -> true));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> t.walkDatabaseRawDesc(bigKey, 1, (k, v) -> true));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> t.walkDatabaseRawKey(bigKey, 1, k -> true));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> t.walkDatabaseRawKeyDesc(bigKey, 1, k -> true));
	}

	@Test
	public void testBoundaryKeyAllowed() throws Exception {
		var rawTable = rawTable();
		var t = new tAutoKeyRandom();
		// 预算按编码后的 key 长度计算；Binary 编码带 2 字节长度头。
		var maxKey = new Binary(new byte[Database.eMaxKeyLength - 2]);

		Assertions.assertNull(rawTable.walk(t, maxKey, 1, (k, v) -> true));
		Assertions.assertNull(rawTable.walkKey(t, maxKey, 1, k -> true));
		Assertions.assertNull(rawTable.walkDatabase(t, maxKey, 1, (k, v) -> true));
		Assertions.assertNull(rawTable.walkDatabaseKey(t, maxKey, 1, k -> true));
		Assertions.assertNull(rawTable.find(t, maxKey));
	}

	@Test
	public void testFindReplaceRemove() throws Exception {
		// find/replace/remove 的执法点在各后端 raw 方法（写主路径 Record1.flush 直呼 raw，
		// 不经过 typed 门面）；DatabaseMemory 曾是唯一没有检查的后端。
		var db = new DatabaseMemory(null, new Config.DatabaseConf());
		var rawTable = (DatabaseMemory.TableMemory)db.openTable("test_kv_key_length", 1);
		var t = new tAutoKeyRandom();
		var bigKey = new Binary(new byte[Database.eMaxKeyLength + 1]);
		var txn = db.beginTransaction();

		Assertions.assertThrows(IllegalArgumentException.class, () -> rawTable.find(t, bigKey));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> rawTable.replace(txn, t.encodeKey(bigKey), t.encodeKey(bigKey)));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> rawTable.remove(txn, t.encodeKey(bigKey)));
	}
}
