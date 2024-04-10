package Zeze.Util;

import java.io.Closeable;
import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rocksdb.*;

public class RocksDatabase extends ReentrantLock implements Closeable {
	static {
		RocksDB.loadLibrary();
	}

	private static final Logger logger = LogManager.getLogger(RocksDatabase.class);
	private static final LRUCache dbCache = new LRUCache(Str.parseLongSize(System.getProperty("rocksdbCache"), 64 << 20));
	private static final TableFormatConfig tableCfg = new BlockBasedTableConfig().setBlockCache(dbCache);
	private static final long dbBuffer = Str.parseLongSize(System.getProperty("rocksdbBuffer"), 64 << 20);
	private static final Options commonOptions = new Options()
			.setCreateIfMissing(true)
			.setTableFormatConfig(tableCfg)
			.setDbWriteBufferSize(dbBuffer) // total write buffer bytes, include all the columns
			.setKeepLogFileNum(5); // reserve "LOG.old.*" file count
	private static final DBOptions commonDbOptions = new DBOptions()
			.setCreateIfMissing(true)
			.setDbWriteBufferSize(dbBuffer) // total write buffer bytes, include all the columns
			.setKeepLogFileNum(5) // reserve "LOG.old.*" file count
			// .setAtomicFlush(true); // atomic batch 独立于这个选项？
			.setMaxWriteBatchGroupSizeBytes(100 * 1024 * 1024);
	private static final ColumnFamilyOptions commonCfOptions = new ColumnFamilyOptions()
			.setTableFormatConfig(tableCfg);
	private static final ReadOptions defaultReadOptions = new ReadOptions();
	private static final WriteOptions defaultWriteOptions = new WriteOptions();
	private static final WriteOptions syncWriteOptions = new WriteOptions().setSync(true);
	private static final TransactionDBOptions transactionDbOptions = new TransactionDBOptions();
	private static final @NotNull MethodHandle mhWriteBatchPutCf;
	private static final @NotNull MethodHandle mhWriteBatchDeleteCf;
	private static final @NotNull MethodHandle mhWriteBatchNativeNew;
	private static final @NotNull MethodHandle mhWriteBatchNew;

