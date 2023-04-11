package Zeze.Util;

import java.io.Closeable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Binary;
import Zeze.Transaction.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.rocksdb.BackupEngine;
import org.rocksdb.BackupEngineOptions;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.Env;
import org.rocksdb.OptimisticTransactionDB;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RestoreOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

public class RocksDatabase implements Closeable {
	private static final Logger logger = LogManager.getLogger(RocksDatabase.class);
	private static final Options commonOptions = new Options()
			.setCreateIfMissing(true)
			.setDbWriteBufferSize(64 << 20) // total write buffer bytes, include all the columns
			.setKeepLogFileNum(5); // reserve "LOG.old.*" file count
	private static final DBOptions commonDbOptions = new DBOptions()
			.setCreateIfMissing(true)
			.setDbWriteBufferSize(64 << 20) // total write buffer bytes, include all the columns
			.setKeepLogFileNum(5) // reserve "LOG.old.*" file count
			// .setAtomicFlush(true); // atomic batch 独立于这个选项？
			.setMaxWriteBatchGroupSizeBytes(100 * 1024 * 1024);
	private static final ColumnFamilyOptions defaultCfOptions = new ColumnFamilyOptions();
	private static final ReadOptions defaultReadOptions = new ReadOptions();
	private static final WriteOptions defaultWriteOptions = new WriteOptions();
	private static final WriteOptions syncWriteOptions = new WriteOptions().setSync(true);

	public static @NotNull Options getCommonOptions() {
		return commonOptions;
	}

	public static @NotNull DBOptions getCommonDbOptions() {
		return commonDbOptions;
	}

	public static @NotNull ColumnFamilyOptions getDefaultCfOptions() {
		return defaultCfOptions;
	}

	public static @NotNull ReadOptions getDefaultReadOptions() {
		return defaultReadOptions;
	}

	public static @NotNull WriteOptions getDefaultWriteOptions() {
		return defaultWriteOptions;
	}

	public static @NotNull WriteOptions getSyncWriteOptions() {
		return syncWriteOptions;
	}

	static {
		RocksDB.loadLibrary();
	}

	private final @NotNull String home;
	private final @NotNull RocksDB rocksDb;
	private final ConcurrentHashMap<String, ColumnFamilyHandle> columnFamilies = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Table> tables = new ConcurrentHashMap<>();

	public @NotNull RocksDB getRocksDb() {
		return rocksDb;
	}

