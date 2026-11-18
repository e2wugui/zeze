package Zeze.Transaction;

import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Fast
public class TestRocksMirrorLegacyKeys {
	@TempDir
	Path directory;

	@Test
	void mirrorCanStoreLegacyKeysThatPrimaryStillRejectsForNewWrites() throws Exception {
		var config = new Config.DatabaseConf();
		config.setDatabaseUrl(directory.resolve("mirror").toString());
		var mirror = new DatabaseRocksDb(null, config, true);
		var primaryConfig = new Config.DatabaseConf();
		primaryConfig.setDatabaseUrl(directory.resolve("primary").toString());
		var primary = new DatabaseRocksDb(null, primaryConfig, false);
		try {
			var mirrorTable = (Database.AbstractKVTable)mirror.openTable("records", 0);
			var primaryTable = (Database.AbstractKVTable)primary.openTable("records", 0);
			var key = ByteBuffer.Wrap(new byte[Database.eMaxKeyLength + 1]);
			var value = new byte[]{1, 2};
			try (var transaction = primary.beginTransaction()) {
				assertThrows(IllegalArgumentException.class,
					() -> primaryTable.replace(transaction, key, ByteBuffer.Wrap(value)));
			}
			try (var transaction = mirror.beginTransaction()) {
				mirrorTable.replace(transaction, key, ByteBuffer.Wrap(value));
				transaction.commit();
			}
			var stored = mirrorTable.find(key);
			assertNotNull(stored);
			assertArrayEquals(value, stored.Copy());
			try (var transaction = mirror.beginTransaction()) {
				mirrorTable.remove(transaction, key);
				transaction.commit();
			}
			assertNull(mirrorTable.find(key));
		} finally {
			try {
				primary.close();
			} finally {
				mirror.close();
			}
		}
	}
}
