package Zeze.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import harness.JdbcTestBackend;
import harness.TestDatabaseConfig;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

/** 三种真实JDBC后端共用的KV契约；每个用例只创建、删除自己的随机表，不创建Application或系统元数据。 */
@NullMarked
abstract class JdbcDatabaseContract {
	protected abstract Config.DbType backend();

	@FunctionalInterface
	private interface TableCheck {
		void run(DatabaseJdbc database, Database.AbstractKVTable table) throws Exception;
	}

	private void withTable(TableCheck check) throws Exception {
		var conf = TestDatabaseConfig.jdbcTestConf(backend());
		if (conf == null) {
			Assumptions.abort("未选择" + backend() + "；运行jdbcTest并设置zezeTestDatabase及zezeTestDatabaseUrl");
			return;
		}
		// 显式选择后的驱动缺失、连接失败均须失败，不能退化为跳过。
		Class.forName(conf.getDruidConf().driverClassName);
		conf.setDisableOperates(true); // 此处只测KV表，避免初始化共享的_Zeze*元数据及存储过程。
		var database = JdbcTestBackend.of(backend()).create(conf);
		try {
			var name = "zeze_jdbc_" + UUID.randomUUID().toString().replace("-", "");
			var raw = database.openTable(name, Bean.hash32(name));
			try {
				raw.waitReady();
				var table = assertInstanceOf(Database.AbstractKVTable.class, raw);
				assertTrue(table.isNew(), "随机表应为本用例新建");
				check.run(database, table);
			} finally {
				try {
					raw.drop();
				} finally {
					raw.close();
				}
			}
		} finally {
			database.close();
		}
	}

	@Test
	@Tag("jdbc")
	@Timeout(45)
	void committedInsertsAreReadableAndWalked() throws Exception {
		withTable((database, table) -> {
			assertNull(table.find(integer(1)));
			assertRows(table, Map.of());
			insertPair(database, table);
			assertValue(table, 1, 11);
			assertValue(table, 2, 22);
			assertRows(table, Map.of(1, 11, 2, 22));
		});
	}

	@Test
	@Tag("jdbc")
	@Timeout(45)
	void replaceUpdatesAnExistingRow() throws Exception {
		withTable((database, table) -> {
			insertPair(database, table);
			try (var transaction = database.beginTransaction()) {
				table.replace(transaction, integer(1), integer(111));
				transaction.commit();
			}
			assertValue(table, 1, 111);
			assertValue(table, 2, 22);
			assertRows(table, Map.of(1, 111, 2, 22));
		});
	}

	@Test
	@Tag("jdbc")
	@Timeout(45)
	void rollbackDiscardsInsertsUpdatesAndDeletes() throws Exception {
		withTable((database, table) -> {
			insertPair(database, table);
			try (var transaction = database.beginTransaction()) {
				table.replace(transaction, integer(1), integer(111));
				table.remove(transaction, integer(2));
				table.replace(transaction, integer(3), integer(33));
				transaction.rollback();
			}
			assertValue(table, 1, 11);
			assertValue(table, 2, 22);
			assertNull(table.find(integer(3)));
			assertRows(table, Map.of(1, 11, 2, 22));
		});
	}

	@Test
	@Tag("jdbc")
	@Timeout(45)
	void committedDeletesRemoveExistingRows() throws Exception {
		withTable((database, table) -> {
			insertPair(database, table);
			assertValue(table, 1, 11);
			try (var transaction = database.beginTransaction()) {
				table.remove(transaction, integer(1));
				table.remove(transaction, integer(3)); // 删除不存在的键也应成功。
				transaction.commit();
			}
			assertNull(table.find(integer(1)));
			assertValue(table, 2, 22);
			assertRows(table, Map.of(2, 22));
			try (var transaction = database.beginTransaction()) {
				table.remove(transaction, integer(2));
				transaction.commit();
			}
			assertNull(table.find(integer(2)));
			assertRows(table, Map.of());
		});
	}

	@Test
	@Tag("jdbc")
	@Timeout(45)
	void oversizedWriteKeyIsRejectedBeforePersistence() throws Exception {
		withTable((database, table) -> {
			try (var transaction = database.beginTransaction()) {
				var key = ByteBuffer.Wrap(new byte[Database.eMaxKeyLength + 1]);
				var error = assertThrows(IllegalArgumentException.class,
						() -> table.replace(transaction, key, integer(1)));
				assertTrue(error.getMessage().contains(Integer.toString(Database.eMaxKeyLength + 1)));
				transaction.rollback();
			}
			assertRows(table, Map.of());
		});
	}

	private static void insertPair(DatabaseJdbc database, Database.AbstractKVTable table) throws Exception {
		try (var transaction = database.beginTransaction()) {
			table.replace(transaction, integer(1), integer(11));
			table.replace(transaction, integer(2), integer(22));
			transaction.commit();
		}
	}

	private static ByteBuffer integer(int value) {
		var buffer = ByteBuffer.Allocate();
		buffer.WriteInt(value);
		return buffer;
	}

	private static int decodeInteger(ByteBuffer buffer) {
		var value = buffer.ReadInt();
		assertEquals(0, buffer.size(), "整数解码不得残留字节");
		return value;
	}

	private static void assertValue(Database.AbstractKVTable table, int key, int expected) {
		var value = table.find(integer(key));
		assertNotNull(value, "提交后的键必须存在: " + key);
		assertEquals(expected, decodeInteger(value));
	}

	private static void assertRows(Database.AbstractKVTable table, Map<Integer, Integer> expected) throws Exception {
		var actual = new HashMap<Integer, Integer>();
		var count = table.walk((key, value) -> {
			assertNull(actual.put(decodeInteger(ByteBuffer.Wrap(key)), decodeInteger(ByteBuffer.Wrap(value))),
					"walk不得返回重复键");
			return true;
		});
		assertEquals(expected.size(), count);
		assertEquals(expected, actual);
	}
}