	public RocksDatabase(@NotNull String home) throws RocksDBException {
		this.home = home;

		var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
		for (var cf : RocksDB.listColumnFamilies(commonOptions, home))
			columnFamilies.add(new ColumnFamilyDescriptor(cf, defaultCfOptions));
		if (columnFamilies.isEmpty())
			columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(StandardCharsets.UTF_8), defaultCfOptions));

		var outHandles = new ArrayList<ColumnFamilyHandle>();
		rocksDb = open(getCommonDbOptions(), home, columnFamilies, outHandles);

		for (int i = 0; i < columnFamilies.size(); i++) {
			var name = new String(columnFamilies.get(i).getName(), StandardCharsets.UTF_8);
			this.columnFamilies.put(name, outHandles.get(i));
		}
	}

	public static @NotNull RocksDB open(@NotNull DBOptions options, @NotNull String path,
										@NotNull List<ColumnFamilyDescriptor> columnFamilyDescriptors,
										@NotNull List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException {
		logger.info("RocksDB.open: '{}'", path);
		var file = new File(path);
		if (!file.isDirectory()) {
			//noinspection ResultOfMethodCallIgnored
			file.mkdirs();
		}
		for (int i = 0; ; ) {
			try {
				return RocksDB.open(options, path, columnFamilyDescriptors, columnFamilyHandles);
			} catch (RocksDBException e) {
				logger.warn("RocksDB.open failed: '{}'", path, e);
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

	public static @NotNull RocksDB open(@NotNull Options options, @NotNull String path) throws RocksDBException {
		logger.info("RocksDB.open: '{}'", path);
		var file = new File(path);
		if (!file.isDirectory()) {
			//noinspection ResultOfMethodCallIgnored
			file.mkdirs();
		}
		for (int i = 0; ; ) {
			try {
				return RocksDB.open(options, path);
			} catch (RocksDBException e) {
				logger.warn("RocksDB.open failed: '{}'", path, e);
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

	public @NotNull ConcurrentHashMap<String, ColumnFamilyHandle> getColumnFamilies() {
		return columnFamilies;
	}

	public @NotNull String getHome() {
		return home;
	}

	public @NotNull Batch beginBatch() {
		return new Batch();
	}

	public synchronized @NotNull Table openTable(@NotNull String name) {
		return tables.computeIfAbsent(name, _name -> new Table(_name, openFamily(_name)));
	}

	public synchronized boolean dropTable(@NotNull Table table) throws RocksDBException {
		// dropTable 和 openTable 互斥.
		if (tables.remove(table.name, table)) {
			var columnFamily = columnFamilies.remove(table.name);
			if (columnFamily != null) {
				rocksDb.dropColumnFamily(columnFamily);
				rocksDb.destroyColumnFamilyHandle(columnFamily);
				return true;
			}
		}
		return false;
	}

	public @NotNull Batch newBatch() {
		return new Batch();
	}

	private @NotNull ColumnFamilyHandle openFamily(@NotNull String name) {
		return columnFamilies.computeIfAbsent(name, key -> {
			try {
				return rocksDb.createColumnFamily(new ColumnFamilyDescriptor(
						key.getBytes(StandardCharsets.UTF_8), getDefaultCfOptions()));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public void close() {
		columnFamilies.clear();
		tables.clear();
		rocksDb.close();
	}

	public final class Table {
		private final @NotNull String name;
		private final @NotNull ColumnFamilyHandle columnFamily;

		public Table(@NotNull String name, @NotNull ColumnFamilyHandle columnFamily) {
			this.name = name;
			this.columnFamily = columnFamily;
		}

		public @NotNull String getName() {
			return name;
		}

		public @NotNull ColumnFamilyHandle getColumnFamily() {
			return columnFamily;
		}

		public byte[] get(byte[] key) throws RocksDBException {
			return rocksDb.get(columnFamily, defaultReadOptions, key);
		}

		public byte[] get(byte[] key, int offset, int size) throws RocksDBException {
			return rocksDb.get(columnFamily, defaultReadOptions, key, offset, size);
		}

		public byte[] get(@NotNull ReadOptions options, byte[] key) throws RocksDBException {
			return rocksDb.get(columnFamily, options, key);
		}

		public byte[] get(@NotNull ReadOptions options, byte[] key, int offset, int size) throws RocksDBException {
			return rocksDb.get(columnFamily, options, key, offset, size);
		}

		public void put(byte[] key, byte[] value) throws RocksDBException {
			rocksDb.put(columnFamily, defaultWriteOptions, key, value);
		}

		public void put(byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) throws RocksDBException {
			rocksDb.put(columnFamily, defaultWriteOptions, key, keyOff, keyLen, value, valueOff, valueLen);
		}

		public void delete(byte[] key) throws RocksDBException {
			rocksDb.delete(columnFamily, defaultWriteOptions, key);
		}

		public void delete(byte[] key, int keyOff, int keyLen) throws RocksDBException {
			rocksDb.delete(columnFamily, defaultWriteOptions, key, keyOff, keyLen);
		}

		public void put(@NotNull WriteOptions options, byte[] key, byte[] value) throws RocksDBException {
			rocksDb.put(columnFamily, options, key, value);
		}

		public void put(@NotNull WriteOptions options, byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) throws RocksDBException {
			rocksDb.put(columnFamily, options, key, keyOff, keyLen, value, valueOff, valueLen);
		}

		public void delete(@NotNull WriteOptions options, byte[] key) throws RocksDBException {
			rocksDb.delete(columnFamily, options, key);
		}

		public void delete(@NotNull WriteOptions options, byte[] key, int keyOff, int keyLen) throws RocksDBException {
			rocksDb.delete(columnFamily, options, key, keyOff, keyLen);
		}

		public void put(@NotNull Batch batch, @NotNull Binary key, @NotNull Binary value) throws RocksDBException {
			batch.put(columnFamily, key.bytesUnsafe(), key.getOffset(), key.size(),
					value.bytesUnsafe(), value.getOffset(), value.size());
		}

		public void put(@NotNull Batch batch, byte[] key, byte[] value) throws RocksDBException {
			batch.put(columnFamily, key, value);
		}

		public void put(@NotNull Batch batch, byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) throws RocksDBException {
			batch.put(columnFamily, key, keyOff, keyLen, value, valueOff, valueLen);
		}

		public void delete(@NotNull Batch batch, @NotNull Binary key) throws RocksDBException {
			batch.delete(columnFamily, key.bytesUnsafe(), key.getOffset(), key.size());
		}

		public void delete(@NotNull Batch batch, byte[] key) throws RocksDBException {
			batch.delete(columnFamily, key);
		}

		public void delete(@NotNull Batch batch, byte[] key, int keyOff, int keyLen) throws RocksDBException {
			batch.delete(columnFamily, key, keyOff, keyLen);
		}

		public @NotNull RocksIterator iterator() {
			return rocksDb.newIterator(columnFamily, defaultReadOptions);
		}

		// 有数据的时候可以直接删除family吧！
		public void drop() throws RocksDBException {
			dropTable(this);
		}
	}

	public final class Batch implements Closeable {
		private final WriteBatch batch = new WriteBatch();

		public void put(@NotNull ColumnFamilyHandle columnFamily, byte[] key, byte[] value) throws RocksDBException {
			batch.put(columnFamily, key, value);
		}

		public void put(@NotNull ColumnFamilyHandle columnFamily, byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) throws RocksDBException {
			batch.put(columnFamily, Database.copyIf(key, keyOff, keyLen), Database.copyIf(value, valueOff, valueLen));
		}

		public void delete(@NotNull ColumnFamilyHandle columnFamily, byte[] key) throws RocksDBException {
			batch.delete(columnFamily, key);
		}

		public void delete(@NotNull ColumnFamilyHandle columnFamily, byte[] key, int off, int len)
				throws RocksDBException {
			batch.delete(columnFamily, Database.copyIf(key, off, len));
		}

		public void commit() throws RocksDBException {
			rocksDb.write(syncWriteOptions, batch);
		}

		public void commit(@NotNull WriteOptions options) throws RocksDBException {
			rocksDb.write(options, batch);
		}

		@Override
		public void close() {
			batch.close();
		}
	}

	public static ArrayList<ColumnFamilyDescriptor> getColumnFamilies(String dir) throws RocksDBException {
		var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
		if (new File(dir).isDirectory()) {
			for (var cf : OptimisticTransactionDB.listColumnFamilies(RocksDatabase.getCommonOptions(), dir))
				columnFamilies.add(new ColumnFamilyDescriptor(cf, RocksDatabase.getDefaultCfOptions()));
		}
		if (columnFamilies.isEmpty())
			columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(), RocksDatabase.getDefaultCfOptions()));
		return columnFamilies;
	}

	public static void backup(String checkpointDir, String backupDir) throws RocksDBException {
		var outHandles = new ArrayList<ColumnFamilyHandle>();
		try (var src = RocksDB.open(RocksDatabase.getCommonDbOptions(), checkpointDir,
				getColumnFamilies(checkpointDir), outHandles);
			 var backupOptions = new BackupEngineOptions(backupDir);
			 var backup = BackupEngine.open(Env.getDefault(), backupOptions)) {
			backup.createNewBackup(src, true);
		}
	}

	public static void restore(String backupDir, String dbName) throws RocksDBException {
		try (var restoreOptions = new RestoreOptions(false);
			 var backupOptions = new BackupEngineOptions(backupDir);
			 var backup = BackupEngine.open(Env.getDefault(), backupOptions)) {
			backup.restoreDbFromLatestBackup(dbName, dbName, restoreOptions);
		}
	}
}
