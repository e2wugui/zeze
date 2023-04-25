package Benchmark;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import Zeze.Application;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.BitConverter;
import Zeze.Util.RocksDatabase;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.LRUCache;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.WriteBatch;

public class BenchRocksDb {
	public static void main1(String[] args) throws Exception {
		System.out.println("begin");
		var dbHome = "~$BenchRocksDb";
		Application.renameAndDeleteDirectory(new File(dbHome));

		long t = System.nanoTime();
		try (var rocksDb = new RocksDatabase(dbHome)) {
			System.out.println("open: " + (System.nanoTime() - t) / 1_000_000 + " ms");
			for (var table : rocksDb.getTableMap().values()) {
				System.out.println("table: " + table.getName() + " => " + table.getCfHandle().getID() + ':'
						+ new String(table.getCfHandle().getName(), StandardCharsets.UTF_8));
			}

			t = System.nanoTime();
			for (int i = 100; i < 200; i++)
				rocksDb.getOrAddTable("table" + i);
			System.out.println("newC: " + (System.nanoTime() - t) / 1_000_000 + " ms");
		}
	}

	public static void main2(String[] args) throws Exception {
		System.out.println("begin");
		RocksDB.loadLibrary();
		var dbHome = "~$BenchRocksDb";
		Application.renameAndDeleteDirectory(new File(dbHome));
		var cache = new LRUCache(8 * 1024 * 1024);
		var cfg = new BlockBasedTableConfig();
		cfg.setBlockCache(cache);
		Options commonOptions = new Options()
				.setCreateIfMissing(true)
				.setDbWriteBufferSize(64 << 20) // total write buffer bytes, include all the columns
				// .setRowCache(cache)
				.setTableFormatConfig(cfg)
				.setKeepLogFileNum(5);
		try (var rocksDb = RocksDB.open(commonOptions, dbHome)) {
			var k = new byte[4];
			var v = new byte[100];
			ThreadLocalRandom.current().nextBytes(v);
			var t = System.nanoTime();
			for (int i = 0; i < 100000; i++) {
				ByteBuffer.intBeHandler.set(k, 0, i);
				rocksDb.put(RocksDatabase.getDefaultWriteOptions(), k, v);
			}
			System.out.println((System.nanoTime() - t) / 1_000_000 + " ms");
			t = System.nanoTime();
			for (int i = 0; i < 100000; i++) {
				ByteBuffer.intBeHandler.set(k, 0, i);
				rocksDb.get(RocksDatabase.getDefaultReadOptions(), k);
			}
			System.out.println((System.nanoTime() - t) / 1_000_000 + " ms");
			var wb = new WriteBatch();
			t = System.nanoTime();
			for (int i = 0; i < 100000; i++) {
				ByteBuffer.intBeHandler.set(k, 0, i);
				wb.clear();
				wb.put(k, v);
				rocksDb.write(RocksDatabase.getDefaultWriteOptions(), wb);
			}
			System.out.println((System.nanoTime() - t) / 1_000_000 + " ms");
			wb.clear();
			t = System.nanoTime();
			for (int i = 0; i < 100000; i++) {
				ByteBuffer.intBeHandler.set(k, 0, i);
				wb.put(k, v);
			}
			rocksDb.write(RocksDatabase.getDefaultWriteOptions(), wb);
			System.out.println((System.nanoTime() - t) / 1_000_000 + " ms");

//			for (int i = 0; i < 256; i++)
//				rocksDb.put(new byte[]{(byte)i}, new byte[i]);
//			for (int i = 0; i < 256; i++)
//				System.out.println(rocksDb.get(new byte[]{(byte)i}).length);
			System.out.println(rocksDb.getProperty("rocksdb.estimate-table-readers-mem"));
			System.out.println(rocksDb.getProperty("rocksdb.cur-size-all-mem-tables"));
			System.out.println(cache.getUsage());
			System.out.println(cache.getPinnedUsage());
		}
		System.out.println("end");
	}

	public static void main(String[] args) throws Exception {
		System.out.println("begin");
		try (var rocksDb = new RocksDatabase("~$BenchRocksDb")) {
			var testTable = rocksDb.getOrAddTable("test");
			var k = new byte[4];
			var v = new byte[100];
			ThreadLocalRandom.current().nextBytes(v);

			var t = System.nanoTime();
			for (int i = 0; i < 100000; i++) {
				ByteBuffer.intBeHandler.set(k, 0, i);
				testTable.put(RocksDatabase.getDefaultWriteOptions(), k, v);
			}
			System.out.println("          put: " + (System.nanoTime() - t) / 1_000_000 + " ms");

			t = System.nanoTime();
			for (int i = 0; i < 100000; i++) {
				ByteBuffer.intBeHandler.set(k, 0, i);
				testTable.get(RocksDatabase.getDefaultReadOptions(), k);
			}
			System.out.println("          get: " + (System.nanoTime() - t) / 1_000_000 + " ms");

			t = System.nanoTime();
			try (var wb = rocksDb.borrowBatch()) {
				for (int i = 0; i < 100000; i++) {
					ByteBuffer.intBeHandler.set(k, 0, i);
					wb.clear();
					testTable.put(wb, k, v);
					wb.commit(RocksDatabase.getDefaultWriteOptions());
				}
			}
			System.out.println("    batch.put: " + (System.nanoTime() - t) / 1_000_000 + " ms");

			byte[] data;
			try (var wb = rocksDb.borrowBatch()) {
				t = System.nanoTime();
				for (int i = 0; i < 100000; i++) {
					ByteBuffer.intBeHandler.set(k, 0, i);
					testTable.put(wb, k, v);
				}
				wb.commit(RocksDatabase.getDefaultWriteOptions());
				System.out.println(" batchAll.put: " + (System.nanoTime() - t) / 1_000_000 + " ms");
				data = wb.getWriteBatch().data();
			}

			t = System.nanoTime();
			var batch = rocksDb.newBatch2();
			for (int i = 0; i < 100000; i++) {
				ByteBuffer.intBeHandler.set(k, 0, i);
				batch.clear();
				testTable.put(batch, k, 0, k.length, v, 0, v.length);
				batch.commit(RocksDatabase.getDefaultWriteOptions());
			}
			System.out.println("   batch2.put: " + (System.nanoTime() - t) / 1_000_000 + " ms");

			batch.clear();
			t = System.nanoTime();
			for (int i = 0; i < 100000; i++) {
				ByteBuffer.intBeHandler.set(k, 0, i);
				testTable.put(batch, k, 0, k.length, v, 0, v.length);
			}
			batch.commit(RocksDatabase.getDefaultWriteOptions());
			System.out.println("batch2All.put: " + (System.nanoTime() - t) / 1_000_000 + " ms");
			byte[] data2 = batch.data();
			System.out.println("data  = " + data.length);
			System.out.println("data2 = " + data2.length);
			ByteBuffer.longLeHandler.set(data, 0, 0); // clear serial number
			System.out.println("check data: " + Arrays.compare(data, data2));
		}
		System.out.println("end");
	}

	public static void main4(String[] args) throws Exception {
		System.out.println("begin");
		RocksDB.loadLibrary();
		var wb = new WriteBatch();
		wb.put("ABC".getBytes(), "1234".getBytes());
		wb.delete("ABC".getBytes());
		System.out.println(wb.getDataSize());
		var d = wb.data();
		System.out.println(BitConverter.toString(d));

		wb = new WriteBatch(d);
		System.out.println(wb.getDataSize());
		d = wb.data();
		System.out.println(BitConverter.toString(d));
		System.out.println("end");
	}
}
