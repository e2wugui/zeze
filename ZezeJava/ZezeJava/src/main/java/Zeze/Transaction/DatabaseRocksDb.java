package Zeze.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import Zeze.Util.OutObject;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

public class DatabaseRocksDb extends Database {
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

	private final RocksDB rocksDb;
	private final ConcurrentHashMap<String, ColumnFamilyHandle> columnFamilies = new ConcurrentHashMap<>();

	static {
		RocksDB.loadLibrary();
	}

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

	public static RocksDB open(DBOptions options, String path, List<ColumnFamilyDescriptor> columnFamilyDescriptors,
							   List<ColumnFamilyHandle> columnFamilyHandles) throws RocksDBException {
		for (int i = 0; ; ) {
			try {
				//options.setAtomicFlush(true); // atomic batch 独立于这个选项？
				options.setMaxWriteBatchGroupSizeBytes(100 * 1024 * 1024);
				return RocksDB.open(options, path, columnFamilyDescriptors, columnFamilyHandles);
			} catch (RocksDBException e) {
				logger.warn("RocksDB.open {} failed:", path, e);
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

	public DatabaseRocksDb(Application zeze, Config.DatabaseConf conf) {
		super(zeze, conf);
		logger.info("new: {}", getDatabaseUrl());

		var dbHome = getDatabaseUrl().isEmpty() ? "db" : getDatabaseUrl();
		try {
			var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
			for (var cf : RocksDB.listColumnFamilies(commonOptions, dbHome))
				columnFamilies.add(new ColumnFamilyDescriptor(cf, defaultCfOptions));
			if (columnFamilies.isEmpty())
				columnFamilies.add(new ColumnFamilyDescriptor("default".getBytes(StandardCharsets.UTF_8), defaultCfOptions));

			// DirectOperates 依赖 Db，所以只能在这里打开。要不然，放在Open里面更加合理。
			var outHandles = new ArrayList<ColumnFamilyHandle>();
			rocksDb = open(commonDbOptions, dbHome, columnFamilies, outHandles);

			for (int i = 0; i < columnFamilies.size(); i++) {
				var name = new String(columnFamilies.get(i).getName(), StandardCharsets.UTF_8);
				this.columnFamilies.put(name, outHandles.get(i));
			}
			setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesRocksDb());
		} catch (RocksDBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		logger.info("Close: {}", getDatabaseUrl());
		if (rocksDb.isOwningHandle()) {
			try {
				rocksDb.syncWal();
			} catch (RocksDBException ignored) {
			}
		}
		rocksDb.close();
		super.close();
	}

	@Override
	public Transaction beginTransaction() {
		return new RocksDbTrans();
	}

	public static Runnable verifyAction;

	// 多表原子查询。
	public HashMap<String, Map<ByteBuffer, ByteBuffer>> finds(Map<String, Set<ByteBuffer>> tableKeys) {
		if (null == verifyAction)
			throw new RuntimeException("only work with flushAtomicTest=true");

		var result = new HashMap<String, Map<ByteBuffer, ByteBuffer>>(tableKeys.size());
		for (var tks : tableKeys.entrySet())
			result.put(tks.getKey(), new HashMap<>(tks.getValue().size()));

		synchronized (rocksDb) {
			for (var tks : tableKeys.entrySet()) {
				var tableName = tks.getKey();
				var table = getTable(tableName);
				var rocksTable = null != table ? (Database.AbstractKVTable)table.getStorage().getDatabaseTable() : null;
				if (null != rocksTable) {
					for (var key : tks.getValue()) {
						var value = rocksTable.find(key);
						if (null != value) {
							var resultTable = result.computeIfAbsent(tableName, _tname_ -> new HashMap<>());
							resultTable.put(key, value);
						}
					}
				}
			}
		}
		return result;
	}

	private final class RocksDbTrans implements Transaction {
		private WriteBatch batch;

		private WriteBatch getBatch() {
			var wb = batch;
			if (wb == null)
				batch = wb = new WriteBatch();
			return wb;
		}

		void put(byte[] key, byte[] value, ColumnFamilyHandle family) {
			try {
				getBatch().put(family, key, value);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		void remove(byte[] key, ColumnFamilyHandle family) {
			try {
				getBatch().delete(family, key);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void commit() {
			if (batch == null)
				return;

			if (null != verifyAction) {
				synchronized (rocksDb) {
					try {
						rocksDb.write(defaultWriteOptions, batch);
					} catch (RocksDBException e) {
						throw new RuntimeException(e);
					}
				}
			} else {
				try {
					rocksDb.write(defaultWriteOptions, batch);
				} catch (RocksDBException e) {
					throw new RuntimeException(e);
				}

			}
		}

		@Override
		public void rollback() {
		}

		@Override
		public void close() {
			if (batch != null) {
				batch.close();
				batch = null;
			}
		}
	}

	private ColumnFamilyHandle getOrAddFamily(String name, OutObject<Boolean> isNew) {
		if (isNew != null)
			isNew.value = false;

		return columnFamilies.computeIfAbsent(name, key -> {
			try {
				if (isNew != null)
					isNew.value = true;
				return rocksDb.createColumnFamily(
						new ColumnFamilyDescriptor(key.getBytes(StandardCharsets.UTF_8), defaultCfOptions));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public Table openTable(String name) {
		var isNew = new OutObject<Boolean>();
		var cfh = getOrAddFamily(name, isNew);
		return new TableRocksDb(cfh, isNew.value);
	}

	private final class TableRocksDb extends Database.AbstractKVTable {
		private final ColumnFamilyHandle columnFamily;
		private final boolean isNew;

		TableRocksDb(ColumnFamilyHandle columnFamily, boolean isNew) {
			this.columnFamily = columnFamily;
			this.isNew = isNew;
		}

		@Override
		public DatabaseRocksDb getDatabase() {
			return DatabaseRocksDb.this;
		}

		@Override
		public boolean isNew() {
			return isNew;
		}

		@Override
		public void close() {
		}

		@Override
		public ByteBuffer find(ByteBuffer key) {
			try {
				var value = rocksDb.get(columnFamily, defaultReadOptions, key.Bytes, key.ReadIndex, key.Size());
				return value != null ? ByteBuffer.Wrap(value) : null;
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void remove(Transaction txn, ByteBuffer key) {
			((RocksDbTrans)txn).remove(key.Copy(), columnFamily);
		}

		@Override
		public void replace(Transaction txn, ByteBuffer key, ByteBuffer value) {
			((RocksDbTrans)txn).put(key.Copy(), value.Copy(), columnFamily);
		}

		@Override
		public long walk(TableWalkHandleRaw callback) {
			try (var it = rocksDb.newIterator(columnFamily, defaultReadOptions)) {
				long countWalked = 0;
				for (it.seekToFirst(); it.isValid(); it.next()) {
					countWalked++;
					if (!callback.handle(it.key(), it.value()))
						break;
				}
				return countWalked;
			}
		}

		@Override
		public long walkKey(TableWalkKeyRaw callback) {
			try (var it = rocksDb.newIterator(columnFamily, defaultReadOptions)) {
				long countWalked = 0;
				for (it.seekToFirst(); it.isValid(); it.next()) {
					countWalked++;
					if (!callback.handle(it.key()))
						break;
				}
				return countWalked;
			}
		}

		@Override
		public long walkDesc(TableWalkHandleRaw callback) {
			try (var it = rocksDb.newIterator(columnFamily, defaultReadOptions)) {
				long countWalked = 0;
				for (it.seekToLast(); it.isValid(); it.prev()) {
					countWalked++;
					if (!callback.handle(it.key(), it.value()))
						break;
				}
				return countWalked;
			}
		}

		@Override
		public long walkKeyDesc(TableWalkKeyRaw callback) {
			try (var it = rocksDb.newIterator(columnFamily, defaultReadOptions)) {
				long countWalked = 0;
				for (it.seekToLast(); it.isValid(); it.prev()) {
					countWalked++;
					if (!callback.handle(it.key()))
						break;
				}
				return countWalked;
			}
		}

		@Override
		public ByteBuffer walk(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			if (proposeLimit <= 0)
				return null;
			try (var it = rocksDb.newIterator(columnFamily, defaultReadOptions)) {
				if (exclusiveStartKey == null)
					it.seekToFirst();
				else
					it.seek(copyIf(exclusiveStartKey));
				if (!it.isValid())
					return null;

				var lastKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey != null && exclusiveStartKey.equals(lastKey)) { // 第一个item可能为exclusiveStartKey时需要忽略。
					if (!callback.handle(lastKey, it.value()))
						return ByteBuffer.Wrap(lastKey);
					proposeLimit--;
					it.next();
				}
				for (; proposeLimit-- > 0 && it.isValid(); it.next()) {
					lastKey = it.key();
					if (!callback.handle(lastKey, it.value()))
						break;
				}
				return ByteBuffer.Wrap(lastKey);
			}
		}

		@Override
		public ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			if (proposeLimit <= 0)
				return null;
			try (var it = rocksDb.newIterator(columnFamily, defaultReadOptions)) {
				if (exclusiveStartKey == null)
					it.seekToFirst();
				else
					it.seek(copyIf(exclusiveStartKey));
				if (!it.isValid())
					return null;

				var lastKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey != null && exclusiveStartKey.equals(lastKey)) { // 第一个item可能为exclusiveStartKey时需要忽略。
					if (!callback.handle(lastKey))
						return ByteBuffer.Wrap(lastKey);
					proposeLimit--;
					it.next();
				}
				for (; proposeLimit-- > 0 && it.isValid(); it.next()) {
					lastKey = it.key();
					if (!callback.handle(lastKey))
						break;
				}
				return ByteBuffer.Wrap(lastKey);
			}
		}

		@Override
		public ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			if (proposeLimit <= 0)
				return null;
			try (var it = rocksDb.newIterator(columnFamily, defaultReadOptions)) {
				if (exclusiveStartKey == null)
					it.seekToLast();
				else
					it.seek(copyIf(exclusiveStartKey));
				if (!it.isValid())
					return null;

				var lastKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey != null && exclusiveStartKey.equals(lastKey)) { // 第一个item可能为exclusiveStartKey时需要忽略。
					if (!callback.handle(lastKey, it.value()))
						return ByteBuffer.Wrap(lastKey);
					proposeLimit--;
					it.prev();
				}
				for (; proposeLimit-- > 0 && it.isValid(); it.prev()) {
					lastKey = it.key();
					if (!callback.handle(lastKey, it.value()))
						break;
				}
				return ByteBuffer.Wrap(lastKey);
			}
		}

		@Override
		public ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			if (proposeLimit <= 0)
				return null;
			try (var it = rocksDb.newIterator(columnFamily, defaultReadOptions)) {
				if (exclusiveStartKey == null)
					it.seekToLast();
				else
					it.seek(copyIf(exclusiveStartKey));
				if (!it.isValid())
					return null;

				var lastKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey != null && exclusiveStartKey.equals(lastKey)) { // 第一个item可能为exclusiveStartKey时需要忽略。
					if (!callback.handle(lastKey))
						return ByteBuffer.Wrap(lastKey);
					proposeLimit--;
					it.prev();
				}
				for (; proposeLimit-- > 0 && it.isValid(); it.prev()) {
					lastKey = it.key();
					if (!callback.handle(lastKey))
						break;
				}
				return ByteBuffer.Wrap(lastKey);
			}
		}
	}

	private final class OperatesRocksDb implements Operates {
		private final ColumnFamilyHandle columnFamily = getOrAddFamily("zeze.OperatesRocksDb.Schemas", null);

		@Override
		public synchronized DataWithVersion getDataWithVersion(ByteBuffer key) {
			try {
				return DataWithVersion.decode(rocksDb.get(columnFamily, defaultReadOptions, key.Bytes, key.ReadIndex, key.Size()));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public synchronized KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			try {
				var dv = DataWithVersion.decode(rocksDb.get(columnFamily, defaultReadOptions, key.Bytes, key.ReadIndex, key.Size()));
				if (dv.version != version)
					return KV.create(version, false);

				dv.version = ++version;
				dv.data = data;
				var value = ByteBuffer.Allocate();
				dv.encode(value);
				rocksDb.put(columnFamily, defaultWriteOptions,
						key.Bytes, key.ReadIndex, key.size(),
						value.Bytes, value.ReadIndex, value.size());
				return KV.create(version, true);
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void setInUse(int localId, String global) {
			// rocksdb 独占由它自己打开的时候保证。
		}

		@Override
		public int clearInUse(int localId, String global) {
			// rocksdb 独占由它自己打开的时候保证。
			return 0;
		}
	}
}
