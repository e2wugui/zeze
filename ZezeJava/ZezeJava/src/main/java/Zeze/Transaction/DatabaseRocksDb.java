package Zeze.Transaction;

import java.util.Objects;

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
import org.jspecify.annotations.Nullable;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteOptions;
import Zeze.Util.RocksDatabase.Batch;

/** RocksDB 数据库后端：列族承载 KV 表，WAL 写入与 Operates 条件写；也用作本地镜像缓存库。 */
public class DatabaseRocksDb extends Database {
	private final RocksDatabase rocksDb;
	// 写选项由 DatabaseUrl 的 ?sync=true 决定（默认普通写；本地镜像缓存库恒普通写）。
	private final WriteOptions writeOptions;

	public DatabaseRocksDb(@Nullable Application zeze, Config.DatabaseConf conf) {
		super(zeze, conf);

		// DatabaseUrl 支持 ?sync=true：homePath/.lock 文件名不能带 query，用前剥离。
		var homePath = getDatabaseUrl().isEmpty() ? "db" : getDatabaseUrl();
		boolean syncWrite = false;
		var queryIndex = homePath.indexOf('?');
		if (queryIndex >= 0) {
			for (var pair : homePath.substring(queryIndex + 1).split("&")) {
				if (pair.equalsIgnoreCase("sync=true"))
					syncWrite = true;
				else if (!pair.isEmpty() && !pair.equalsIgnoreCase("sync=false"))
					logger.warn("DatabaseUrl unknown option ignored: {}", pair);
			}
			homePath = homePath.substring(0, queryIndex);
		}
		writeOptions = syncWrite
				? RocksDatabase.getSyncWriteOptions() : RocksDatabase.getDefaultWriteOptions();
		try {
			// DirectOperates 依赖 Db，所以只能在这里打开。要不然，放在Open里面更加合理。
			rocksDb = new RocksDatabase(homePath);
		} catch (RocksDBException e) {
			throw Task.forceThrow(e);
		}
		try {
			setDirectOperates(conf.isDisableOperates() ? new NullOperates() : new OperatesRocksDb());
		} catch (Throwable failure) {
			try {
				rocksDb.close();
			} catch (Throwable closeFailure) {
				if (closeFailure != failure)
					failure.addSuppressed(closeFailure);
			}
			throw failure;
		}
	}

	@Override
	public void close() {
		logger.info("Close: {}", getDatabaseUrl());
		try {
			super.close();
		} finally {
			if (!rocksDb.isClosed()) {
				try {
					rocksDb.getRocksDb().syncWal();
				} catch (RocksDBException ignored) {
				}
				rocksDb.close();
			}
		}
	}

