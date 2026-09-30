package Zeze.History;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database;
import Zeze.Transaction.Table;
import Zeze.Util.KV;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Fast
public class TestHistoryOwnerConcurrentClaim {

	private static Application newApp(String url, String owner) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setNoDatabase(true); // exercise only the owner protocol, without cache directories or GCM
		conf.setDefaultTableConf(new Config.TableConf());
		conf.setHistory(owner);
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl(url);
		conf.getDatabaseConfMap().put("", dbConf);
		var app = new Application(owner, conf);
		var module = new HistoryModule(app);
		var moduleField = Application.class.getDeclaredField("historyModule");
		moduleField.setAccessible(true);
		moduleField.set(app, module);
		// Table.open normally binds this field; the protocol only needs the real shared database.
		var databaseField = Table.class.getDeclaredField("database");
		databaseField.setAccessible(true);
		databaseField.set(module.getHistoryTable(), app.getDatabase(""));
		return app;
	}

	private static final class PausedInitialRead implements Database.Operates {
		private final Database.Operates delegate;
		private final CountDownLatch observedEmpty = new CountDownLatch(1);
		private final CountDownLatch resume = new CountDownLatch(1);
		private boolean initial = true;

		private PausedInitialRead(Database.Operates delegate) {
			this.delegate = delegate;
		}

		@Override
		public Database.DataWithVersion getDataWithVersion(ByteBuffer key) {
			var observed = delegate.getDataWithVersion(key);
			if (initial) {
				initial = false;
				observedEmpty.countDown();
				try {
					if (!resume.await(10, TimeUnit.SECONDS))
						throw new AssertionError("competing claim was not resumed");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new AssertionError(e);
				}
			}
			return observed;
		}

		@Override
		public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			return delegate.saveDataWithSameVersion(key, data, version);
		}

		@Override
		public void setInUse(int localId, String global) {
			delegate.setInUse(localId, global);
		}

		@Override
		public int clearInUse(int localId, String global) {
			return delegate.clearInUse(localId, global);
		}
	}

	@Test
	public void completedClaimRejectsCompetingEmptyRead(@TempDir Path tempDir) throws Exception {
		checkClaim(tempDir, false);
	}

	@Test
	public void existingSameOwnerRejectsCompetingEmptyRead(@TempDir Path tempDir) throws Exception {
		checkClaim(tempDir, true);
	}

	private static void checkClaim(Path tempDir, boolean existingOwner) throws Exception {
		// Both applications deliberately share the real Memory URL bucket; this tests the
		// DirectOperates CAS protocol and does not claim to exercise cross-Application GCM.
		var first = newApp(tempDir.toString(), "firstOwner");
		var second = newApp(tempDir.toString(), "secondOwner");
		var delayed = new PausedInitialRead(second.getDatabase("").getDirectOperates());
		var operationsField = Database.class.getDeclaredField("directOperates");
		operationsField.setAccessible(true);
		operationsField.set(second.getDatabase(""), delayed);
		CompletableFuture<Throwable> competing = null;
		try {
			competing = CompletableFuture.supplyAsync(() -> {
				try {
					OwnerCheck.verify(second);
					return null;
				} catch (Throwable e) {
					return e;
				}
			});
			Assertions.assertTrue(delayed.observedEmpty.await(5, TimeUnit.SECONDS));
			if (existingOwner) {
				// A same-name verifier of a pre-existing version-zero marker must also
				// stabilize it before allowing this stale empty reader to continue.
				var key = ByteBuffer.Allocate();
				key.WriteString(OwnerCheck.OWNER_KEY);
				var owner = ByteBuffer.Allocate();
				owner.WriteString(first.getConfig().getHistory());
				first.getDatabase("").getDirectOperates().saveDataWithSameVersion(key, owner, 0);
			}
			OwnerCheck.verify(first); // a successful startup must make this owner stable
			delayed.resume.countDown();
			Assertions.assertInstanceOf(IllegalStateException.class, competing.get(5, TimeUnit.SECONDS),
					"an earlier empty read must not overwrite an owner whose verification already succeeded");
			Assertions.assertDoesNotThrow(() -> OwnerCheck.verify(first));
		} finally {
			delayed.resume.countDown();
			if (competing != null)
				competing.get(10, TimeUnit.SECONDS);
			second.stop();
			first.stop();
		}
	}
}
