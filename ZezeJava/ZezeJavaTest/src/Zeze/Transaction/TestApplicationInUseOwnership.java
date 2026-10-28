package Zeze.Transaction;

import java.nio.file.Path;
import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestApplicationInUseOwnership {
	@TempDir
	Path directory;

	private static final class Registration extends Database.NullOperates {
		boolean held;
		boolean reject;
		int clears;

		@Override
		public void setInUse(int serverId, String global) {
			if (reject || held)
				throw new IllegalStateException("registration rejected");
			held = true;
		}

		@Override
		public int clearInUse(int serverId, String global) {
			clears++;
			held = false;
			return 0;
		}
	}

	private static final class RegistrationDatabase extends Database {
		RegistrationDatabase(Registration registration) {
			super(null, new Config.DatabaseConf());
			setDirectOperates(registration);
		}

		@Override
		public Database.Table openTable(String name, int id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Database.Transaction beginTransaction() {
			throw new UnsupportedOperationException();
		}
	}

	private Application application() throws Exception {
		var conf = new Config();
		conf.setNoDatabase(true);
		conf.setServiceManager("disable");
		conf.setServerId(FastServerIds.TEST_APPLICATION_IN_USE_OWNERSHIP);
		var database = new Config.DatabaseConf();
		database.setDatabaseType(Config.DbType.RocksDb);
		database.setDatabaseUrl(directory.resolve("database").toString());
		conf.getDatabaseConfMap().put("", database);
		var app = new Application("ApplicationInUseOwnership", conf);
		app.getDatabase("").close();
		app.getDatabases().clear();
		conf.setNoDatabase(false);
		return app;
	}

	@Test
	void rejectedRegistrationDoesNotClearExistingOwner() throws Exception {
		var registration = new Registration();
		registration.held = true;
		var app = application();
		app.getDatabases().put("", new RegistrationDatabase(registration));
		assertThrows(IllegalStateException.class, app::start);
		app.stop();
		assertTrue(registration.held);
		assertEquals(0, registration.clears);
	}

	@Test
	void partialStartOnlyClearsSuccessfullyAcquiredRegistrations() throws Exception {
		var first = new Registration();
		var refused = new Registration();
		refused.held = true;
		var untouched = new Registration();
		var app = application();
		// HashMap iteration follows these distinct buckets in both old and new implementations.
		app.getDatabases().put("", new RegistrationDatabase(first));
		app.getDatabases().put("a", new RegistrationDatabase(refused));
		app.getDatabases().put("b", new RegistrationDatabase(untouched));
		assertThrows(IllegalStateException.class, app::start);
		assertTrue(first.held);
		app.stop();
		assertFalse(first.held);
		assertEquals(1, first.clears);
		assertTrue(refused.held);
		assertEquals(0, refused.clears);
		assertEquals(0, untouched.clears);
		app.stop();
		assertEquals(1, first.clears);
	}
}
