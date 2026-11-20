package Zeze;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseMemory;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestMemorySchemaUpgradeRetry {
	private static final String TABLE = "MemorySchemaUpgrade";
	private static final String BACKUP = "zeze_backup_" + TABLE + "_0";

	private static Application newApp(String url, int serverId, int version) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl(url);
		conf.getDatabaseConfMap().put("", dbConf);
		var app = new Application("TestMemorySchemaUpgradeRetry@" + serverId, conf);
		var schemas = new Schemas();
		schemas.addTable(new Schemas.Table(TABLE, "long", "int", version));
		app.setSchemas(schemas);
		return app;
	}

	private static DatabaseMemory database(Application app) {
		return (DatabaseMemory)app.getDatabase("");
	}

	private static DatabaseMemory.TableMemory open(DatabaseMemory db, String name) {
		return (DatabaseMemory.TableMemory)db.openTable(name, 0);
	}

	private static ByteBuffer bytes(int value) {
		return ByteBuffer.Wrap(new byte[] {(byte)value});
	}

	private static void write(DatabaseMemory db, String name, int value) throws Exception {
		var table = open(db, name);
		try (var txn = db.beginTransaction()) {
			table.replace(txn, bytes(1), bytes(value));
			txn.commit();
		}
	}

	private static int publishedVersion(Application app) {
		var key = ByteBuffer.Allocate();
		key.WriteString("zeze.Schemas.V4." + app.getConfig().getServerId());
		var metadata = requireNonNull(database(app).getDataWithVersion(key));
		var schemas = new Schemas();
		schemas.decode(requireNonNull(metadata.data));
		return requireNonNull(schemas.tables.get(TABLE)).version;
	}

	private static void seedPreviousSchema(String url, int serverId) throws Exception {
		var previous = newApp(url, serverId, 0);
		try {
			previous.publishSchemas();
			write(database(previous), TABLE, 10);
		} finally {
			database(previous).close();
		}
	}

	private static void assertRowsPreserved(DatabaseMemory db) {
		assertEquals(bytes(10), open(db, BACKUP).find(bytes(1)));
		assertEquals(bytes(20), open(db, TABLE).find(bytes(1)));
	}

	@Test
	public void startupRetriesAfterTheReplacementOpenedBeforeSchemaPublish() throws Exception {
		int serverId = FastServerIds.takeoverPoolNext();
		String url = "memory_schema_retry_" + serverId;
		seedPreviousSchema(url, serverId);

		var interrupted = newApp(url, serverId, 1);
		try {
			interrupted.schemasCompatible();
			// 模拟开表成功后、publishSchemas 之前中断，同一 JVM 保留后端数据。
			write(database(interrupted), TABLE, 20);
			assertRowsPreserved(database(interrupted));
			assertEquals(0, publishedVersion(interrupted));
		} finally {
			database(interrupted).close();
		}

		var retry = newApp(url, serverId, 1);
		try {
			retry.start();
			assertEquals(1, publishedVersion(retry));
			assertRowsPreserved(database(retry));
		} finally {
			retry.stop();
		}
	}

	@Test
	public void applicationsWithSeparateSchemaMetadataCanUpgradeTheSharedTable() throws Exception {
		int firstId = FastServerIds.takeoverPoolNext();
		int peerId = FastServerIds.takeoverPoolNext();
		String url = "memory_schema_peers_" + firstId;
		seedPreviousSchema(url, firstId);
		seedPreviousSchema(url, peerId);

		var first = newApp(url, firstId, 1);
		try {
			first.start();
			write(database(first), TABLE, 20);
			var peer = newApp(url, peerId, 1);
			try {
				assertEquals(0, publishedVersion(peer));
				peer.start();
				assertEquals(1, publishedVersion(peer));
				assertRowsPreserved(database(peer));
			} finally {
				peer.stop();
			}
		} finally {
			first.stop();
		}
	}

	@Test
	public void upgradeRejectsABackupCreatedFromAnotherTable() throws Exception {
		int serverId = FastServerIds.takeoverPoolNext();
		String url = "memory_schema_conflict_" + serverId;
		seedPreviousSchema(url, serverId);
		var upgrade = newApp(url, serverId, 1);
		var db = database(upgrade);
		try {
			write(db, "UnrelatedTable", 20);
			db.renameTable("UnrelatedTable", BACKUP);
			assertThrows(IllegalStateException.class, upgrade::schemasCompatible);
			assertEquals(bytes(10), open(db, TABLE).find(bytes(1)));
			assertEquals(bytes(20), open(db, BACKUP).find(bytes(1)));
			assertEquals(0, publishedVersion(upgrade));
		} finally {
			db.close();
		}
	}
}
