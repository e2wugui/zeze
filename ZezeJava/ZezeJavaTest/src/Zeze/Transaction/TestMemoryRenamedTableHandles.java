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

	@Test
	public void rolledBackWritesByNameLeaveBackupNamesAvailable() throws Exception {
		var db = newDatabase();
		try {
			var key = bytes(1);
			var source = open(db, "RollbackSource");
			put(db, source, key, 10);
			try (var pending = (DatabaseMemory.MemTrans)db.beginTransaction()) {
				pending.remove("RollbackRemoveBackup", key);
				pending.replace("RollbackReplaceBackup", key, bytes(20));
				pending.rollback();
			}

			db.renameTable("RollbackSource", "RollbackRemoveBackup");
			db.renameTable("RollbackRemoveBackup", "RollbackReplaceBackup");
			assertEquals(bytes(10), open(db, "RollbackReplaceBackup").find(key));
		} finally {
			db.close();
		}
	}

	@Test
	public void writesByNameCanCreateMissingTablesOnCommit() {
		var db = newDatabase();
		try {
			var key = bytes(1);
			try (var pending = (DatabaseMemory.MemTrans)db.beginTransaction()) {
				pending.replace("CreateOnCommit", key, bytes(10));
				pending.commit();
			}
			assertEquals(bytes(10), open(db, "CreateOnCommit").find(key));
		} finally {
			db.close();
		}
	}

	@Test
	public void writesStagedWhileTableWasMissingCannotModifyARenamedTable() throws Exception {
		var db = newDatabase();
		try {
			var key = bytes(1);
			var source = open(db, "SourceForPendingName");
			var sibling = open(db, "SiblingForPendingName");
			put(db, source, key, 10);
			put(db, sibling, key, 30);
			try (var pending = (DatabaseMemory.MemTrans)db.beginTransaction()) {
				pending.replace("PendingRenameTarget", key, bytes(20));
				sibling.replace(pending, key, bytes(40));
				db.renameTable("SourceForPendingName", "PendingRenameTarget");
				assertThrows(IllegalStateException.class, pending::commit);
			}
			assertEquals(bytes(10), open(db, "PendingRenameTarget").find(key));
			assertEquals(bytes(30), sibling.find(key));
		} finally {
			db.close();
		}
	}

	@Test
	public void renamingBackDoesNotReviveOldHandlesOrWrites() throws Exception {
		var db = newDatabase();
		try {
			var key = bytes(1);
			var original = open(db, "RoundTripSource");
			put(db, original, key, 10);
			try (var pending = db.beginTransaction()) {
				original.replace(pending, key, bytes(20));
				db.renameTable("RoundTripSource", "RoundTripBackup");
				var backup = open(db, "RoundTripBackup");
				put(db, backup, key, 30);
				db.renameTable("RoundTripBackup", "RoundTripSource");

				assertThrows(IllegalStateException.class, pending::commit);
				assertThrows(IllegalStateException.class, () -> original.find(key));
				assertThrows(IllegalStateException.class, () -> backup.find(key));
				assertEquals(bytes(30), open(db, "RoundTripSource").find(key));
			}
		} finally {
			db.close();
		}
	}
}
