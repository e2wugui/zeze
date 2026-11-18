package Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import harness.Fast;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Fast
public class TestRocksTableRenameIdentity {
	@TempDir
	Path directory;

	@Test
	void renamingATableToItsOwnNamePreservesItsRowsAfterReopen() throws Exception {
		var config = new Config.DatabaseConf();
		config.setDatabaseType(Config.DbType.RocksDb);
		config.setDatabaseUrl(directory.resolve("database").toString());
		var key = ByteBuffer.Wrap(new byte[]{1});
		var value = new byte[]{2, 3};
		var database = new DatabaseRocksDb(null, config);
		try {
			var table = (Database.AbstractKVTable)database.openTable("values", 0);
			try (var transaction = database.beginTransaction()) {
				table.replace(transaction, key, ByteBuffer.Wrap(value));
				transaction.commit();
			}
			database.renameTable("values", "values");
			var found = table.find(key);
			assertNotNull(found);
			assertArrayEquals(value, found.Copy());
		} finally {
			database.close();
		}
		database = new DatabaseRocksDb(null, config);
		try {
			var table = (Database.AbstractKVTable)database.openTable("values", 0);
			var found = table.find(key);
			assertNotNull(found);
			assertArrayEquals(value, found.Copy());
		} finally {
			database.close();
		}
	}
}
