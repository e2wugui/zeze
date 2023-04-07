package Zeze.Util;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Transaction.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

public class RocksDatabase {
	private static final Logger logger = LogManager.getLogger(RocksDatabase.class);
	private static final Options commonOptions = new Options()
			.setCreateIfMissing(true)
			.setDbWriteBufferSize(64 << 20) // total write buffer bytes, include all the columns
			.setKeepLogFileNum(5); // reserve "LOG.old.*" file count
	private static final DBOptions commonDbOptions = new DBOptions()
			.setCreateIfMissing(true)
			.setDbWriteBufferSize(64 << 20) // total write buffer bytes, include all the columns
			.setKeepLogFileNum(5); // reserve "LOG.old.*" file count
	private static final ColumnFamilyOptions defaultCfOptions = new ColumnFamilyOptions();
	private static final ReadOptions defaultReadOptions = new ReadOptions();
	private static final WriteOptions defaultWriteOptions = new WriteOptions();
	private static final WriteOptions syncWriteOptions = new WriteOptions().setSync(true);

	public static Options getCommonOptions() {
		return commonOptions;
	}

	public static DBOptions getCommonDbOptions() {
		return commonDbOptions;
	}

	public static ColumnFamilyOptions getDefaultCfOptions() {
		return defaultCfOptions;
	}

	public static ReadOptions getDefaultReadOptions() {
		return defaultReadOptions;
	}

	public static WriteOptions getDefaultWriteOptions() {
		return defaultWriteOptions;
	}

	public static WriteOptions getSyncWriteOptions() {
		return syncWriteOptions;
	}

	static {
		RocksDB.loadLibrary();
	}

	private final String home;
	private final RocksDB rocksDb;
	private final ConcurrentHashMap<String, ColumnFamilyHandle> columnFamilies = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Table> tables = new ConcurrentHashMap<>();

