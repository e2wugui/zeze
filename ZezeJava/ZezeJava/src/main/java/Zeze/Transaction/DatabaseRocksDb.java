package Zeze.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.IntList;
import Zeze.Util.KV;
import Zeze.Util.OutObject;
import Zeze.Util.RocksDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;

public class DatabaseRocksDb extends Database {
	private final @NotNull RocksDB rocksDb;
	private final HashMap<String, ColumnFamilyHandle> columnFamilies = new HashMap<>();

	public DatabaseRocksDb(@NotNull Application zeze, @NotNull Config.DatabaseConf conf) {
		super(zeze, conf);

		var dbHome = getDatabaseUrl().isEmpty() ? "db" : getDatabaseUrl();
		try {
			var columnFamilies = new ArrayList<ColumnFamilyDescriptor>();
			for (var cf : RocksDB.listColumnFamilies(RocksDatabase.getCommonOptions(), dbHome))
				columnFamilies.add(new ColumnFamilyDescriptor(cf, RocksDatabase.getDefaultCfOptions()));
			if (columnFamilies.isEmpty()) {
				columnFamilies.add(new ColumnFamilyDescriptor(
						"default".getBytes(StandardCharsets.UTF_8), RocksDatabase.getDefaultCfOptions()));
			}

			// DirectOperates 依赖 Db，所以只能在这里打开。要不然，放在Open里面更加合理。
			var outHandles = new ArrayList<ColumnFamilyHandle>();
			rocksDb = RocksDatabase.open(RocksDatabase.getCommonDbOptions(), dbHome, columnFamilies, outHandles);

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
		columnFamilies.clear();
		if (rocksDb.isOwningHandle()) {
			try {
				rocksDb.syncWal();
			} catch (RocksDBException ignored) {
			}
		}
		super.close();
		rocksDb.close();
	}

	@Override
	public @NotNull Transaction beginTransaction() {
		return new RocksDbTrans();
	}

	public static @Nullable Runnable verifyAction;

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
				//noinspection DataFlowIssue
				var rocksTable = null != table ? (Database.AbstractKVTable)table.getStorage().getDatabaseTable() : null;
				if (null != rocksTable) {
					for (var key : tks.getValue()) {
						var value = rocksTable.find(key);
						if (null != value) {
							var resultTable = result.computeIfAbsent(tableName, __ -> new HashMap<>());
							resultTable.put(key, value);
						}
					}
				}
			}
		}
		return result;
	}

	private final class RocksDbTrans implements Transaction {
		private @Nullable WriteBatch batch;

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
						rocksDb.write(RocksDatabase.getDefaultWriteOptions(), batch);
					} catch (RocksDBException e) {
						throw new RuntimeException(e);
					}
				}
			} else {
				try {
					rocksDb.write(RocksDatabase.getDefaultWriteOptions(), batch);
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

	private synchronized @NotNull ColumnFamilyHandle getOrAddFamily(String name, @Nullable OutObject<Boolean> isNew) {
		if (isNew != null)
			isNew.value = false;

		return columnFamilies.computeIfAbsent(name, key -> {
			try {
				if (isNew != null)
					isNew.value = true;
				return rocksDb.createColumnFamily(new ColumnFamilyDescriptor(
						key.getBytes(StandardCharsets.UTF_8), RocksDatabase.getDefaultCfOptions()));
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

	@Override
	public synchronized @NotNull Table @NotNull [] openTables(String @NotNull [] names) {
		var n = names.length;
		var tables = new Table[n];
		var newIndexes = new IntList();
		var keyList = new ArrayList<byte[]>();
		for (int idx = 0; idx < n; idx++) {
			var cfh = columnFamilies.get(names[idx]);
			if (cfh != null)
				tables[idx] = new TableRocksDb(cfh, false);
			else {
				newIndexes.add(idx);
				keyList.add(names[idx].getBytes(StandardCharsets.UTF_8));
			}
		}
		n = keyList.size();
		if (n > 0) {
			try {
				var cfhs = rocksDb.createColumnFamilies(RocksDatabase.getDefaultCfOptions(), keyList);
				if (cfhs.size() != keyList.size()) {
					throw new IllegalStateException("createColumnFamilies unmatched: "
							+ cfhs.size() + " != " + keyList.size());
				}
				for (int i = 0; i < n; i++) {
					var idx = newIndexes.get(i);
					var cfh = cfhs.get(i);
					columnFamilies.put(names[idx], cfh);
					tables[idx] = new TableRocksDb(cfh, true);
				}
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}
		return tables;
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
				var value = rocksDb.get(columnFamily, RocksDatabase.getDefaultReadOptions(), key.Bytes, key.ReadIndex, key.size());
				return value != null ? ByteBuffer.Wrap(value) : null;
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public void remove(Transaction txn, ByteBuffer key) {
			((RocksDbTrans)txn).remove(key.CopyIf(), columnFamily);
		}

		@Override
		public void replace(Transaction txn, ByteBuffer key, ByteBuffer value) {
			((RocksDbTrans)txn).put(key.CopyIf(), value.CopyIf(), columnFamily);
		}

		@Override
		public long walk(TableWalkHandleRaw callback) {
			try (var it = rocksDb.newIterator(columnFamily, RocksDatabase.getDefaultReadOptions())) {
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
			try (var it = rocksDb.newIterator(columnFamily, RocksDatabase.getDefaultReadOptions())) {
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
			try (var it = rocksDb.newIterator(columnFamily, RocksDatabase.getDefaultReadOptions())) {
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
			try (var it = rocksDb.newIterator(columnFamily, RocksDatabase.getDefaultReadOptions())) {
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
			try (var it = rocksDb.newIterator(columnFamily, RocksDatabase.getDefaultReadOptions())) {
				if (exclusiveStartKey == null)
					it.seekToFirst();
				else
					it.seek(copyIf(exclusiveStartKey));
				if (!it.isValid())
					return null;

				var lastKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey != null && exclusiveStartKey.equals(lastKey)) { // 第一个item可能为exclusiveStartKey时需要忽略。
					it.next();
				}
				for (; proposeLimit-- > 0 && it.isValid(); it.next()) {
					lastKey = it.key();
					if (!callback.handle(lastKey, it.value()))
						break;
				}
				return it.isValid() ? ByteBuffer.Wrap(lastKey) : null;
			}
		}

		@Override
		public ByteBuffer walkKey(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			if (proposeLimit <= 0)
				return null;
			try (var it = rocksDb.newIterator(columnFamily, RocksDatabase.getDefaultReadOptions())) {
				if (exclusiveStartKey == null)
					it.seekToFirst();
				else
					it.seek(copyIf(exclusiveStartKey));
				if (!it.isValid())
					return null;

				var lastKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey != null && exclusiveStartKey.equals(lastKey)) { // 第一个item可能为exclusiveStartKey时需要忽略。
					it.next();
				}
				for (; proposeLimit-- > 0 && it.isValid(); it.next()) {
					lastKey = it.key();
					if (!callback.handle(lastKey))
						break;
				}
				return it.isValid() ? ByteBuffer.Wrap(lastKey) : null;
			}
		}

		@Override
		public ByteBuffer walkDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkHandleRaw callback) {
			if (proposeLimit <= 0)
				return null;
			try (var it = rocksDb.newIterator(columnFamily, RocksDatabase.getDefaultReadOptions())) {
				if (exclusiveStartKey == null)
					it.seekToLast();
				else
					it.seekForPrev(copyIf(exclusiveStartKey));
				if (!it.isValid())
					return null;

				var lastKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey != null && exclusiveStartKey.equals(lastKey)) { // 第一个item可能为exclusiveStartKey时需要忽略。
					it.prev();
				}
				for (; proposeLimit-- > 0 && it.isValid(); it.prev()) {
					lastKey = it.key();
					if (!callback.handle(lastKey, it.value()))
						break;
				}
				return it.isValid() ? ByteBuffer.Wrap(lastKey) : null;
			}
		}

		@Override
		public ByteBuffer walkKeyDesc(ByteBuffer exclusiveStartKey, int proposeLimit, TableWalkKeyRaw callback) {
			if (proposeLimit <= 0)
				return null;
			try (var it = rocksDb.newIterator(columnFamily, RocksDatabase.getDefaultReadOptions())) {
				if (exclusiveStartKey == null)
					it.seekToLast();
				else
					it.seekForPrev(copyIf(exclusiveStartKey));
				if (!it.isValid())
					return null;

				var lastKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey != null && exclusiveStartKey.equals(lastKey)) { // 第一个item可能为exclusiveStartKey时需要忽略。
					it.prev();
				}
				for (; proposeLimit-- > 0 && it.isValid(); it.prev()) {
					lastKey = it.key();
					if (!callback.handle(lastKey))
						break;
				}
				return it.isValid() ? ByteBuffer.Wrap(lastKey) : null;
			}
		}
	}

	private final class OperatesRocksDb implements Operates {
		private final ColumnFamilyHandle columnFamily = getOrAddFamily("zeze.OperatesRocksDb.Schemas", null);

		@Override
		public synchronized DataWithVersion getDataWithVersion(ByteBuffer key) {
			try {
				return DataWithVersion.decode(rocksDb.get(columnFamily,
						RocksDatabase.getDefaultReadOptions(), key.Bytes, key.ReadIndex, key.size()));
			} catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public synchronized KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			try {
				var dv = DataWithVersion.decode(rocksDb.get(columnFamily,
						RocksDatabase.getDefaultReadOptions(), key.Bytes, key.ReadIndex, key.size()));
				if (dv.version != version)
					return KV.create(version, false);

				dv.version = ++version;
				dv.data = data;
				var value = ByteBuffer.Allocate(5 + 9 + dv.data.size());
				dv.encode(value);
				rocksDb.put(columnFamily, RocksDatabase.getDefaultWriteOptions(),
						key.Bytes, key.ReadIndex, key.size(),
						value.Bytes, 0, value.WriteIndex);
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
