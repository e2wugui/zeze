package Zeze;

import java.util.HashMap;
import java.util.UUID;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseMemory;
import Zeze.Util.KV;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Fast
public class TestSchemasPublishPreservesReopenedTable {

	private static final String TABLE = "PublishGeneration";
	private static final String BACKUP = "zeze_backup_" + TABLE + "_0";

	private static Schemas schemas(int version) {
		var schemas = new Schemas();
		schemas.addBean(new Schemas.Bean("PublishGenerationValue", false));
		schemas.addTable(new Schemas.Table(TABLE, "long", "PublishGenerationValue", version));
		return schemas;
	}

	private static final class SchemaDatabase extends Database implements Database.Operates {
		private final DatabaseMemory metadata;
		private final HashMap<String, String> physicalTables = new HashMap<>();
		private int renameCalls;
		private int publishAttempts;
		private boolean conflictNextSave;

		private SchemaDatabase(Application app) {
			super(app, new Config.DatabaseConf());
			var metadataConf = new Config.DatabaseConf();
			metadataConf.setDatabaseUrl("schemas-publish-generation-" + UUID.randomUUID());
			metadata = new DatabaseMemory(null, metadataConf);
			setDirectOperates(this);
		}

		@Override
		public Database.Table openTable(String name, int id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Database.Transaction beginTransaction() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void renameTable(String source, String target) {
			renameCalls++;
			if (!physicalTables.containsKey(source) && physicalTables.containsKey(target))
				return;
			if (physicalTables.containsKey(target))
				throw new IllegalStateException("rename target already exists: " + target);
			physicalTables.put(target, requireNonNull(physicalTables.remove(source)));
		}

		@Override
		public void setInUse(int localId, String global) {
		}

		@Override
		public int clearInUse(int localId, String global) {
			return 0;
		}

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
			return metadata.getDataWithVersion(key);
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			publishAttempts++;
			if (conflictNextSave) {
				conflictNextSave = false;
				var concurrent = requireNonNull(metadata.getDataWithVersion(key));
				// A competing publisher saves the same previous schema with a newer CAS version.
				metadata.saveDataWithSameVersion(key, requireNonNull(concurrent.data), concurrent.version);
				return KV.create(0L, false);
			}
			return requireNonNull(metadata.saveDataWithSameVersion(key, data, version));
		}

		@Override
		public void close() {
			metadata.close();
			super.close();
		}
	}

	private static void publishUpgrade(boolean conflict) throws Exception {
		var conf = new Config();
		conf.setNoDatabase(true);
		conf.setServerId(-1);
		conf.setServiceManager("disable");
		conf.setDefaultTableConf(new Config.TableConf());
		var app = new Application("TestSchemasPublishPreservesReopenedTable", conf);
		var db = new SchemaDatabase(app);
		try {
			app.getDatabases().put("", db);
			var key = ByteBuffer.Allocate();
			key.WriteString("zeze.Schemas.V4." + conf.getServerId());
			var previous = ByteBuffer.Allocate();
			schemas(0).encode(previous);
			db.metadata.saveDataWithSameVersion(key, previous, 0);
			db.physicalTables.put(TABLE, "previous generation rows");

			app.setSchemas(schemas(1));
			app.schemasCompatible();
			assertEquals("previous generation rows", db.physicalTables.get(BACKUP));

			// atomicOpenDatabase opens a fresh current table between checking and publishing.
			db.physicalTables.put(TABLE, "current generation rows");
			db.conflictNextSave = conflict;
			app.publishSchemas();

			assertEquals(1, db.renameCalls, "publishing or retrying CAS must not repeat physical migration");
			assertEquals("previous generation rows", db.physicalTables.get(BACKUP));
			assertEquals("current generation rows", db.physicalTables.get(TABLE));
			assertEquals(conflict ? 2 : 1, db.publishAttempts);
			var saved = new Schemas();
			saved.decode(requireNonNull(requireNonNull(db.getDataWithVersion(key)).data));
			assertEquals(1, requireNonNull(saved.tables.get(TABLE)).version);
		} finally {
			app.stop();
			db.close();
		}
	}

	@Test
	public void publishingKeepsTheReopenedTableAndItsBackup() throws Exception {
		publishUpgrade(false);
	}

	@Test
	public void publicationCasRetryKeepsTheReopenedTableAndItsBackup() throws Exception {
		publishUpgrade(true);
	}
}
