package Zeze.Util;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.RocksDB;
import static org.junit.jupiter.api.Assertions.*;

@Fast
@Timeout(10)
public class TestRocksBatchReturnAfterClose {
	@TempDir
	Path directory;

	@Test
	public void returnedBatchIsDestroyedAfterTheDatabaseHasClosed() throws Exception {
		RocksDB.loadLibrary();
		var database = new RocksDatabase(directory.resolve("db").toString());
		var batch = database.borrowBatch();
		try {
			assertFalse(batch.isClosed());
			database.close();
			assertFalse(batch.isClosed(), "借用者仍持有独立WriteBatch句柄");
			batch.close();
			assertTrue(batch.isClosed(), "迟到归还必须销毁句柄，不得复活已排空的池");
			batch.close();
			assertTrue(batch.isClosed());
			assertThrows(IllegalStateException.class, database::borrowBatch);
			assertThrows(IllegalStateException.class, () -> database.getOrAddTable("late"));
			assertThrows(IllegalStateException.class, () -> database.getOrAddTables(new String[]{"late"}));
			assertThrows(IllegalStateException.class, database::newCheckpoint);
		} finally {
			batch.close();
			database.close();
		}
	}


}