	static {
		try {
			var lookup = MethodHandles.lookup();
			var clsWriteBatch = WriteBatch.class;
			// native void put(long handle, byte[] key, int keyLen, byte[] value, int valueLen, long cfHandle);
			var m = clsWriteBatch.getDeclaredMethod("put",
					long.class, byte[].class, int.class, byte[].class, int.class, long.class);
			m.setAccessible(true);
			mhWriteBatchPutCf = lookup.unreflect(m);
			// native void delete(long handle, byte[] key, int keyLen, long cfHandle);
			m = clsWriteBatch.getDeclaredMethod("delete", long.class, byte[].class, int.class, long.class);
			m.setAccessible(true);
			mhWriteBatchDeleteCf = lookup.unreflect(m);
			// native static long newWriteBatch(byte[] serialized, int serializedLength)
			m = clsWriteBatch.getDeclaredMethod("newWriteBatch", byte[].class, int.class);
			m.setAccessible(true);
			mhWriteBatchNativeNew = lookup.unreflect(m);
			// WriteBatch(long nativeHandle, boolean owningNativeHandle)
			var c = clsWriteBatch.getDeclaredConstructor(long.class, boolean.class);
			c.setAccessible(true);
			mhWriteBatchNew = lookup.unreflectConstructor(c);
		} catch (ReflectiveOperationException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static @NotNull Options getCommonOptions() {
		return commonOptions;
	}

	public static @NotNull DBOptions getCommonDbOptions() {
		return commonDbOptions;
	}

	public static @NotNull ColumnFamilyOptions getCommonCfOptions() {
		return commonCfOptions;
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

	public enum DbType {
		eRocksDb,
		eOptimisticTransactionDb,
		eTransactionDb,
	}

	private final @NotNull String homePath;
	private final @NotNull RocksDB rocksDb;
	private final @Nullable OptimisticTransactionDB optimisticTransactionDb;
	private final @Nullable TransactionDB transactionDb;

	private final ConcurrentHashMap<String, Table> tableMap = new ConcurrentHashMap<>();
	private @Nullable Map<String, Table> tableMapView;
	private @Nullable ArrayList<Batch> batchPool;

	public RocksDatabase(@NotNull String homePath) throws RocksDBException {
		this(homePath, DbType.eRocksDb);
	}

	public RocksDatabase(@NotNull String homePath, @NotNull DbType dbType) throws RocksDBException {
		this.homePath = homePath;
		var cfds = getCfDescriptors(homePath);
		int cfCount = cfds.size();
		var cfhs = new ArrayList<ColumnFamilyHandle>(cfCount);
		rocksDb = open(dbType, commonDbOptions, homePath, cfds, cfhs);
		optimisticTransactionDb = dbType == DbType.eOptimisticTransactionDb ? (OptimisticTransactionDB)rocksDb : null;
		transactionDb = dbType == DbType.eTransactionDb ? (TransactionDB)rocksDb : null;
		for (int i = 0; i < cfCount; i++) {
			var name = new String(cfds.get(i).getName(), StandardCharsets.UTF_8);
			tableMap.put(name, new Table(name, cfhs.get(i)));
		}
	}

	public static @NotNull ArrayList<ColumnFamilyDescriptor> getCfDescriptors(@NotNull String homePath)
			throws RocksDBException {
		var cfds = new ArrayList<ColumnFamilyDescriptor>();
		for (byte[] cfn : RocksDB.listColumnFamilies(commonOptions, homePath))
			cfds.add(new ColumnFamilyDescriptor(cfn, commonCfOptions));
		if (cfds.isEmpty())
			cfds.add(new ColumnFamilyDescriptor("default".getBytes(StandardCharsets.UTF_8), commonCfOptions));
		return cfds;
	}

	public @NotNull Transaction beginOptimisticTransaction() {
		//noinspection DataFlowIssue
		return optimisticTransactionDb.beginTransaction(getDefaultWriteOptions());
	}

	public @NotNull Transaction beginTransaction() {
		//noinspection DataFlowIssue
		return transactionDb.beginTransaction(getDefaultWriteOptions());
	}

	// RocksDB用完时需确保调用close回收堆外内存
	public static @NotNull RocksDB open(@NotNull DBOptions options, @NotNull String path,
										@NotNull List<ColumnFamilyDescriptor> cfds,
										@NotNull List<ColumnFamilyHandle> cfhs) throws RocksDBException {
		return open(DbType.eRocksDb, options, path, cfds, cfhs);
	}

	private static @NotNull RocksDB realOpen(@NotNull DbType dbType, @NotNull DBOptions options, @NotNull String path,
											 @NotNull List<ColumnFamilyDescriptor> cfds,
											 @NotNull List<ColumnFamilyHandle> cfhs) throws RocksDBException {
		switch (dbType) {
		case eRocksDb:
			return RocksDB.open(options, path, cfds, cfhs);
		case eOptimisticTransactionDb:
			return OptimisticTransactionDB.open(options, path, cfds, cfhs);
		case eTransactionDb:
			return TransactionDB.open(options, transactionDbOptions, path, cfds, cfhs);
		default:
			throw new UnsupportedOperationException("unknown dbType=" + dbType);
		}
	}

	public static @NotNull RocksDB open(@NotNull DbType dbType, @NotNull DBOptions options, @NotNull String path,
										@NotNull List<ColumnFamilyDescriptor> cfds,
										@NotNull List<ColumnFamilyHandle> cfhs) throws RocksDBException {
		logger.info("RocksDB.open: '{}'", path);
		var file = new File(path);
		if (!file.isDirectory()) {
			//noinspection ResultOfMethodCallIgnored
			file.mkdirs();
		}
		for (int i = 0; ; ) {
			try {
				var rocksDb = realOpen(dbType, options, path, cfds, cfhs);
				if (cfds.size() != cfhs.size())
					throw new IllegalStateException("RocksDB.open unmatched: " + cfds.size() + " != " + cfhs.size());
				return rocksDb;
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

	// RocksDB用完时需确保调用close回收堆外内存
	public static @NotNull RocksDB open(@NotNull String path) throws RocksDBException {
		return open(commonOptions, path);
	}

	// RocksDB用完时需确保调用close回收堆外内存
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

	public @NotNull String getHomePath() {
		return homePath;
	}

	public @NotNull RocksDB getRocksDb() {
		return rocksDb;
	}

	public @Nullable OptimisticTransactionDB getOptimisticTransactionDb() {
		return optimisticTransactionDb;
	}

	public @NotNull Map<String, Table> getTableMap() {
		var view = tableMapView;
		if (view == null)
			tableMapView = view = Collections.unmodifiableMap(tableMap);
		return view;
	}

	public @Nullable Table getTable(@NotNull String name) {
		return tableMap.get(name);
	}

	public @NotNull Table getOrAddTable(@NotNull String name) throws RocksDBException {
		return getOrAddTable(name, null);
	}

	public @NotNull Table getOrAddTable(@NotNull String name, @Nullable OutObject<Boolean> isNew)
			throws RocksDBException {
		lock();
		try {
			var table = tableMap.get(name);
			if (table != null) {
				if (isNew != null)
					isNew.value = false;
				return table;
			}
			if (isNew != null)
				isNew.value = true;
			table = new Table(name, rocksDb.createColumnFamily(
					new ColumnFamilyDescriptor(name.getBytes(StandardCharsets.UTF_8), commonCfOptions)));
			tableMap.put(name, table);
			return table;
		} finally {
			unlock();
		}
	}

	public @NotNull Table @NotNull [] getOrAddTables(String @NotNull [] names) throws RocksDBException {
		return getOrAddTables(names, null);
	}

	public @NotNull Table @NotNull [] getOrAddTables(String @NotNull [] names,
																  boolean @Nullable [] isNews) throws RocksDBException {
		lock();
		try {
			var n = names.length;
			var tables = new Table[n];
			var newIndexes = new IntList();
			var newNames = new ArrayList<byte[]>();
			for (int i = 0; i < n; i++) {
				var table = tableMap.get(names[i]);
				if (table != null)
					tables[i] = table;
				else {
					newIndexes.add(i);
					newNames.add(names[i].getBytes(StandardCharsets.UTF_8));
				}
				if (isNews != null && i < isNews.length)
					isNews[i] = table == null;
			}
			n = newIndexes.size();
			if (n > 0) {
				var cfhs = rocksDb.createColumnFamilies(commonCfOptions, newNames);
				if (cfhs.size() != newNames.size()) {
					throw new IllegalStateException("createColumnFamilies unmatched: "
							+ cfhs.size() + " != " + newNames.size());
				}
				for (int i = 0; i < n; i++) {
					var idx = newIndexes.get(i);
					var table = new Table(names[idx], cfhs.get(i));
					tableMap.put(table.name, table);
					tables[idx] = table;
				}
			}
			return tables;
		} finally {
			unlock();
		}
	}

	public boolean dropTable(@NotNull String name) throws RocksDBException {
		lock();
		try {
			var table = tableMap.remove(name);
			if (table == null)
				return false;
			var cfh = table.getCfHandle();
			rocksDb.dropColumnFamily(cfh);
			rocksDb.destroyColumnFamilyHandle(cfh);
			return true;
		} finally {
			unlock();
		}
	}

	public int dropTables(String @NotNull [] names) throws RocksDBException {
		lock();
		try {
			var cfhs = new ArrayList<ColumnFamilyHandle>();
			for (var name : names) {
				var table = tableMap.remove(name);
				if (table != null)
					cfhs.add(table.getCfHandle());
			}
			if (cfhs.isEmpty())
				return 0;
			rocksDb.dropColumnFamilies(cfhs);
			for (var cfh : cfhs)
				rocksDb.destroyColumnFamilyHandle(cfh);
			return cfhs.size();
		} finally {
			unlock();
		}
	}

	// Batch用完时需确保调用close回收堆外内存,推荐使用try(var b = newBatch()) {...}
	public @NotNull Batch newBatch() {
		return new Batch();
	}

	// Batch用完时需确保调用close归还pool,推荐使用try(var b = borrowBatch()) {...}
	public @NotNull Batch borrowBatch() {
		lock();
		try {
			var bp = batchPool;
			if (bp == null)
				batchPool = bp = new ArrayList<>();
			int n = bp.size();
			if (n > 0)
				return bp.remove(n - 1);
		} finally {
			unlock();
		}
		return new Batch() {
			@Override
			public void close() {
				RocksDatabase.this.lock();
				try {
					clear();
					batchPool.add(this);
				} finally {
					RocksDatabase.this.unlock();
				}
			}
		};
	}

	public @NotNull Batch2 newBatch2() {
		return new Batch2();
	}

	public @NotNull Batch2 newBatch2(int capacity) {
		return new Batch2(capacity);
	}

	// Checkpoint用完时需确保调用close回收堆外内存,推荐使用try(var c = newCheckpoint()) {...}
	public @NotNull Checkpoint newCheckpoint() {
		return Checkpoint.create(rocksDb);
	}

	public boolean isClosed() {
		return !rocksDb.isOwningHandle();
	}

	@Override
	public void close() {
		lock();
		try {
			var bp = batchPool;
			if (bp != null) {
				for (var b : bp)
					b.batch.close();
				bp.clear();
			}
			tableMap.clear();
			rocksDb.close();
		} finally {
			unlock();
		}
	}

	public static void backup(@NotNull String checkpointDir, @NotNull String backupDir) throws RocksDBException {
		backup(DbType.eRocksDb, checkpointDir, backupDir);
	}

	public static void backup(@NotNull DbType dbType, @NotNull String checkpointDir, @NotNull String backupDir)
			throws RocksDBException {
		var cfhs = new ArrayList<ColumnFamilyHandle>();
		try (var src = realOpen(dbType, commonDbOptions, checkpointDir, getCfDescriptors(checkpointDir), cfhs);
			 var backupOptions = new BackupEngineOptions(backupDir);
			 var backup = BackupEngine.open(Env.getDefault(), backupOptions)) {
			backup.createNewBackup(src, true);
		}
	}

	public static void restore(@NotNull String backupDir, @NotNull String dbName) throws RocksDBException {
		try (var restoreOptions = new RestoreOptions(false);
			 var backupOptions = new BackupEngineOptions(backupDir);
			 var backup = BackupEngine.open(Env.getDefault(), backupOptions)) {
			backup.restoreDbFromLatestBackup(dbName, dbName, restoreOptions);
		}
	}

	private static void writeVarInt(@NotNull ByteBuffer bb, int i) {
		long v = i & 0xffff_ffffL;
		if (v < (1 << 7))
			bb.WriteByte((byte)v);
		else if (v < (1 << 14)) {
			bb.EnsureWrite(2);
			var bytes = bb.Bytes;
			var wi = bb.WriteIndex;
			bytes[wi] = (byte)(v | 0x80);
			bytes[wi + 1] = (byte)(v >> 7);
			bb.WriteIndex = wi + 2;
		} else if (v < (1 << 21)) {
			bb.EnsureWrite(3);
			var bytes = bb.Bytes;
			var wi = bb.WriteIndex;
			bytes[wi] = (byte)(v | 0x80);
			bytes[wi + 1] = (byte)((v >> 7) | 0x80);
			bytes[wi + 2] = (byte)(v >> 14);
			bb.WriteIndex = wi + 3;
		} else if (v < (1 << 28)) {
			bb.EnsureWrite(4);
			var bytes = bb.Bytes;
			var wi = bb.WriteIndex;
			bytes[wi] = (byte)(v | 0x80);
			bytes[wi + 1] = (byte)((v >> 7) | 0x80);
			bytes[wi + 2] = (byte)((v >> 14) | 0x80);
			bytes[wi + 3] = (byte)(v >> 21);
			bb.WriteIndex = wi + 4;
		} else {
			bb.EnsureWrite(5);
			var bytes = bb.Bytes;
			var wi = bb.WriteIndex;
			bytes[wi] = (byte)(v | 0x80);
			bytes[wi + 1] = (byte)((v >> 7) | 0x80);
			bytes[wi + 2] = (byte)((v >> 14) | 0x80);
			bytes[wi + 3] = (byte)((v >> 21) | 0x80);
			bytes[wi + 4] = (byte)(v >> 28);
			bb.WriteIndex = wi + 5;
		}
	}

	public final class Table {
		private final @NotNull String name;
		private final @NotNull ColumnFamilyHandle cfHandle;
		private final int id;

		public Table(@NotNull String name, @NotNull ColumnFamilyHandle cfHandle) {
			this.name = name;
			this.cfHandle = cfHandle;
			id = cfHandle.getID();
		}

		public long getKeyNumbers() throws RocksDBException {
			return rocksDb.getLongProperty(cfHandle, "rocksdb.estimate-num-keys");
		}

		public @NotNull String getName() {
			return name;
		}

		public @NotNull ColumnFamilyHandle getCfHandle() {
			return cfHandle;
		}

		public int getId() {
			return id;
		}

		public byte @Nullable [] get(byte[] key) throws RocksDBException {
			return get(defaultReadOptions, key);
		}

		public byte @Nullable [] get(byte[] key, int offset, int size) throws RocksDBException {
			return get(defaultReadOptions, key, offset, size);
		}

		public byte @Nullable [] get(@NotNull ReadOptions options, byte[] key) throws RocksDBException {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var r = rocksDb.get(cfHandle, options, key);
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addRunInfo("RocksDB.get", System.nanoTime() - timeBegin);
			return r;
		}

		public byte @Nullable [] get(@NotNull ReadOptions options, byte[] key, int offset, int size)
				throws RocksDBException {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			var r = rocksDb.get(cfHandle, options, key, offset, size);
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addRunInfo("RocksDB.get", System.nanoTime() - timeBegin);
			return r;
		}

		public void put(byte[] key, byte[] value) throws RocksDBException {
			put(defaultWriteOptions, key, value);
		}

		public void put(byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) throws RocksDBException {
			put(defaultWriteOptions, key, keyOff, keyLen, value, valueOff, valueLen);
		}

		public void put(@NotNull WriteOptions options, byte[] key, byte[] value) throws RocksDBException {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			rocksDb.put(cfHandle, options, key, value);
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addRunInfo("RocksDB.put", System.nanoTime() - timeBegin);
		}

		public void put(@NotNull WriteOptions options, byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) throws RocksDBException {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			rocksDb.put(cfHandle, options, key, keyOff, keyLen, value, valueOff, valueLen);
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addRunInfo("RocksDB.put", System.nanoTime() - timeBegin);
		}

		public void delete(byte[] key) throws RocksDBException {
			delete(defaultWriteOptions, key);
		}

		public void delete(byte[] key, int keyOff, int keyLen) throws RocksDBException {
			delete(defaultWriteOptions, key, keyOff, keyLen);
		}

		public void delete(@NotNull WriteOptions options, byte[] key) throws RocksDBException {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			rocksDb.delete(cfHandle, options, key);
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addRunInfo("RocksDB.delete", System.nanoTime() - timeBegin);
		}

		public void delete(@NotNull WriteOptions options, byte[] key, int keyOff, int keyLen) throws RocksDBException {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			rocksDb.delete(cfHandle, options, key, keyOff, keyLen);
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addRunInfo("RocksDB.delete", System.nanoTime() - timeBegin);
		}

		public void deleteRange(byte[] first, byte[] last) throws RocksDBException {
			deleteRange(defaultWriteOptions, first, last);
		}

		public void deleteRange(@NotNull WriteOptions options, byte[] first, byte[] last) throws RocksDBException {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			rocksDb.deleteRange(cfHandle, options, first, last);
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addRunInfo("RocksDB.deleteRange", System.nanoTime() - timeBegin);
		}

		public void put(@NotNull Transaction t, @NotNull Binary key, @NotNull Binary value) throws RocksDBException {
			put(t, key.bytesUnsafe(), key.getOffset(), key.size(),
					value.bytesUnsafe(), value.getOffset(), value.size());
		}

		public void put(@NotNull Transaction t, byte[] key, byte[] value) throws RocksDBException {
			put(t, key, 0, key.length, value, 0, value.length);
		}

		public void put(@NotNull Transaction t, byte[] key, int keyLen, byte[] value, int valueLen)
				throws RocksDBException {
			put(t, key, 0, keyLen, value, 0, valueLen);
		}

		public void put(@NotNull Transaction t, byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) throws RocksDBException {
			// batch 优化成内部方法调用了？仅在keyOff不等于0时拷贝！！！
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			key = Database.copyIf(key, keyOff, keyLen);
			value = Database.copyIf(value, valueOff, valueLen);
			t.put(cfHandle, key, value);
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addRunInfo("RocksDBTxn.put", System.nanoTime() - timeBegin);
		}

		public void delete(@NotNull Transaction t, @NotNull Binary key) throws RocksDBException {
			delete(t, key.bytesUnsafe(), key.getOffset(), key.size());
		}

		public void delete(@NotNull Transaction t, byte[] key) throws RocksDBException {
			delete(t, key, 0, key.length);
		}

		public void delete(@NotNull Transaction t, byte[] key, int keyLen) throws RocksDBException {
			delete(t, key, 0, keyLen);
		}

		public void delete(@NotNull Transaction t, byte[] key, int keyOff, int keyLen) throws RocksDBException {
			// batch 优化成内部方法调用了？仅在keyOff不等于0时拷贝！！！
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			key = Database.copyIf(key, keyOff, keyLen);
			t.delete(cfHandle, key);
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addRunInfo("RocksDBTxn.delete", System.nanoTime() - timeBegin);
		}

		public void put(@NotNull Batch batch, @NotNull Binary key, @NotNull Binary value) throws RocksDBException {
			put(batch, key.bytesUnsafe(), key.getOffset(), key.size(),
					value.bytesUnsafe(), value.getOffset(), value.size());
		}

		public void put(@NotNull Batch batch, byte[] key, byte[] value) throws RocksDBException {
			batch.put(cfHandle, key, value);
		}

		public void put(@NotNull Batch batch, byte[] key, int keyLen, byte[] value, int valueLen)
				throws RocksDBException {
			batch.put(cfHandle, key, keyLen, value, valueLen);
		}

		public void put(@NotNull Batch batch, byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) throws RocksDBException {
			if (keyOff != 0)
				key = Arrays.copyOfRange(key, keyOff, keyOff + keyLen);
			if (valueOff != 0)
				value = Arrays.copyOfRange(value, valueOff, valueOff + valueLen);
			batch.put(cfHandle, key, keyLen, value, valueLen);
		}

		public void delete(@NotNull Batch batch, @NotNull Binary key) throws RocksDBException {
			delete(batch, key.bytesUnsafe(), key.getOffset(), key.size());
		}

		public void delete(@NotNull Batch batch, byte[] key) throws RocksDBException {
			batch.delete(cfHandle, key);
		}

		public void delete(@NotNull Batch batch, byte[] key, int keyLen) throws RocksDBException {
			batch.delete(cfHandle, key, keyLen);
		}

		public void delete(@NotNull Batch batch, byte[] key, int keyOff, int keyLen) throws RocksDBException {
			if (keyOff == 0)
				batch.delete(cfHandle, key, keyLen);
			else
				batch.delete(cfHandle, Arrays.copyOfRange(key, keyOff, keyOff + keyLen));
		}

		public void put(@NotNull Batch2 batch, byte[] key, int keyOff, int keyLen,
						byte[] value, int valueOff, int valueLen) {
			var bb = batch.bb;
			if (id == 0)
				bb.WriteByte(1); // kTypeValue
			else {
				bb.WriteByte(5); // kTypeColumnFamilyValue
				writeVarInt(bb, id);
			}
			writeVarInt(bb, keyLen);
			bb.Append(key, keyOff, keyLen);
			writeVarInt(bb, valueLen);
			bb.Append(value, valueOff, valueLen);
			batch.count++;
		}

		public void delete(@NotNull Batch2 batch, byte[] key, int keyOff, int keyLen) {
			var bb = batch.bb;
			if (id == 0)
				bb.WriteByte(0); // kTypeDeletion
			else {
				bb.WriteByte(4); // kTypeColumnFamilyDeletion
				writeVarInt(bb, id);
			}
			writeVarInt(bb, keyLen);
			bb.Append(key, keyOff, keyLen);
			batch.count++;
		}

		// RocksIterator用完时需确保调用close回收堆外内存,推荐使用try(var it = iterator()) {...}
		public @NotNull RocksIterator iterator() {
			return rocksDb.newIterator(cfHandle, defaultReadOptions);
		}

		// 有数据的时候可以直接删除family吧！
		public void drop() throws RocksDBException {
			dropTable(name);
		}

		public void compact() throws RocksDBException {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			rocksDb.compactRange(cfHandle);
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addRunInfo("RocksDB.compact", System.nanoTime() - timeBegin);
		}

		public void clear() throws RocksDBException {
			deleteToEnd(iterator());
		}

		public void deleteToEnd(RocksIterator it) throws RocksDBException {
			if (it.isValid()) {
				var first = it.key();
				it.seekToLast();
				if (it.isValid()) {
					var last = it.key();
					// deleteRange 不包含last，需要制造一个比当前last后面的key。
					last = Arrays.copyOf(last, last.length + 1);
					deleteRange(first, last);
				}
			}
		}

	}

	public class Batch implements Closeable {
		private final WriteBatch batch = new WriteBatch();

		public @NotNull WriteBatch getWriteBatch() {
			return batch;
		}

		public int getCount() {
			return batch.count();
		}

		public boolean isClosed() {
			return !batch.isOwningHandle();
		}

		public void put(@NotNull ColumnFamilyHandle cfh, byte[] key, byte[] value) throws RocksDBException {
			batch.put(cfh, key, value);
		}

		public void put(@NotNull ColumnFamilyHandle cfh, byte[] key, int keyLen, byte[] value, int valueLen)
				throws RocksDBException {
			try {
				mhWriteBatchPutCf.invokeExact(batch, batch.getNativeHandle(), key, keyLen, value, valueLen,
						cfh.getNativeHandle());
			} catch (Throwable e) {
				Task.forceThrow(e);
			}
		}

		public void delete(@NotNull ColumnFamilyHandle cfh, byte[] key) throws RocksDBException {
			batch.delete(cfh, key);
		}

		public void delete(@NotNull ColumnFamilyHandle cfh, byte[] key, int keyLen) throws RocksDBException {
			try {
				mhWriteBatchDeleteCf.invokeExact(batch, batch.getNativeHandle(), key, keyLen, cfh.getNativeHandle());
			} catch (Throwable e) {
				Task.forceThrow(e);
			}
		}

		public void commit() throws RocksDBException {
			commit(syncWriteOptions);
		}

		public void commit(@NotNull WriteOptions options) throws RocksDBException {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			rocksDb.write(options, batch);
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addRunInfo("RocksDB.write", System.nanoTime() - timeBegin);
		}

		// clear后可以再次put,delete,commit. 复用Batch性能更高
		public void clear() {
			batch.clear();
		}

		@Override
		public void close() {
			batch.close();
		}
	}

	// 在JVM堆内收集批量操作,只在提交时调用native方法,减少native调用的次数,批量多时性能会提高一些,但会多占用一些JVM堆
	// 这里不提供put和delete方法,只能在Table内调用
	public class Batch2 {
		private final ByteBuffer bb;
		private int count;

		public Batch2() {
			this(32);
		}

		public Batch2(int capacity) {
			bb = ByteBuffer.Allocate(capacity);
			bb.EnsureWrite(12);
			bb.WriteIndex = 12;
		}

		public void commit() throws RocksDBException {
			commit(syncWriteOptions);
		}

		public void commit(@NotNull WriteOptions options) throws RocksDBException {
			var timeBegin = PerfCounter.ENABLE_PERF ? System.nanoTime() : 0;
			ByteBuffer.intLeHandler.set(bb.Bytes, 8, count);
			try (var wb = (WriteBatch)mhWriteBatchNew.invokeExact(
					(long)mhWriteBatchNativeNew.invokeExact(bb.Bytes, bb.WriteIndex), true)) {
				rocksDb.write(options, wb);
			} catch (Throwable e) {
				Task.forceThrow(e);
			}
			if (PerfCounter.ENABLE_PERF)
				PerfCounter.instance.addRunInfo("RocksDB.write", System.nanoTime() - timeBegin);
		}

		public byte[] copy() {
			return Arrays.copyOf(bb.Bytes, bb.WriteIndex);
		}

		// clear后可以再次put,delete,commit. 复用Batch性能更高
		public void clear() {
			bb.WriteIndex = 12;
			count = 0;
		}
	}
}
