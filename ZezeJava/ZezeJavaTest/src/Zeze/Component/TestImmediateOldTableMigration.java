package Zeze.Component;

import harness.Fast;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.CheckpointMode;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TestRrsReadFastPathOldTableMigration.MigrateBean;
import Zeze.Transaction.TestRrsReadFastPathOldTableMigration.tMigrate;

@Fast
public class TestImmediateOldTableMigration {
	@TempDir
	Path directory;

	private final class Env implements AutoCloseable {
		final Application app;
		final tMigrate table = new tMigrate();

		Env(boolean withOldDatabase) throws Exception {
			var config = TakeoverTestEnv.newConf("dryrun", 600_000, 600_000);
			config.setCheckpointMode(CheckpointMode.Immediately);
			config.setDefaultTableConf(new Config.TableConf());
			var primary = new Config.DatabaseConf();
			primary.setDatabaseType(Config.DbType.RocksDb);
			primary.setDatabaseUrl(directory.resolve("primary").toString());
			config.getDatabaseConfMap().put("", primary);
			if (withOldDatabase) {
				config.getDefaultTableConf().setDatabaseOldMode(1);
				config.getDefaultTableConf().setDatabaseOldName("old");
				var old = new Config.DatabaseConf();
				old.setName("old");
				old.setDatabaseType(Config.DbType.RocksDb);
				old.setDatabaseUrl(directory.resolve("old").toString());
				config.getDatabaseConfMap().put("old", old);
			}
			app = new Application("ImmediateOldTableMigration." + config.getServerId(), config);
			app.initialize(new TakeoverTestEnv.TestAppBase(app));
			app.addTable("", table);
			app.start();
		}

		@Override
		public void close() throws Exception {
			app.stop();
		}
	}

	@Test
	public void pureReadPersistsInThePrimaryAndSurvivesRemovingTheOldDatabase() throws Exception {
		try (var env = new Env(true)) {
			var oldDatabase = env.app.getDatabase("old");
			var oldTable = oldDatabase.openTable(env.table.getName(), env.table.getId());
			var value = new MigrateBean();
			value.value = 42;
			try (var transaction = oldDatabase.beginTransaction()) {
				oldTable.replace(transaction, env.table.encodeKey(1L), ByteBuffer.encode(value));
				transaction.commit();
			}
			Assertions.assertNull(env.table.selectFromDatabase(1L));
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				var loaded = env.table.get(1L);
				Assertions.assertNotNull(loaded);
				Assertions.assertEquals(42, loaded.value);
				return Procedure.Success;
			}, "ImmediateOldTableMigration.read").call());
			var migrated = env.table.selectFromDatabase(1L);
			Assertions.assertNotNull(migrated, "successful read must copy the record into the primary database");
			Assertions.assertEquals(42, migrated.value);
		}
		try (var env = new Env(false)) {
			Assertions.assertEquals(Procedure.Success, env.app.newProcedure(() -> {
				var loaded = env.table.get(1L);
				Assertions.assertNotNull(loaded, "the migrated record must no longer depend on the old database");
				Assertions.assertEquals(42, loaded.value);
				return Procedure.Success;
			}, "ImmediateOldTableMigration.restart").call());
		}
	}
}
