package Dbh2;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import Zeze.Raft.LogSequence;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.BitConverter;
import Zeze.Util.RocksDatabase;
import org.junit.Test;
import org.rocksdb.RocksDBException;

public class TestRocksDb {
	// 测试RocksDb：key + commit_ts 方式编码，但是能快速定位到最后一个key的能力。
	@Test
	public void testKeyGet() throws RocksDBException {
		var path = Path.of("TestRocksDb");
		LogSequence.deleteDirectory(path.toFile());
		try (var rdb = new RocksDatabase(path.toString(), RocksDatabase.DbType.eOptimisticTransactionDb)) {
			var db = rdb.getOptimisticTransactionDb();
			assert db != null;
			var key = "key".getBytes(StandardCharsets.UTF_8);
			var value = "value".getBytes(StandardCharsets.UTF_8);
			byte[] key1;
			{
				var bb = ByteBuffer.Allocate();
				bb.Append(key);
				bb.WriteLong(1);
				key1 = bb.Copy();
			}
			byte[] key2;
			{
				var bb = ByteBuffer.Allocate();
				bb.Append(key);
				bb.WriteLong(2);
				key2 = bb.Copy();
			}
			byte[] key3;
			{
				var bb = ByteBuffer.Allocate();
				bb.Append(key);
				bb.WriteLong(3);
				key3 = bb.Copy();
			}
			try (var trans = db.beginTransaction(RocksDatabase.getDefaultWriteOptions())) {
				trans.put(key1, value);
				trans.put(key2, value);
				trans.commit();
			}
			try (var it = db.newIterator()) {
				it.seekForPrev(key3);
				while (it.isValid()) {
					System.out.println(BitConverter.toString(it.key()) + "->" + BitConverter.toString(it.value()));
					it.next();
				}
			}
		} finally {
			LogSequence.deleteDirectory(path.toFile());
		}
	}

	public static void main(String[] args) throws RocksDBException {
		var path = Path.of("TestRocksDb");
		LogSequence.deleteDirectory(path.toFile());
		try (var db = new RocksDatabase(path.toString())) {
			var table = db.getOrAddTable("TestTable");
			var key = new byte[24];
			var value = new byte[128];
			var rand = ThreadLocalRandom.current();
			var t = System.nanoTime();
			var batch = db.newBatch2();
			for (int i = 0; i < 10_000_000; ) {
				rand.nextBytes(key);
				table.put(batch, key, 0, key.length, value, 0, value.length);
				if (++i % 10 == 0) {
					batch.commit(RocksDatabase.getDefaultWriteOptions());
					batch.clear();
				}
				if (i % 100_000 == 0) {
					var tt = System.nanoTime();
					System.out.println(i + ": " + (tt - t) / 1_000_000 + " ms, " + (tt - t) / 100_000 + " ns/put");
					t = tt;
				}
			}
		}
	}
}
