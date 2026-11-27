package Zeze.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import Zeze.Application;
import Zeze.Config.DatabaseConf;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;
import org.jspecify.annotations.Nullable;

/**
 * 内存数据库后端：按 DatabaseUrl 在进程内模拟 KV 表与 Operates，主要用于测试 checkpoint 流程。
 * （表自身的内存表由 Table.storage == null 表示，与本后端不同。）
 */
public final class DatabaseMemory extends Database implements Database.Operates {
	// 按 DatabaseUrl 分区，避免同进程多 Memory 库实例互相串扰 schemas 版本记录。
	private static final HashMap<String, HashMap<ByteBuffer, DataWithVersion>> dataWithVersions = new HashMap<>();
	// 删除哨兵：必须为独立实例，避免与 0 字节编码值（ByteBuffer.Empty）别名碰撞。
	private static final byte [] removed = new byte[0];
	private static final HashMap<String, HashMap<String, TableData>> databaseTables = new HashMap<>();
	private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private record TableData(TreeMap<ByteBuffer, byte[]> rows, @Nullable String renamedFrom) {
		private TableData() {
			this(new TreeMap<>(ByteBuffer::compareTo), null);
		}

	}

	public static void clear() {
		lock.writeLock().lock();
		try {
			for (var db : databaseTables.values())
				for (var table : db.values())
					table.rows.clear();
			// KV 表清了而 Operates 数据不清会在测试间残留版本前像，clear 一并清理。
			dataWithVersions.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void renameTable(String tableOldName, String tableNewName) throws Exception {
		if (tableOldName.equals(tableNewName))
			return;
		lock.writeLock().lock();
		try {
			var tables = databaseTables.get(getDatabaseUrl());
			if (tables == null)
				return;
			var table = tables.get(tableOldName);
			if (table == null)
				return;
			var target = tables.get(tableNewName);
			if (target != null) {
				// 兼容检查可能重跑，此时原名下已经打开替代表。只接受实际由该源表改名的目标。
				if (tableOldName.equals(target.renamedFrom))
					return;
				throw new IllegalStateException("table already exists: " + tableNewName);
	}
			// 搬迁行数据，给目标名分配新的表身份。即使日后改回原名，旧句柄仍然失效。
			tables.put(tableNewName, new TableData(table.rows, tableOldName));
			tables.remove(tableOldName);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public DatabaseMemory(@Nullable Application zeze, DatabaseConf conf) {
		super(zeze, conf);
		setDirectOperates(conf.isDisableOperates() ? new NullOperates() : this);
	}

	@Override
	public int clearInUse(int localId, String global) {
		return 0;
	}

	@Override
	public void setInUse(int localId, String global) {
	}

	@Override
	public @Nullable DataWithVersion getDataWithVersion(ByteBuffer key) {
		// 静态共享 map 须由静态读写锁守卫，与 clear() 及跨实例访问互斥。
		lock.readLock().lock();
		try {
			var db = dataWithVersions.get(getDatabaseUrl());
			var exist = db != null ? db.get(key) : null;
			if (exist == null)
				return null;
			var copy = new DataWithVersion();
			//noinspection DataFlowIssue
			copy.data = ByteBuffer.Wrap(exist.data.Copy());
			copy.version = exist.version;
			return copy;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public KV<Long, Boolean> saveDataWithSameVersion(ByteBuffer key, ByteBuffer data,
															  long version) {
		// 同getDataWithVersion，静态写锁守卫共享dataWithVersions。
		lock.writeLock().lock();
		try {
			var db = dataWithVersions.computeIfAbsent(getDatabaseUrl(), __ -> new HashMap<>());
			var exist = db.get(key);
			if (exist != null) {
				if (exist.version != version)
					return KV.create(exist.version, false);
				exist.data = ByteBuffer.Wrap(data.Copy());
				return KV.create(++exist.version, true);
			}
			DataWithVersion tempVar = new DataWithVersion();
			tempVar.data = ByteBuffer.Wrap(data.Copy());
			tempVar.version = version;
			db.put(ByteBuffer.Wrap(key.Copy()), tempVar);
			return KV.create(version, true);
		} finally {
			lock.writeLock().unlock();
		}
	}

	private record TableBatch(@Nullable TableData expectedTable, HashMap<ByteBuffer, byte[]> writes) {
	}

	public final class MemTrans implements Transaction {
		private final HashMap<String, TableBatch> batch = new HashMap<>();

		@Override
		public void commit() {
			if (batch.isEmpty())
				return;
			int n = 0;
			// 整个db同步。
			lock.writeLock().lock();
			try {
				var tables = databaseTables.get(getDatabaseUrl());
				// 校验整批后才创建缺失表或写入行，避免误写换代的表或部分提交。
				for (var entry : batch.entrySet()) {
					var current = tables != null ? tables.get(entry.getKey()) : null;
					if (entry.getValue().expectedTable() != current)
						throw new IllegalStateException("memory table handle is no longer current: " + entry.getKey());
				}
				if (tables == null) {
					tables = new HashMap<>();
					databaseTables.put(getDatabaseUrl(), tables);
				}
				for (var entry : batch.entrySet()) {
					var updates = entry.getValue();
					var table = updates.expectedTable();
					if (table == null) {
						table = new TableData();
						tables.put(entry.getKey(), table);
						entry.setValue(new TableBatch(table, updates.writes()));
					}
					for (var r : updates.writes().entrySet()) {
						if (r.getValue() == removed)
							table.rows.remove(r.getKey());
						else
							table.rows.put(r.getKey(), r.getValue());
						n++;
					}
				}
				batch.clear();
			} finally {
				lock.writeLock().unlock();
				if (n >= 10000)
					logger.warn("MemTrans commit too many records: {}", n);
			}
		}

		@Override
		public void rollback() {
			batch.clear();
		}

		@Override
		public void close() {
			batch.clear();
		}

		// 批次及提交后的表都会持有这些字节，调用方可以继续复用自己的缓冲区。
		public void remove(String tableName, ByteBuffer key) {
			stage(tableName, key, removed);
		}

		public void replace(String tableName, ByteBuffer key, ByteBuffer value) {
			stage(tableName, key, value.Copy());
		}

		private void stage(String tableName, ByteBuffer key, byte[] value) {
			TableData table;
			lock.readLock().lock();
			try {
				var tables = databaseTables.get(getDatabaseUrl());
				table = tables != null ? tables.get(tableName) : null;
			} finally {
				lock.readLock().unlock();
	}
			stage(tableName, table, key, value);
		}

		private void stage(String tableName, @Nullable TableData table, ByteBuffer key, byte[] value) {
			var updates = batch.computeIfAbsent(tableName, __ -> new TableBatch(table, new HashMap<>()));
			if (updates.expectedTable() != table)
				throw new IllegalStateException("different table generations in one transaction: " + tableName);
			updates.writes().put(ByteBuffer.Wrap(key.Copy()), value);
		}
	}

	@Override
	public Transaction beginTransaction() {
		return new MemTrans();
	}

	@Override
	public Database.Table openTable(String name, int id) {
		return new TableMemory(name);
		}

	// 仅支持从一个db原子的查询数据。

	// 多表原子查询。
	public HashMap<String, Map<ByteBuffer, @Nullable ByteBuffer>> finds(
			Map<String, Set<ByteBuffer>> tableKeys) {
		var result = new HashMap<String, Map<ByteBuffer, @Nullable ByteBuffer>>(tableKeys.size());
		for (var tks : tableKeys.entrySet())
			result.put(tks.getKey(), new HashMap<>(tks.getValue().size()));
		lock.readLock().lock();
		try {
			var db = databaseTables.get(getDatabaseUrl());
			for (var tks : tableKeys.entrySet()) {
				var tableName = tks.getKey();
				var table = db != null ? db.get(tableName) : null;
				var tableFinds = result.get(tableName);
				for (var key : tks.getValue()) {
					ByteBuffer value = null;
					if (table != null) {
						var v = table.rows.get(key);
						if (v != null)
							value = ByteBuffer.Wrap(ByteBuffer.Copy(v));
					}
					tableFinds.put(key, value); // also put null value
				}
			}
		} finally {
			lock.readLock().unlock();
		}
		return result;
	}

	// 单表原子查询
	public HashMap<ByteBuffer, @Nullable ByteBuffer> finds(String tableName, Set<ByteBuffer> keys) {
		var result = new HashMap<ByteBuffer, @Nullable ByteBuffer>(keys.size());
		lock.readLock().lock();
		try {
			var db = databaseTables.get(getDatabaseUrl());
			var table = db != null ? db.get(tableName) : null;
			for (var key : keys) {
				ByteBuffer value = null;
				if (table != null) {
					var v = table.rows.get(key);
					if (v != null)
						value = ByteBuffer.Wrap(ByteBuffer.Copy(v));
				}
				result.put(key, value); // also put null value
			}
		} finally {
			lock.readLock().unlock();
		}
		return result;
	}

	public final class TableMemory extends Database.AbstractKVTable {
		private final String name;
		private final TableData data;

		public TableMemory(String name) {
			this.name = name;
			lock.writeLock().lock();
			try {
				var tables = databaseTables.computeIfAbsent(getDatabaseUrl(), __ -> new HashMap<>());
				// 同 URL 的实例共享行数据，各自持有表句柄及所属数据库。
				data = tables.computeIfAbsent(name, __ -> new TableData());
			} finally {
				lock.writeLock().unlock();
		}
		}

		// 调用方持有数据库读锁或写锁。
		private void checkCurrent() {
			var tables = databaseTables.get(getDatabaseUrl());
			if (tables == null || tables.get(name) != data)
				throw new IllegalStateException("memory table handle is no longer current: " + name);
		}

		@Override
		public DatabaseMemory getDatabase() {
			return DatabaseMemory.this;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean isNew() {
			return true;
		}

		@Override
		public @Nullable ByteBuffer find(ByteBuffer key) {
			lock.readLock().lock();
			try {
				checkCurrent();
				var value = data.rows.get(key);
				return value != null ? ByteBuffer.Wrap(ByteBuffer.Copy(value)) : null;
			} finally {
				lock.readLock().unlock();
			}
		}

		@Override
		public void remove(Transaction t, ByteBuffer key) {
			((MemTrans)t).stage(name, data, key, removed);
		}

		@Override
		public void replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			checkKvKeyLength(name, key);
			((MemTrans)t).stage(name, data, key, value.Copy());
		}

		@Override
		public void clear() {
			lock.writeLock().lock();
			try {
				checkCurrent();
				data.rows.clear();
			} finally {
				lock.writeLock().unlock();
			}
		}

		@Override
		public long getSize() {
			lock.readLock().lock();
			try {
				checkCurrent();
				return data.rows.size();
			} finally {
				lock.readLock().unlock();
			}
		}

		@Override
		public long getSizeApproximation() {
			return getSize();
		}

		@Override
		public long walk(TableWalkHandleRaw callback) throws Exception {
			ByteBuffer[] keys;
			byte[][] values;
			int i = 0, n;
			lock.readLock().lock();
			try {
				checkCurrent();
				n = data.rows.size();
				keys = new ByteBuffer[n];
				values = new byte[n][];
				for (var e : data.rows.entrySet()) {
					keys[i] = e.getKey();
					values[i++] = e.getValue();
				}
			} finally {
				lock.readLock().unlock();
			}
			long count = 0;
			for (i = 0; i < n; i++) {
				count++;
				if (!callback.handle(keys[i].Copy(), values[i].clone()))
					break;
			}
			return count;
		}

		@Override
		public long walkKey(TableWalkKeyRaw callback) throws Exception {
			ByteBuffer[] keys;
			lock.readLock().lock();
			try {
				checkCurrent();
				keys = data.rows.keySet().toArray(new ByteBuffer[0]);
			} finally {
				lock.readLock().unlock();
			}
			long count = 0;
			for (var key : keys) {
				count++;
				if (!callback.handle(key.Copy()))
					break;
			}
			return count;
		}

		@Override
		public long walkDesc(TableWalkHandleRaw callback) throws Exception {
			ByteBuffer[] keys;
			byte[][] values;
			int i = 0, n;
			lock.readLock().lock();
			try {
				checkCurrent();
				n = data.rows.size();
				keys = new ByteBuffer[n];
				values = new byte[n][];
				for (var e : data.rows.descendingMap().entrySet()) {
					keys[i] = e.getKey();
					values[i++] = e.getValue();
				}
			} finally {
				lock.readLock().unlock();
			}
			long count = 0;
			for (i = 0; i < n; i++) {
				count++;
				if (!callback.handle(keys[i].Copy(), values[i].clone()))
					break;
			}
			return count;
		}

		@Override
		public long walkKeyDesc(TableWalkKeyRaw callback) throws Exception {
			ByteBuffer[] keys;
			lock.readLock().lock();
			try {
				checkCurrent();
				keys = data.rows.descendingKeySet().toArray(new ByteBuffer[0]);
			} finally {
				lock.readLock().unlock();
			}
			long count = 0;
			for (var key : keys) {
				count++;
				if (!callback.handle(key.Copy()))
					break;
			}
			return count;
		}

		@Override
		public @Nullable ByteBuffer walk(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
										 TableWalkHandleRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			final var keys = new ArrayList<ByteBuffer>();
			final var values = new ArrayList<byte[]>();
			lock.readLock().lock();
			try {
				checkCurrent();
				var map = data.rows;
				var it = exclusiveStartKey == null
						? map.entrySet().iterator()
						: map.tailMap(exclusiveStartKey).entrySet().iterator();
				if (it.hasNext()) {
					var next = it.next();
					if (!next.getKey().equals(exclusiveStartKey)) { // 第一个Item可能需要忽略
						keys.add(next.getKey());
						values.add(next.getValue());
						proposeLimit--;
					}
					while (proposeLimit-- > 0 && it.hasNext()) {
						next = it.next();
						keys.add(next.getKey());
						values.add(next.getValue());
					}
				}
			} finally {
				lock.readLock().unlock();
			}
			byte[] lastKey = null;
			for (int i = 0, n = keys.size(); i < n; i++) {
				lastKey = keys.get(i).Copy();
				if (!callback.handle(lastKey, values.get(i).clone()))
					break;
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public @Nullable ByteBuffer walkKey(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											TableWalkKeyRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			final var keys = new ArrayList<ByteBuffer>();
			lock.readLock().lock();
			try {
				checkCurrent();
				var map = data.rows;
				var it = exclusiveStartKey == null
						? map.keySet().iterator()
						: map.tailMap(exclusiveStartKey).keySet().iterator();
				if (it.hasNext()) {
					var key = it.next();
					if (!key.equals(exclusiveStartKey)) { // 第一个Item可能需要忽略
						keys.add(key);
						proposeLimit--;
					}
					while (proposeLimit-- > 0 && it.hasNext())
						keys.add(it.next());
				}
			} finally {
				lock.readLock().unlock();
			}
			byte[] lastKey = null;
			for (var key : keys) {
				lastKey = key.Copy();
				if (!callback.handle(lastKey))
					break;
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public @Nullable ByteBuffer walkDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
											 TableWalkHandleRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			final var keys = new ArrayList<ByteBuffer>();
			final var values = new ArrayList<byte[]>();
			lock.readLock().lock();
			try {
				checkCurrent();
				var map = data.rows;
				var it = exclusiveStartKey == null
						? map.descendingMap().entrySet().iterator()
						: map.descendingMap().tailMap(exclusiveStartKey).entrySet().iterator();
				if (it.hasNext()) {
					var next = it.next();
					if (!next.getKey().equals(exclusiveStartKey)) { // 第一个Item可能需要忽略
						keys.add(next.getKey());
						values.add(next.getValue());
						proposeLimit--;
					}
					while (proposeLimit-- > 0 && it.hasNext()) {
						next = it.next();
						keys.add(next.getKey());
						values.add(next.getValue());
					}
				}
			} finally {
				lock.readLock().unlock();
			}
			byte[] lastKey = null;
			for (int i = 0, n = keys.size(); i < n; i++) {
				lastKey = keys.get(i).Copy();
				if (!callback.handle(lastKey, values.get(i).clone()))
					break;
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public @Nullable ByteBuffer walkKeyDesc(@Nullable ByteBuffer exclusiveStartKey, int proposeLimit,
												TableWalkKeyRaw callback) throws Exception {
			if (proposeLimit <= 0)
				return null;
			final var keys = new ArrayList<ByteBuffer>();
			lock.readLock().lock();
			try {
				checkCurrent();
				var map = data.rows;
				var it = exclusiveStartKey == null
						? map.descendingMap().keySet().iterator()
						: map.descendingMap().tailMap(exclusiveStartKey).keySet().iterator();
				if (it.hasNext()) {
					var key = it.next();
					if (!key.equals(exclusiveStartKey)) { // 第一个Item可能需要忽略
						keys.add(key);
						proposeLimit--;
					}
					while (proposeLimit-- > 0 && it.hasNext())
						keys.add(it.next());
				}
			} finally {
				lock.readLock().unlock();
			}
			byte[] lastKey = null;
			for (var key : keys) {
				lastKey = key.Copy();
				if (!callback.handle(lastKey))
					break;
			}
			return lastKey != null ? ByteBuffer.Wrap(lastKey) : null;
		}

		@Override
		public void close() {
		}
	}
}
