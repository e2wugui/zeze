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
	// Application 启动时以 "zeze_cache_<serverId>" 命名创建 LocalRocksCacheDb（Application.java），
	// 它是纯本地缓存：每次启动前整目录删除重建，数据可随时从后端库重读，写路径不需要也不应付出 fsync 代价。
	// 而主库的 flush 要求 WAL 落盘：CheckpointWhenCommit 表"提交即物理落库"的承诺必须覆盖掉电场景
	// （非 sync 写只写 OS 页缓存，进程崩溃可由 WAL 恢复，主机掉电则丢失 WAL 尾部，如 AutoKey 水位回退重发已发出的号）。
	// RocksDatabase.Batch.commit() 无参版本即为 sync 写；Services/Token 等需要强持久的路径也是既有实践。
	private final boolean isLocalRocksCache;

	public DatabaseRocksDb(@Nullable Application zeze, @NotNull Config.DatabaseConf conf) {
		super(zeze, conf);

		isLocalRocksCache = conf.getName().startsWith("zeze_cache_");
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
	public void renameTable(String oldName, String newName) throws Exception {
		// RocksDB 不支持列族重命名：把旧表数据拷贝到备份列族后再删除原列族，
		// 对齐其他后端（MySQL RENAME TABLE / PG ALTER TABLE RENAME / Mongo renameCollection）
		// "版本升级时备份旧表"的语义。原来的直接 drop 会销毁旧数据，升级后无任何恢复途径。
		var oldTable = rocksDb.getTable(oldName);
		if (oldTable == null) // 不存在的表保持与 dropTable 相同的 no-op 行为
			return;
		var newTable = rocksDb.getOrAddTable(newName);
		try (var batch = rocksDb.borrowBatch()) {
			int n = 0;
			try (var it = oldTable.iterator()) {
				for (it.seekToFirst(); it.isValid(); it.next()) {
					newTable.put(batch, it.key(), it.value());
					if (++n >= 10000) { // 分批提交，避免大表时单个 WriteBatch 无界增长
						batch.commit();
						batch.clear();
						n = 0;
					}
				}
			}
			if (n > 0)
				batch.commit();
			// 拷贝中断（异常上抛）时旧表未被 drop，下次启动重跑本方法重新全量拷贝（put 幂等），自愈。
			rocksDb.dropTable(oldName);
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
				throw Task.forceThrow(e);
			}
		}

		void remove(byte @NotNull [] key, @NotNull RocksDatabase.Table table) {
			try {
				table.delete(getBatch(), key);
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public void commit() {
			if (batch == null)
				return;

			// 主库 sync 写（见 isLocalRocksCache 注释），本地缓存库维持非 sync 的默认写。
			var options = isLocalRocksCache ? RocksDatabase.getDefaultWriteOptions() : RocksDatabase.getSyncWriteOptions();
			if (verifyAction != null) {
				lock();
				try {
					batch.commit(options);
				} catch (RocksDBException e) {
					throw Task.forceThrow(e);
				} finally {
					unlock();
				}
			} else {
				try {
					batch.commit(options);
				} catch (RocksDBException e) {
					throw Task.forceThrow(e);
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
				throw Task.forceThrow(ex);
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
		private final RocksDatabase.Table table = getOrAddTable("Zeze_OperatesRocksDb_Schemas", null);

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
				table.put(key.Bytes, key.ReadIndex, key.size(), value.Bytes, value.ReadIndex, value.size());
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
