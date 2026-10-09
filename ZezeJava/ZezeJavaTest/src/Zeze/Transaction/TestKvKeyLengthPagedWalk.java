package Zeze.Trans;

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
 * 带游标（exclusiveStartKey）的遍历必须在统一入口检查 key 长度预算：写预算900
 * （跨后端迁移上限）只约束新写，读/删/游标按存量预算2712放行（旧预算内的存量key
 * 必须可读可删可遍历定位，否则合法存量变成不可访问数据）。检查收敛在
 * AbstractKVTable 的 typed 分页门面与 TableX.walkDatabaseRaw，这里用 Memory 后端 +
 * 未开表的 tAutoKeyRandom（binary key）逐一验证入口（检查发生在触及 storage 之前）。
 */
@Fast
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
		// find/replace/remove 的执法点在各后端 raw 方法（写主路径 Record1.flush 直呼 raw，
		// 不经过 typed 门面）；写按900，读/删按存量预算2712。
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl(FastServerIds.URL_TEST_KV_KEY_LENGTH_PAGED_WALK);
		var db = new DatabaseMemory(null, dbConf);
		var rawTable = (DatabaseMemory.TableMemory)db.openTable("test_kv_key_length", 1);
		var t = new tAutoKeyRandom();
		var legacyKey = new Binary(new byte[Database.eMaxKeyLength + 1]); // 901：存量预算内
		var tooBigKey = new Binary(new byte[Database.eMaxLegacyKeyLength + 1]);
		var txn = db.beginTransaction();

		// 存量预算内（901）可读可删；新写（replace）仍按900拒绝
		Assertions.assertNull(rawTable.find(t, legacyKey), "存量预算内的key必须可读");
		rawTable.remove(txn, t.encodeKey(legacyKey)); // no-op，不得抛
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> rawTable.replace(txn, t.encodeKey(legacyKey), t.encodeKey(legacyKey)));
		// 超存量预算（2713）读/删照旧拒绝
		Assertions.assertThrows(IllegalArgumentException.class, () -> rawTable.find(t, tooBigKey));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> rawTable.remove(txn, t.encodeKey(tooBigKey)));
	}
}
