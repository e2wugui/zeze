package Zeze.Transaction;

import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestMemoryRenamedTableHandles {

	private static DatabaseMemory newDatabase() {
		var conf = new Config.DatabaseConf();
		conf.setDatabaseType(Config.DbType.Memory);
		conf.setDatabaseUrl(FastServerIds.URL_TEST_MEMORY_RENAMED_TABLE_HANDLES);
		return new DatabaseMemory(null, conf);
	}

	private static ByteBuffer bytes(int value) {
		return ByteBuffer.Wrap(new byte[] {(byte)value});
	}

	private static DatabaseMemory.TableMemory open(DatabaseMemory db, String name) {
		return (DatabaseMemory.TableMemory)db.openTable(name, 0);
	}

	private static void put(DatabaseMemory db, Database.Table table, ByteBuffer key, int value) throws Exception {
		try (var txn = db.beginTransaction()) {
			table.replace(txn, key, bytes(value));
			txn.commit();
		}
	}

	@Test
	public void renamedTableRejectsAnEntireInFlightBatch() throws Exception {
		var db = newDatabase();
		var peer = newDatabase();
		try {
			var key = bytes(1);
			var old = open(db, "PendingTable");
			var sibling = open(db, "UnaffectedTable");
			put(db, old, key, 10);
			put(db, sibling, key, 20);
			try (var pending = db.beginTransaction()) {
				sibling.replace(pending, key, bytes(21));
				old.replace(pending, key, bytes(11));
				old.remove(pending, bytes(2));

				peer.renameTable("PendingTable", "PendingTableBackup");
				var replacement = open(peer, "PendingTable");
				var backup = open(peer, "PendingTableBackup");
				put(peer, replacement, key, 30);
				put(peer, replacement, bytes(2), 40);

				assertThrows(IllegalStateException.class, pending::commit);
				assertEquals(bytes(20), sibling.find(key));
				assertEquals(bytes(10), backup.find(key));
				assertNull(backup.find(bytes(2)));
				assertEquals(bytes(30), replacement.find(key));
				assertEquals(bytes(40), replacement.find(bytes(2)));
			}
		} finally {
			peer.close();
			db.close();
		}
	}

	@Test
	public void renamedHandleCannotReadOrClearItsBackup() throws Exception {
		var db = newDatabase();
		try {
			var key = bytes(1);
			var old = open(db, "StaleTable");
			put(db, old, key, 10);
			db.renameTable("StaleTable", "StaleTableBackup");
			var backup = open(db, "StaleTableBackup");

			assertThrows(IllegalStateException.class, () -> old.find(key));
			assertThrows(IllegalStateException.class, () -> old.walk((k, v) -> true));
			assertThrows(IllegalStateException.class, old::getSize);
			assertThrows(IllegalStateException.class, old::clear);
			assertThrows(IllegalStateException.class, () -> put(db, old, key, 99));
			assertEquals(bytes(10), backup.find(key));
			assertNull(open(db, "StaleTable").find(key));
		} finally {
			db.close();
		}
	}
}
