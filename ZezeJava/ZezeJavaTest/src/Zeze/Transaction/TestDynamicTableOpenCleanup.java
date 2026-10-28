package Zeze.Transaction;

import java.nio.file.Path;
import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jspecify.annotations.Nullable;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestDynamicTableOpenCleanup {
	@TempDir
	Path directory;
	private static final class OpeningTable extends TestAddTableAtomicity.StubTable {
		final RuntimeException openFailure = new IllegalStateException("open failed");
		boolean failOpen;
		boolean failClose;
		int closes;

		OpeningTable(String name, boolean failOpen) {
			super(Bean.hash32(name), name);
			this.failOpen = failOpen;
		}

		@Override
		public @Nullable Storage<?, ?> open(Application app, Database database,
				DatabaseRocksDb.@Nullable Table localTable) {
			if (failOpen)
				throw openFailure;
			return null;
		}

		@Override
		void close() {
			closes++;
			if (failClose)
				throw new IllegalStateException("close failed");
		}
	}

	private Application application() throws Exception {
		var conf = new Config();
		conf.setNoDatabase(true);
		conf.setServiceManager("disable");
		var database = new Config.DatabaseConf();
		database.setDatabaseType(Config.DbType.RocksDb);
		database.setDatabaseUrl(directory.resolve("database").toString());
		conf.getDatabaseConfMap().put("", database);
		return new Application("DynamicTableOpenCleanup", conf);
	}

	@Test
	void failedOpenRemovesRegistrationAndAllowsRetry() throws Exception {
		var app = application();
		var table = new OpeningTable("DynamicOpenCleanupRetry", true);
		try {
			assertSame(table.openFailure, assertThrows(IllegalStateException.class,
					() -> app.openDynamicTable("", table)));
			assertNull(app.getTable(table.getId()));
			assertNull(app.getTable(table.getName()));
			assertNull(app.getDatabase("").getTable(table.getName()));
			assertNull(TableKey.tables.get(table.getId()));
			assertEquals(1, table.closes);
			table.failOpen = false;
			app.openDynamicTable("", table);
			assertSame(table, app.getTable(table.getId()));
			assertSame(table, app.getDatabase("").getTable(table.getName()));
		} finally {
			app.getDatabase("").close();
		}
	}

	@Test
	void cleanupFailurePreservesOpenFailureAndRemovesRegistration() throws Exception {
		var app = application();
		var table = new OpeningTable("DynamicOpenCleanupCloseFailure", true);
		table.failClose = true;
		assertSame(table.openFailure, assertThrows(IllegalStateException.class,
				() -> app.openDynamicTable("", table)));
		assertEquals(1, table.openFailure.getSuppressed().length);
		assertNull(app.getTable(table.getId()));
		assertNull(app.getDatabase("").getTable(table.getName()));
		app.getDatabase("").close();
	}

	@Test
	void duplicateRegistrationDoesNotCloseExistingTable() throws Exception {
		var app = application();
		var existing = new OpeningTable("DynamicOpenCleanupExisting", false);
		try {
			app.openDynamicTable("", existing);
			var duplicate = new OpeningTable(existing.getName(), true);
			assertThrows(IllegalStateException.class, () -> app.openDynamicTable("", duplicate));
			assertSame(existing, app.getTable(existing.getId()));
			assertSame(existing, app.getDatabase("").getTable(existing.getName()));
			assertEquals(0, existing.closes);
			assertEquals(0, duplicate.closes);
		} finally {
			app.getDatabase("").close();
		}
	}
}
