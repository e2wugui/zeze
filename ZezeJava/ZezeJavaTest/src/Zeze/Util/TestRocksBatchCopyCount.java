package Zeze.Util;

import java.nio.file.Path;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.WriteBatch;
import Zeze.Serialize.ByteBuffer;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Fast
public class TestRocksBatchCopyCount {
	@TempDir
	Path directory;

	@Test
	public void copiedBatchHeaderTracksPendingOperationsAndClear() throws Exception {
		try (var database = new RocksDatabase(directory.resolve("db").toString())) {
			var table = database.getOrAddTable("records");
			var batch = database.newBatch2();
			table.put(batch, new byte[]{1}, 0, 1, new byte[]{2}, 0, 1);
			var first = batch.copy();
			assertEquals(1, ByteBuffer.ToInt(first, 8));
			try (var copied = new WriteBatch(first)) {
				database.getRocksDb().write(RocksDatabase.getDefaultWriteOptions(), copied);
			}
			assertArrayEquals(new byte[]{2}, table.get(new byte[]{1}));
			table.delete(batch, new byte[]{1}, 0, 1);
			assertEquals(2, ByteBuffer.ToInt(batch.copy(), 8));
			try (var copied = new WriteBatch(batch.copy())) {
				database.getRocksDb().write(RocksDatabase.getDefaultWriteOptions(), copied);
			}
			assertNull(table.get(new byte[]{1}));
			assertEquals(1, ByteBuffer.ToInt(first, 8));
			batch.clear();
			assertEquals(12, batch.copy().length);
			assertEquals(0, ByteBuffer.ToInt(batch.copy(), 8));
			try (var copied = new WriteBatch(batch.copy())) {
				database.getRocksDb().write(RocksDatabase.getDefaultWriteOptions(), copied);
			}
			assertNull(table.get(new byte[]{1}));
		}
	}
}