	public RocksDatabase(String home) throws RocksDBException {
		this.home = home;

		var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
		for (var cf : RocksDB.listColumnFamilies(commonOptions, home))
			columnFamilies.add(new ColumnFamilyDescriptor(cf, defaultCfOptions));
		if (columnFamilies.isEmpty())
			columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(StandardCharsets.UTF_8), defaultCfOptions));

		var outHandles = new ArrayList<ColumnFamilyHandle>();
		rocksDb = RocksDatabase.open(RocksDatabase.getCommonDbOptions(), home, columnFamilies, outHandles);

		for (int i = 0; i < columnFamilies.size(); i++) {
			var name = new String(columnFamilies.get(i).getName(), StandardCharsets.UTF_8);
			this.columnFamilies.put(name, outHandles.get(i));
		}
	}

	public ConcurrentHashMap<String, ColumnFamilyHandle> getColumnFamilies() {
		return columnFamilies;
	}

	public String getHome() {
		return home;
	}

	public Batch beginBatch() {
		return new Batch();
	}

	public synchronized Table openTable(String name) {
		return tables.computeIfAbsent(name, _name -> new Table(name, openFamily(name)));
	}

	public synchronized void dropTable(Table table) throws RocksDBException {
		// dropTable 和 openTable 互斥.
		if (null != tables.remove(table.name) && null != columnFamilies.remove(table.name)) {
			rocksDb.dropColumnFamily(table.columnFamily);
			rocksDb.destroyColumnFamilyHandle(table.columnFamily);
		}
	}

	private @NotNull ColumnFamilyHandle openFamily(String name) {
		return columnFamilies.computeIfAbsent(name, key -> {
			try {
				return rocksDb.createColumnFamily(new ColumnFamilyDescriptor(
						key.getBytes(StandardCharsets.UTF_8),
						RocksDatabase.getDefaultCfOptions()));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public void close() {
		for (var table : tables.values())
			table.close();
		rocksDb.close();
	}

	public final class Table {
		private final String name;
		private final ColumnFamilyHandle columnFamily;

		public Table(String name, ColumnFamilyHandle columnFamily) {
			this.name = name;
			this.columnFamily = columnFamily;
		}

		public String getName() {
			return name;
		}

		public ColumnFamilyHandle getColumnFamily() {
			return columnFamily;
		}

		public byte[] get(byte[] key) throws RocksDBException {
			return rocksDb.get(columnFamily, defaultReadOptions, key);
		}

		public byte[] get(byte[] key, int offset, int size) throws RocksDBException {
			return rocksDb.get(columnFamily, defaultReadOptions, key, offset, size);
		}

		public byte[] get(ReadOptions options, byte[] key) throws RocksDBException {
			return rocksDb.get(columnFamily, options, key);
		}

		public byte[] get(ReadOptions options, byte[] key, int offset, int size) throws RocksDBException {
			return rocksDb.get(columnFamily, options, key, offset, size);
		}

		public void put(byte[] key, byte[] value) throws RocksDBException {
			rocksDb.put(columnFamily, defaultWriteOptions, key, value);
		}

		public void put(byte[] key, int keyOff, int keyLen, byte[] value, int valueOff, int valueLen) throws RocksDBException {
			rocksDb.put(columnFamily, defaultWriteOptions, key, keyOff, keyLen, value, valueOff, valueLen);
		}

		public void delete(byte[] key) throws RocksDBException {
			rocksDb.delete(columnFamily, defaultWriteOptions, key);
		}

		public void delete(byte[] key, int keyOff, int keyLen) throws RocksDBException {
			rocksDb.delete(columnFamily, defaultWriteOptions, key, keyOff, keyLen);
		}

		public void put(WriteOptions options, byte[] key, byte[] value) throws RocksDBException {
			rocksDb.put(columnFamily, options, key, value);
		}

		public void put(WriteOptions options, byte[] key, int keyOff, int keyLen, byte[] value, int valueOff, int valueLen) throws RocksDBException {
			rocksDb.put(columnFamily, options, key, keyOff, keyLen, value, valueOff, valueLen);
		}

		public void delete(WriteOptions options, byte[] key) throws RocksDBException {
			rocksDb.delete(columnFamily, options, key);
		}

		public void delete(WriteOptions options, byte[] key, int keyOff, int keyLen) throws RocksDBException {
			rocksDb.delete(columnFamily, options, key, keyOff, keyLen);
		}

		public void put(Batch batch, byte[] key, byte[] value) throws RocksDBException {
			batch.put(columnFamily, key, value);
		}

		public void put(Batch batch, byte[] key, int keyOff, int keyLen, byte[] value, int valueOff, int valueLen) throws RocksDBException {
			batch.put(columnFamily, key, keyOff, keyLen, value, valueOff, valueLen);
		}

		public void delete(Batch batch, byte[] key) throws RocksDBException {
			batch.delete(columnFamily, key);
		}

		public void delete(Batch batch, byte[] key, int keyOff, int keyLen) throws RocksDBException {
			batch.delete(columnFamily, key, keyOff, keyLen);
		}

		public RocksIterator iterator() {
			return rocksDb.newIterator(columnFamily, defaultReadOptions);
		}

		// 有数据的时候可以直接删除family吧！
		public void drop() throws RocksDBException {
			dropTable(this);
		}

		public void close() {
			tables.remove(name);
		}
	}

	public final class Batch implements Closeable {
		private final WriteBatch batch;

		public Batch() {
			batch = new WriteBatch();
		}

		public void put(ColumnFamilyHandle columnFamily, byte[] key, byte[] value) throws RocksDBException {
			batch.put(columnFamily, key, value);
		}

		public void put(ColumnFamilyHandle columnFamily, byte[] key, int keyOff, int keyLen, byte[] value, int valueOff, int valueLen) throws RocksDBException {
			batch.put(columnFamily, Database.copyIf(key, keyOff, keyLen), Database.copyIf(value, valueOff, valueLen));
		}

		public void delete(ColumnFamilyHandle columnFamily, byte[] key) throws RocksDBException {
			batch.delete(columnFamily, key);
		}

		public void delete(ColumnFamilyHandle columnFamily, byte[] key, int off, int len) throws RocksDBException {
			batch.delete(columnFamily, Database.copyIf(key, off, len));
		}

		public void commit() throws RocksDBException {
			rocksDb.write(syncWriteOptions, batch);
		}

		public void commit(WriteOptions options) throws RocksDBException {
			rocksDb.write(options, batch);
		}

		@Override
		public void close() throws IOException {
			batch.close();
		}
	}

	public static RocksDB open(DBOptions options, String path, List<ColumnFamilyDescriptor> columnFamilyDescriptors,
							   List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException {
		logger.info("RocksDB.open: '{}'", path);
		for (int i = 0; ; ) {
			try {
				//options.setAtomicFlush(true); // atomic batch 独立于这个选项？
				options.setMaxWriteBatchGroupSizeBytes(100 * 1024 * 1024);
				return RocksDB.open(options, path, columnFamilyDescriptors, columnFamilyHandles);
			} catch (RocksDBException e) {
				logger.warn("RocksDB.open '{}' failed:", path, e);
				if (++i >= 10)
					throw e;
				try {
					//noinspection BusyWait
					Thread.sleep(1000);
				} catch (InterruptedException ignored) {
					throw e;
				}
			}
		}
	}

}