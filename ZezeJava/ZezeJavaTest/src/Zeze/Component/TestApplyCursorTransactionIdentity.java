package Zeze.Component;

import Zeze.Application;
import Zeze.Config;
import Zeze.History.ApplyDatabaseZeze;
import Zeze.Util.Id128;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestApplyCursorTransactionIdentity {
	private static final class Env implements AutoCloseable {
		final Application app;
		final ApplyDatabaseZeze first;
		final ApplyDatabaseZeze second;

		Env() throws Exception {
			var conf = TakeoverTestEnv.newConf("off", 3000, 1000);
			conf.setNoDatabase(true);
			for (var name : new String[]{"applyFirst", "applySecond"}) {
				var database = new Config.DatabaseConf();
				database.setName(name);
				database.setDatabaseUrl(name + "_" + conf.getServerId());
				conf.getDatabaseConfMap().put(name, database);
			}
			app = new Application("TestApplyCursorTransactionIdentity", conf);
			first = new ApplyDatabaseZeze(app, "applyFirst");
			second = new ApplyDatabaseZeze(app, "applySecond");
		}

		@Override public void close() throws Exception { app.stop(); }
	}

	@Test
	void rejectsTransactionFromAnotherDatabase() throws Exception {
		try (var env = new Env(); var firstTxn = env.first.beginRecordTxn(); var secondTxn = env.second.beginRecordTxn()) {
			assertThrows(IllegalStateException.class, () -> env.second.saveCursor(new Id128(0, 3), firstTxn));
			firstTxn.commit();
			assertNull(env.second.loadCursor());
		}
	}

	@Test
	void rejectsAnotherWrapperEvenWhenThePhysicalDatabaseMatches() throws Exception {
		try (var env = new Env(); var txn = env.first.beginRecordTxn()) {
			var otherWrapper = new ApplyDatabaseZeze(env.app, "applyFirst");
			assertThrows(IllegalStateException.class, () -> otherWrapper.saveCursor(new Id128(0, 4), txn));
		}
	}

	@Test
	void rejectsFinishedTransactionAndPreservesActiveCursor() throws Exception {
		try (var env = new Env()) {
			var finished = env.first.beginRecordTxn();
			finished.rollback();
			try (var current = env.first.beginRecordTxn()) {
				assertThrows(IllegalStateException.class, () -> env.first.saveCursor(new Id128(0, 5), finished));
				env.first.saveCursor(new Id128(0, 6), current);
				assertNull(env.first.loadCursor());
				current.commit();
			}
			assertEquals(new Id128(0, 6), env.first.loadCursor());
			try (var rolledBack = env.first.beginRecordTxn()) {
				env.first.saveCursor(new Id128(0, 7), rolledBack);
			}
			assertEquals(new Id128(0, 6), env.first.loadCursor());
		}
	}
}
