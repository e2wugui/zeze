package Zeze.Transaction;

import org.jspecify.annotations.NullMarked;

import Zeze.Config;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseMemory;
import demo.Module1.tAutoKeyRandom;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 带游标（exclusiveStartKey）的遍历在统一入口按存量预算 2712 护栏（挡应用侧垃圾游标，
 * 存量键全放行）；typed 读写执法在 TableX 入口按 900（见 Database.eMaxKeyLength 契约）。
 * 检查收敛在 AbstractKVTable 的 typed 分页门面与 TableX.walkDatabaseRaw，这里用 Memory
 * 后端 + 未开表的 tAutoKeyRandom（binary key）逐一验证入口（检查发生在触及 storage 之前）。
 */
@Fast
@NullMarked
public class TestKvKeyLengthPagedWalk {

	private static Database.Table rawTable() {
		var conf = new Config.DatabaseConf();
		conf.setDatabaseUrl(FastServerIds.URL_TEST_KV_KEY_LENGTH_PAGED_WALK);
		return new DatabaseMemory(null, conf).openTable("test_kv_key_length", 1);
	}

	@Test
	public void testTypedPagedWalks() throws Exception {
		var rawTable = rawTable();
		var t = new tAutoKeyRandom();
		var bigKey = new Binary(new byte[Database.eMaxLegacyKeyLength + 1]); // 读路径按存量预算执法

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
		var bigKey = ByteBuffer.Wrap(new byte[Database.eMaxLegacyKeyLength + 1]); // 读路径按存量预算执法

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
		// 执法分层（Database.eMaxKeyLength 契约）：typed 读写入口在 TableX 按 900；
		// 后端 raw 的 find/remove 不检查（删除管线必须畅通），replace 保留 900 作为
		// flush 兜底断言。这里直接打 raw 层，钉分层行为。
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl(FastServerIds.URL_TEST_KV_KEY_LENGTH_PAGED_WALK);
		var db = new DatabaseMemory(null, dbConf);
		var rawTable = (DatabaseMemory.TableMemory)db.openTable("test_kv_key_length", 1);
		var t = new tAutoKeyRandom();
		var legacyKey = new Binary(new byte[Database.eMaxKeyLength + 1]); // 901：存量预算内
		var tooBigKey = new Binary(new byte[Database.eMaxLegacyKeyLength + 1]);
		var txn = db.beginTransaction();

		// 存量预算内（901）raw 层读/删不设限（拒绝发生在 typed 入口，事务成功之前）
		Assertions.assertNull(rawTable.find(t, legacyKey), "raw 层读不检查");
		rawTable.remove(txn, t.encodeKey(legacyKey)); // no-op，不得抛
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> rawTable.replace(txn, t.encodeKey(legacyKey), t.encodeKey(legacyKey)));
		// 超存量预算（2713）raw 读/删同样不抛：垃圾键防护在 typed 入口与游标护栏
		Assertions.assertNull(rawTable.find(t, tooBigKey));
		rawTable.remove(txn, t.encodeKey(tooBigKey)); // no-op，不得抛
	}
}
