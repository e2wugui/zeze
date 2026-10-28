package Zeze.Transaction;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.RandomAccessFile;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.FlushOptions;
import org.rocksdb.RocksDBException;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestRocksTableBackup {
	@TempDir
	Path directory;

	private DatabaseRocksDb database() {
		var config = new Config.DatabaseConf();
		config.setDatabaseType(Config.DbType.RocksDb);
		config.setDatabaseUrl(directory.resolve("backup").toString());
		return new DatabaseRocksDb(null, config, false);
	}

	private static RocksDatabase rocks(DatabaseRocksDb database) throws Exception {
		var field = DatabaseRocksDb.class.getDeclaredField("rocksDb");
		field.setAccessible(true);
		return (RocksDatabase)field.get(database);
	}

	@Test
	void failedScanPreservesSource() throws Exception {
		var database = database();
		try {
			var source = rocks(database).getOrAddTable("source");
			source.put(new byte[]{1}, new byte[4096]);
			try (var options = new FlushOptions().setWaitForFlush(true)) {
				rocks(database).getRocksDb().flush(options, source.getCfHandle());
			}
		} finally {
			database.close();
		}
		// Damage the data block while retaining the SST footer/manifest, so open succeeds
		// and the actual backup iterator reports a checksum error instead of normal EOF.
		try (var files = Files.list(directory.resolve("backup"))) {
			var sst = files.filter(path -> path.toString().endsWith(".sst")).findFirst().orElseThrow();
			try (var file = new RandomAccessFile(sst.toFile(), "rw")) {
				var first = file.read();
				file.seek(0);
				file.write(first ^ 0xff);
			}
		}
		database = database();
		try {
			var reopened = database;
			assertThrows(RocksDBException.class, () -> reopened.renameTable("source", "saved"));
			assertNotNull(rocks(database).getTable("source"));
		} finally {
			database.close();
		}
	}

	@Test
	void backsUpEmptyAndMissingTables() throws Exception {
		var database = database();
		try {
			database.renameTable("absent", "unused");
			assertNull(rocks(database).getTable("unused"));
			database.openTable("empty", 0);
			database.renameTable("empty", "saved");
			assertNull(rocks(database).getTable("empty"));
			assertNotNull(rocks(database).getTable("saved"));
		} finally {
			database.close();
		}
	}

	@Test
	void copiesMultipleBatchesBeforeDroppingSource() throws Exception {
		var database = database();
		try {
			var source = (Database.AbstractKVTable)database.openTable("source", 0);
			try (var transaction = database.beginTransaction()) {
				for (int i = 0; i < 10_005; i++) {
					var key = ByteBuffer.Allocate();
					key.WriteInt(i);
					source.replace(transaction, key, key);
				}
				transaction.commit();
			}
			database.renameTable("source", "saved");
			assertNull(rocks(database).getTable("source"));
			var target = (Database.AbstractKVTable)database.openTable("saved", 0);
			assertEquals(10_005, target.walk((key, value) -> {
				assertArrayEquals(key, value);
				return true;
			}));
		} finally {
			database.close();
		}
	}
}