	@Override
	public void renameTable(String oldName, String newName) throws Exception {
		// 列族相同意味着无需重命名；复制后删除会直接销毁源数据。
		if (oldName.equals(newName))
			return;
		// RocksDB 不支持列族重命名：全量拷贝到新列族后再删除源列族（中断时可幂等重跑）。
		var oldTable = rocksDb.getTable(oldName);
		if (oldTable == null) // 不存在的表保持与 dropTable 相同的 no-op 行为
			return;
		var newTable = rocksDb.getOrAddTable(newName);
		try (var batch = rocksDb.borrowBatch()) {
			int n = 0;
			try (var it = oldTable.iterator()) {
				for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next()) {
					newTable.put(batch, it.key(), it.value());
					if (++n >= 10000) { // 分批提交，避免大表时单个 WriteBatch 无界增长
						batch.commit(RocksDatabase.getSyncWriteOptions());
						batch.clear();
						n = 0;
					}
				}
				it.status(); // invalid也可能是扫描失败；确认正常EOF后才允许删除源表。
			}
			if (n > 0)
				batch.commit(RocksDatabase.getSyncWriteOptions());
			// 拷贝中断（异常上抛）时旧表未被 drop，下次启动重跑本方法重新全量拷贝（put 幂等），自愈。
			rocksDb.dropTable(oldName);
		}
	}

	@Override
	public Transaction beginTransaction() {
		return new RocksDbTrans();
	}

	public static @Nullable Runnable verifyAction;

	// 多表原子查询。
	public HashMap<String, Map<ByteBuffer, ByteBuffer>> finds(Map<String, Set<ByteBuffer>> tableKeys) {
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
		private @Nullable Batch batch;

		private RocksDatabase.Batch getBatch() {
			var wb = batch;
			if (wb == null)
				batch = wb = rocksDb.borrowBatch();
			return wb;
		}

		void put(byte [] key, byte [] value, RocksDatabase.Table table) {
			try {
				table.put(getBatch(), key, value);
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			}
		}

		void remove(byte [] key, RocksDatabase.Table table) {
			try {
				table.delete(getBatch(), key);
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public void commit() {
			if (batch == null || batch.getCount() == 0)
				return;

			// 写选项由 DatabaseUrl 的 ?sync=true 决定（默认普通写），本地缓存库恒普通写。
			var options = writeOptions;
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
			batch.clear();
		}

		@Override
		public void rollback() {
			if (batch != null)
				batch.clear();
		}

		@Override
		public void close() {
			if (batch != null) {
				batch.close();
				batch = null;
			}
		}
	}

	private RocksDatabase.Table getOrAddTable(String name, @Nullable OutObject<Boolean> isNew) {
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
	public Table openTable(String name, int id) {
		var isNew = new OutObject<Boolean>();
		var table = getOrAddTable(name, isNew);
		return new TableRocksDb(table, Objects.requireNonNull(isNew.value));
	}

	@Override
	public Table [] openTables(String [] names, int [] ids) {
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

	// 将主库已读记录回填本地镜像；不检查键长以支持存量超长键完成 load→remove 清理。
	static void backfillMirror(Table mirror, Transaction txn, ByteBuffer key, ByteBuffer value) {
		var target = (TableRocksDb)mirror;
		if (!target.getDatabase().getDatabaseUrl().startsWith("zeze_cache_"))
			throw new IllegalStateException("backfillMirror requires the local RocksDB cache");
		target.put(txn, key, value);
	}

	private final class TableRocksDb extends Database.AbstractKVTable {
		private final RocksDatabase.Table table;
		private final boolean isNew;

		TableRocksDb(RocksDatabase.Table table, boolean isNew) {
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
		public @Nullable ByteBuffer find(ByteBuffer key) {
			try {
				var value = table.get(key.Bytes, key.ReadIndex, key.size());
				return value != null ? ByteBuffer.Wrap(value) : null;
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			}
		}

		@Override
		public void remove(Transaction txn, ByteBuffer key) {
			((RocksDbTrans)txn).remove(key.CopyIf(), table);
		}

		@Override
		public void replace(Transaction txn, ByteBuffer key, ByteBuffer value) {
			checkKvKeyLength(table.getName(), key);
			put(txn, key, value);
		}

		private void put(Transaction txn, ByteBuffer key, ByteBuffer value) {
			((RocksDbTrans)txn).put(key.CopyIf(), value.CopyIf(), table);
		}

		@Override
		public long getSize() {
			try (var it = table.iterator()) {
				long countWalked = 0;
				for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next())
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
		public long walk(TableWalkHandleRaw callback) throws Exception {
			try (var it = table.iterator()) {
				long countWalked = 0;
				for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next()) {
					countWalked++;
					if (!callback.handle(it.key(), it.value()))
						break;
				}
				return countWalked;
			}
		}

		@Override
		public long walkKey(TableWalkKeyRaw callback) throws Exception {
			try (var it = table.iterator()) {
				long countWalked = 0;
				for (it.seekToFirst(); RocksDatabase.isValidOrThrow(it); it.next()) {
					countWalked++;
					if (!callback.handle(it.key()))
						break;
				}
				return countWalked;
			}
		}

		@Override
		public long walkDesc(TableWalkHandleRaw callback) throws Exception {
			try (var it = table.iterator()) {
				long countWalked = 0;
				for (it.seekToLast(); RocksDatabase.isValidOrThrow(it); it.prev()) {
					countWalked++;
					if (!callback.handle(it.key(), it.value()))
						break;
				}
				return countWalked;
			}
		}

		@Override
		public long walkKeyDesc(TableWalkKeyRaw callback) throws Exception {
			try (var it = table.iterator()) {
				long countWalked = 0;
				for (it.seekToLast(); RocksDatabase.isValidOrThrow(it); it.prev()) {
					countWalked++;
					if (!callback.handle(it.key()))
						break;
				}
				return countWalked;
			}
		}

		@Override
		public @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
										 TableWalkHandleRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			try (var it = table.iterator()) {
				if (exclusiveStartKey == null)
					it.seekToFirst();
				else
					it.seek(exclusiveStartKey.CopyIf());
				if (!RocksDatabase.isValidOrThrow(it))
					return null;

				var lastKey = it.key();
				if (exclusiveStartKey != null && exclusiveStartKey.contentEquals(lastKey)) // 第一个item可能为exclusiveStartKey时需要忽略。
					it.next();
				for (; proposeLimit-- > 0 && RocksDatabase.isValidOrThrow(it); it.next()) {
					lastKey = it.key();
					if (!callback.handle(lastKey, it.value()))
						break;
				}
				return RocksDatabase.isValidOrThrow(it) ? ByteBuffer.Wrap(lastKey) : null;
			}
		}

		@Override
		public @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											TableWalkKeyRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			try (var it = table.iterator()) {
				if (exclusiveStartKey == null)
					it.seekToFirst();
				else
					it.seek(exclusiveStartKey.CopyIf());
				if (!RocksDatabase.isValidOrThrow(it))
					return null;

				var lastKey = it.key();
				if (exclusiveStartKey != null && exclusiveStartKey.contentEquals(lastKey)) // 第一个item可能为exclusiveStartKey时需要忽略。
					it.next();
				for (; proposeLimit-- > 0 && RocksDatabase.isValidOrThrow(it); it.next()) {
					lastKey = it.key();
					if (!callback.handle(lastKey))
						break;
				}
				return RocksDatabase.isValidOrThrow(it) ? ByteBuffer.Wrap(lastKey) : null;
			}
		}

		@Override
		public @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											 TableWalkHandleRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			try (var it = table.iterator()) {
				if (exclusiveStartKey == null)
					it.seekToLast();
				else
					it.seekForPrev(exclusiveStartKey.CopyIf());
				if (!RocksDatabase.isValidOrThrow(it))
					return null;

				var lastKey = it.key();
				if (exclusiveStartKey != null && exclusiveStartKey.contentEquals(lastKey)) // 第一个item可能为exclusiveStartKey时需要忽略。
					it.prev();
				for (; proposeLimit-- > 0 && RocksDatabase.isValidOrThrow(it); it.prev()) {
					lastKey = it.key();
					if (!callback.handle(lastKey, it.value()))
						break;
				}
				return RocksDatabase.isValidOrThrow(it) ? ByteBuffer.Wrap(lastKey) : null;
			}
		}

		@Override
		public @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												TableWalkKeyRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			try (var it = table.iterator()) {
				if (exclusiveStartKey == null)
					it.seekToLast();
				else
					it.seekForPrev(exclusiveStartKey.CopyIf());
				if (!RocksDatabase.isValidOrThrow(it))
					return null;

				var lastKey = it.key();
				if (exclusiveStartKey != null && exclusiveStartKey.contentEquals(lastKey)) // 第一个item可能为exclusiveStartKey时需要忽略。
					it.prev();
				for (; proposeLimit-- > 0 && RocksDatabase.isValidOrThrow(it); it.prev()) {
					lastKey = it.key();
					if (!callback.handle(lastKey))
						break;
				}
				return RocksDatabase.isValidOrThrow(it) ? ByteBuffer.Wrap(lastKey) : null;
			}
		}
	}

	private final class OperatesRocksDb implements Operates {
		private final RocksDatabase.Table table = getOrAddTable("Zeze_OperatesRocksDb_Schemas", null);

		@Override
		public DataWithVersion getDataWithVersion(ByteBuffer key) {
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
		public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data,
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
				// schemas 版本记录与事务提交同选项（?sync=true 决定，默认普通写；见类头注释）。
				table.put(writeOptions, key.Bytes, key.ReadIndex, key.size(), value.Bytes, value.ReadIndex, value.size());
				return KV.create(version, true);
			} catch (RocksDBException e) {
				throw Task.forceThrow(e);
			} finally {
				unlock();
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
