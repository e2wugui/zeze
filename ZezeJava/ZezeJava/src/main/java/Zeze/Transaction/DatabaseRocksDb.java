package Zeze.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import Zeze.Util.OutObject;
import Zeze.Util.RocksDatabase;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rocksdb.RocksDBException;

public class DatabaseRocksDb extends Database {
	private final @NotNull RocksDatabase rocksDb;

	public DatabaseRocksDb(@NotNull Application zeze, @NotNull Config.DatabaseConf conf) {
		super(zeze, conf);

		var homePath = getDatabaseUrl().isEmpty() ? "db" : getDatabaseUrl();
		try {
			// DirectOperates 依赖 Db，所以只能在这里打开。要不然，放在Open里面更加合理。
			rocksDb = new RocksDatabase(homePath);
			setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesRocksDb());
		} catch (RocksDBException e) {
			throw Task.forceThrow(e);
		}
	}

	@Override
	public void close() {
		logger.info("Close: {}", getDatabaseUrl());
		super.close();
		if (!rocksDb.isClosed()) {
			try {
				rocksDb.getRocksDb().syncWal();
			} catch (RocksDBException ignored) {
			}
			rocksDb.close();
		}
	}

	@Override
	public @NotNull Transaction beginTransaction() {
		return new RocksDbTrans();
	}

	public static @Nullable Runnable verifyAction;

	// 多表原子查询。
	public @NotNull HashMap<String, Map<ByteBuffer, ByteBuffer>> finds(@NotNull Map<String, Set<ByteBuffer>> tableKeys) {
		if (verifyAction == null)
			throw new IllegalStateException("only work with flushAtomicTest=true");

		var result = new HashMap<String, Map<ByteBuffer, ByteBuffer>>(tableKeys.size());
		for (var tks : tableKeys.entrySet())
			result.put(tks.getKey(), new HashMap<>(tks.getValue().size()));

		lock();
		try {
			for (var tks : tableKeys.entrySet()) {
				var tableName = tks.getKey();
				var table = getTable(tableName);
				//noinspection DataFlowIssue
				var rocksTable = table != null ? (Database.AbstractKVTable)table.getStorage().getDatabaseTable() : null;
				if (rocksTable != null) {
					for (var key : tks.getValue()) {
						var value = rocksTable.find(key);
						if (value != null)
							result.computeIfAbsent(tableName, __ -> new HashMap<>()).put(key, value);
					}
				}
			}
		} finally {
			unlock();
		}
		return result;
	}

	private final class RocksDbTrans implements Transaction {
		private @Nullable RocksDatabase.Batch batch;

		private @NotNull RocksDatabase.Batch getBatch() {
			var wb = batch;
			if (wb == null)
				batch = wb = rocksDb.borrowBatch();
			return wb;
		}

		void put(byte @NotNull [] key, byte @NotNull [] value, @NotNull RocksDatabase.Table table) {
			try {
				table.put(getBatch(), key, value);
			} catch (RocksDBException e) {
				Task.forceThrow(e);
			}
		}

		void remove(byte @NotNull [] key, @NotNull RocksDatabase.Table table) {
			try {
				table.delete(getBatch(), key);
			} catch (RocksDBException e) {
				Task.forceThrow(e);
			}
		}

		@Override
		public void commit() {
			if (batch == null)
				return;

			if (verifyAction != null) {
				lock();
				try {
					batch.commit(RocksDatabase.getDefaultWriteOptions());
				} catch (RocksDBException e) {
					Task.forceThrow(e);
				} finally {
					unlock();
				}
			} else {
				try {
					batch.commit(RocksDatabase.getDefaultWriteOptions());
				} catch (RocksDBException e) {
					Task.forceThrow(e);
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

	private @NotNull RocksDatabase.Table getOrAddTable(@NotNull String name, @Nullable OutObject<Boolean> isNew) {
		lock();
		try {
			return rocksDb.getOrAddTable(name, isNew);
		} catch (RocksDBException e) {
			throw Task.forceThrow(e);
		} finally {
			unlock();
		}
	}

	@Override
	public @NotNull Table openTable(@NotNull String name, int id) {
		var isNew = new OutObject<Boolean>();
		var table = getOrAddTable(name, isNew);
		return new TableRocksDb(table, isNew.value);
	}

	@Override
	public @NotNull Table @NotNull [] openTables(String @NotNull [] names, int @NotNull [] ids) {
		lock();
		try {
			var n = names.length;
			var tables = new Table[n];
			var isNews = new boolean[n];
			var rocksDbTables = rocksDb.getOrAddTables(names, isNews);
			for (int i = 0; i < n; i++)
				tables[i] = new TableRocksDb(rocksDbTables[i], isNews[i]);
			return tables;
		} catch (RocksDBException e) {
			throw Task.forceThrow(e);
		} finally {
			unlock();
		}
	}

	private final class TableRocksDb extends Database.AbstractKVTable {
		private final @NotNull RocksDatabase.Table table;
		private final boolean isNew;

		TableRocksDb(@NotNull RocksDatabase.Table table, boolean isNew) {
			this.table = table;
			this.isNew = isNew;
		}

		@Override
		public void clear() {
			try {
				table.clear();
			} catch (RocksDBException ex) {
				Task.forceThrow(ex);
			}
		}

		@Override
		public @NotNull DatabaseRocksDb getDatabase() {
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
		public @Nullable ByteBuffer find(@NotNull ByteBuffer key) {
			try {
				var value = table.get(key.Bytes, key.ReadIndex, key.size());
				return value != null ? ByteBuffer.Wrap(value) : null;
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public void remove(@NotNull Transaction txn, @NotNull ByteBuffer key) {
			((RocksDbTrans)txn).remove(key.CopyIf(), table);
		}

		@Override
		public void replace(@NotNull Transaction txn, @NotNull ByteBuffer key, @NotNull ByteBuffer value) {
			((RocksDbTrans)txn).put(key.CopyIf(), value.CopyIf(), table);
		}

		@Override
		public long getSize() {
			try (var it = table.iterator()) {
				long countWalked = 0;
				for (it.seekToFirst(); it.isValid(); it.next())
					countWalked++;
				return countWalked;
			}
		}

		@Override
		public long getSizeApproximation() {
			try {
				return table.getKeyNumbers();
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public long walk(@NotNull TableWalkHandleRaw callback) throws Exception {
			try (var it = table.iterator()) {
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
		public long walkKey(@NotNull TableWalkKeyRaw callback) throws Exception {
			try (var it = table.iterator()) {
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
		public long walkDesc(@NotNull TableWalkHandleRaw callback) throws Exception {
			try (var it = table.iterator()) {
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
		public long walkKeyDesc(@NotNull TableWalkKeyRaw callback) throws Exception {
			try (var it = table.iterator()) {
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
		public @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
										 @NotNull TableWalkHandleRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			try (var it = table.iterator()) {
				if (exclusiveStartKey == null)
					it.seekToFirst();
				else
					it.seek(exclusiveStartKey.CopyIf());
				if (!it.isValid())
					return null;

				var lastKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey != null && exclusiveStartKey.equals(lastKey)) // 第一个item可能为exclusiveStartKey时需要忽略。
					it.next();
				for (; proposeLimit-- > 0 && it.isValid(); it.next()) {
					lastKey = it.key();
					if (!callback.handle(lastKey, it.value()))
						break;
				}
				return it.isValid() ? ByteBuffer.Wrap(lastKey) : null;
			}
		}

		@Override
		public @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											@NotNull TableWalkKeyRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			try (var it = table.iterator()) {
				if (exclusiveStartKey == null)
					it.seekToFirst();
				else
					it.seek(exclusiveStartKey.CopyIf());
				if (!it.isValid())
					return null;

				var lastKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey != null && exclusiveStartKey.equals(lastKey)) // 第一个item可能为exclusiveStartKey时需要忽略。
					it.next();
				for (; proposeLimit-- > 0 && it.isValid(); it.next()) {
					lastKey = it.key();
					if (!callback.handle(lastKey))
						break;
				}
				return it.isValid() ? ByteBuffer.Wrap(lastKey) : null;
			}
		}

		@Override
		public @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											 @NotNull TableWalkHandleRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			try (var it = table.iterator()) {
				if (exclusiveStartKey == null)
					it.seekToLast();
				else
					it.seekForPrev(exclusiveStartKey.CopyIf());
				if (!it.isValid())
					return null;

				var lastKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey != null && exclusiveStartKey.equals(lastKey)) // 第一个item可能为exclusiveStartKey时需要忽略。
					it.prev();
				for (; proposeLimit-- > 0 && it.isValid(); it.prev()) {
					lastKey = it.key();
					if (!callback.handle(lastKey, it.value()))
						break;
				}
				return it.isValid() ? ByteBuffer.Wrap(lastKey) : null;
			}
		}

		@Override
		public @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												@NotNull TableWalkKeyRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			try (var it = table.iterator()) {
				if (exclusiveStartKey == null)
					it.seekToLast();
				else
					it.seekForPrev(exclusiveStartKey.CopyIf());
				if (!it.isValid())
					return null;

				var lastKey = it.key();
				//noinspection EqualsBetweenInconvertibleTypes
				if (exclusiveStartKey != null && exclusiveStartKey.equals(lastKey)) // 第一个item可能为exclusiveStartKey时需要忽略。
					it.prev();
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
		private final RocksDatabase.Table table = getOrAddTable("zeze.OperatesRocksDb.Schemas", null);

		@Override
		public @NotNull DataWithVersion getDataWithVersion(@NotNull ByteBuffer key) {
			lock();
			try {
				return DataWithVersion.decode(table.get(key.Bytes, key.ReadIndex, key.size()));
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			} finally {
				unlock();
			}
		}

		@Override
		public @NotNull KV<Long, Boolean> saveDataWithSameVersion(@NotNull ByteBuffer key, @NotNull ByteBuffer data,
																  long version) {
			lock();
			try {
				var dv = DataWithVersion.decode(table.get(key.Bytes, key.ReadIndex, key.size()));
				if (dv.version != version)
					return KV.create(version, false);

				dv.version = ++version;
				dv.data = data;
				var value = ByteBuffer.Allocate(5 + 9 + dv.data.size());
				dv.encode(value);
				table.put(key.Bytes, key.ReadIndex, key.size(), value.Bytes, 0, value.WriteIndex);
				return KV.create(version, true);
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			} finally {
				unlock();
			}
		}

		@Override
		public void setInUse(int localId, @NotNull String global) {
			// rocksdb 独占由它自己打开的时候保证。
		}

		@Override
		public int clearInUse(int localId, @NotNull String global) {
			// rocksdb 独占由它自己打开的时候保证。
			return 0;
		}
	}
}
