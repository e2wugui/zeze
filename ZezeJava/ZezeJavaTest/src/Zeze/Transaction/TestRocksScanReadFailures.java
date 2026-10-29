package Zeze.Transaction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import Zeze.Config;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.rocksdb.FlushOptions;
import org.rocksdb.RocksDBException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestRocksScanReadFailures {

	private static final String TABLE = "scan_failures";

	@TempDir
	Path directory;

	enum Scan {
		SIZE, VALUES, KEYS, VALUES_DESC, KEYS_DESC,
		PAGED_VALUES, PAGED_KEYS, PAGED_VALUES_DESC, PAGED_KEYS_DESC, CLEAR
	}

	private void createCorruptTable() throws Exception {
		byte[] lastValue = null;
		try (var db = new RocksDatabase(directory.toString());
			 var flush = new FlushOptions().setWaitForFlush(true)) {
			var table = db.getOrAddTable(TABLE);
			var random = new Random(731089);
			for (int key = 1; key <= 32; key++) {
				var value = new byte[1024];
				random.nextBytes(value); // Incompressible data spans multiple SST data blocks.
				table.put(new byte[] {(byte)key}, value);
				lastValue = value;
			}
			db.getRocksDb().flush(flush, table.getCfHandle());
		}
		Path sst;
		try (var files = Files.list(directory)) {
			var tables = files.filter(p -> p.toString().endsWith(".sst")).toList();
			assertEquals(1, tables.size());
			sst = tables.getFirst();
		}
		var data = Files.readAllBytes(sst);
		var position = -1;
		outer: for (int i = 0; i <= data.length - 32; i++) {
			for (int j = 0; j < 32; j++) {
				if (data[i + j] != lastValue[j])
					continue outer;
			}
			position = i;
			break;
		}
		assertTrue(position >= 0, "last value must be stored in an uncompressed data block");
		data[position + 16] ^= 1; // Leave the index/footer intact; only the final data block checksum fails.
		Files.write(sst, data);
	}

	private DatabaseRocksDb openDatabase() {
		var conf = new Config.DatabaseConf();
		conf.setDatabaseUrl(directory.toString());
		conf.setDisableOperates(true);
		return new DatabaseRocksDb(null, conf, false);
	}

	@ParameterizedTest
	@EnumSource(Scan.class)
	public void readFailureCannotBeReportedAsSuccessfulCompletion(Scan scan) throws Exception {
		createCorruptTable();
		var db = openDatabase();
		try {
			var table = (Database.AbstractKVTable)db.openTable(TABLE, 1);
			var visited = new AtomicInteger();
			TableWalkHandleRaw values = (key, value) -> { visited.incrementAndGet(); return true; };
			TableWalkKeyRaw keys = key -> { visited.incrementAndGet(); return true; };
			assertThrows(RocksDBException.class, () -> {
				switch (scan) {
				case SIZE -> table.getSize();
				case VALUES -> table.walk(values);
				case KEYS -> table.walkKey(keys);
				case VALUES_DESC -> table.walkDesc(values);
				case KEYS_DESC -> table.walkKeyDesc(keys);
				case PAGED_VALUES -> table.walk(null, 100, values);
				case PAGED_KEYS -> table.walkKey(null, 100, keys);
				case PAGED_VALUES_DESC -> table.walkDesc(null, 100, values);
				case PAGED_KEYS_DESC -> table.walkKeyDesc(null, 100, keys);
				case CLEAR -> table.clear();
				}
			});
			if (scan == Scan.VALUES || scan == Scan.KEYS || scan == Scan.PAGED_VALUES || scan == Scan.PAGED_KEYS)
				assertTrue(visited.get() > 0, "ascending scans must expose a failure after some valid rows");
		} finally {
			db.close();
		}
	}

	@Test
	public void callbackCancellationDoesNotReadTheDamagedRemainder() throws Exception {
		createCorruptTable();
		var db = openDatabase();
		try {
			var table = (Database.AbstractKVTable)db.openTable(TABLE, 1);
			assertEquals(1, table.walk((key, value) -> false));
		} finally {
			db.close();
		}
	}

	@Test
	public void nonpositivePageLimitsDoNotReadTheTable() throws Exception {
		createCorruptTable();
		var db = openDatabase();
		try {
			var table = (Database.AbstractKVTable)db.openTable(TABLE, 1);
			assertNull(table.walk(null, 0, (key, value) -> { throw new AssertionError(); }));
			assertNull(table.walkKeyDesc(null, -1, key -> { throw new AssertionError(); }));
		} finally {
			db.close();
		}
	}
}
