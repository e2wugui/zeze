package Zeze.Transaction;

import java.lang.reflect.Field;
import java.util.TreeMap;

import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * key长度预算只约束新写：读与删除按旧预算（2712）放行存量——预算从2712调小到
 * 900后，存量901..2712的key仍必须可读可删，否则合法存量变成不可访问数据，
 * 业务无法读取、清理或迁移。新写（replace）仍按900拒绝，跨后端迁移预检
 * 同理由写预算承担。
 */
@Fast
public class TestLegacyLongKeyReadableRemovable {

	@Test
	public void legacyLongKeyRemainsAccessibleForReadAndDelete() throws Exception {
		var conf = new Config.DatabaseConf();
		conf.setDatabaseUrl("legacy_long_key_access");
		var db = new DatabaseMemory(null, conf);
		try {
			var table = (DatabaseMemory.TableMemory)db.openTable("LegacyKeyTable", 1);

			// 构造901字节存量key（旧预算内、新预算外），经反射种入原始存储
			// （新写路径按900拒绝，无法经replace写入）。
			var legacyKey = ByteBuffer.Allocate(901);
			legacyKey.WriteBytes(new byte[901]);
			var legacyValue = new byte[]{7, 7, 7};
			Field mapField = DatabaseMemory.TableMemory.class.getDeclaredField("map");
			mapField.setAccessible(true);
			@SuppressWarnings("unchecked")
			var rawMap = (TreeMap<ByteBuffer, byte[]>)mapField.get(table);
			rawMap.put(legacyKey, legacyValue);

			// 存量读：必须可读（修复前：901 > 900 抛异常）
			var found = table.find(legacyKey);
			assertNotNull(found, "存量901字节key必须可读");
			assertArrayEquals(legacyValue, found.Copy());

			// 存量删：必须可删（业务清理/迁移的必要路径）
			try (var txn = db.beginTransaction()) {
				table.remove(txn, legacyKey);
				txn.commit();
			}
			assertNull(table.find(legacyKey), "删除后不可读");

			// 新写仍按900执法（迁移预算不放松）
			try (var txn = db.beginTransaction()) {
				var again = ByteBuffer.Allocate(901);
				again.WriteBytes(new byte[901]);
				assertThrows(IllegalArgumentException.class, () -> table.replace(txn, again,
						ByteBuffer.Wrap(new byte[]{1})), "新写仍受900预算约束");
				txn.rollback();
			}
		} finally {
			db.close();
		}
	}
}
