package Zeze.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import Zeze.Config.DatabaseConf;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;

/**
 * Zeze.Transaction.Table.storage 为 null 时，就表示内存表了。这个实现是为了测试 checkpoint 流程。
 */
public final class DatabaseMemory extends Database implements Database.Operates {
	private static final HashMap<ByteBuffer, DataWithVersion> DataWithVersions = new HashMap<>();
	private static final byte[] Removed = ByteBuffer.Empty;
	private static final HashMap<String, HashMap<String, TableMemory>> databaseTables = new HashMap<>();
	private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public static void clear() {
		lock.writeLock().lock();
		try {
			databaseTables.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	public DatabaseMemory(DatabaseConf conf) {
		super(conf);
		setDirectOperates(this);
	}

	@Override
	public int ClearInUse(int localId, String global) {
		return 0;
	}

	@Override
	public void SetInUse(int localId, String global) {
	}

	@Override
	public synchronized DataWithVersion GetDataWithVersion(ByteBuffer key) {
		var exist = DataWithVersions.get(key);
		if (exist == null)
			return null;
		var copy = new DataWithVersion();
		copy.Data = ByteBuffer.Wrap(exist.Data.Copy());
		copy.Version = exist.Version;
		return copy;
	}

	@Override
	public synchronized KV<Long, Boolean> SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
		var exist = DataWithVersions.get(key);
		if (exist != null) {
			if (exist.Version != version)
				return KV.Create(exist.Version, false);
			exist.Data = ByteBuffer.Wrap(data.Copy());
			return KV.Create(++exist.Version, true);
		}
		DataWithVersion tempVar = new DataWithVersion();
		tempVar.Data = ByteBuffer.Wrap(data.Copy());
		tempVar.Version = version;
		DataWithVersions.put(ByteBuffer.Wrap(key.Copy()), tempVar);
		return KV.Create(version, true);
	}

	public final class MemTrans implements Transaction {
		private final HashMap<String, HashMap<ByteBuffer, byte[]>> batch = new HashMap<>();

		@Override
		public void Commit() {
			// 整个db同步。
			lock.writeLock().lock();
			try {
				var db = databaseTables.computeIfAbsent(getDatabaseUrl(), __ -> new HashMap<>());
				for (var e : batch.entrySet()) {
					//if (e.getValue().size() > 2)
					//	System.err.println("commit for: " + e.getKey() + " keys:" + e.getValue().keySet());
					var map = db.computeIfAbsent(e.getKey(), TableMemory::new).Map;
					for (var r : e.getValue().entrySet()) {
						if (r.getValue() == Removed)
							map.remove(r.getKey());
						else
							map.put(r.getKey(), r.getValue());
					}
				}
			} finally {
				lock.writeLock().unlock();
			}
		}

		@Override
		public void Rollback() {
		}

		@Override
		public void close() {
		}

		public void Remove(String tableName, ByteBuffer key) {
			batch.computeIfAbsent(tableName, __ -> new HashMap<>()).put(ByteBuffer.Wrap(key.Copy()), Removed);
		}

		public void Replace(String tableName, ByteBuffer key, ByteBuffer value) {
			batch.computeIfAbsent(tableName, __ -> new HashMap<>()).put(ByteBuffer.Wrap(key.Copy()), value.Copy());
		}
	}

	@Override
	public Transaction BeginTransaction() {
		return new MemTrans();
	}

	@Override
	public Database.Table OpenTable(String name) {
		lock.writeLock().lock();
		try {
			var tables = databaseTables.computeIfAbsent(getDatabaseUrl(), __ -> new HashMap<>());
			return tables.computeIfAbsent(name, TableMemory::new);
		} finally {
			lock.writeLock().unlock();
		}
	}

	// 仅支持从一个db原子的查询数据。

	// 多表原子查询。
	public HashMap<String, Map<ByteBuffer, ByteBuffer>> Finds(Map<String, Set<ByteBuffer>> tableKeys) {
		var result = new HashMap<String, Map<ByteBuffer, ByteBuffer>>(tableKeys.size());
		for (var tks : tableKeys.entrySet())
			result.put(tks.getKey(), new HashMap<>(tks.getValue().size()));
		lock.readLock().lock();
		try {
			var db = databaseTables.get(getDatabaseUrl());
			for (var tks : tableKeys.entrySet()) {
				var tableName = tks.getKey();
				var table = db != null ? db.get(tableName) : null;
				var map = table != null ? table.Map : null;
				var tableFinds = result.get(tableName);
				for (var key : tks.getValue()) {
					ByteBuffer value = null;
					if (map != null) {
						var v = map.get(key);
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
	public HashMap<ByteBuffer, ByteBuffer> Finds(String tableName, Set<ByteBuffer> keys) {
		var result = new HashMap<ByteBuffer, ByteBuffer>(keys.size());
		// System.err.println("finds for: " + tableName + " keys.size=" + keys.size());
		lock.readLock().lock();
		try {
			var db = databaseTables.get(getDatabaseUrl());
			var table = db != null ? db.get(tableName) : null;
			var map = table != null ? table.Map : null;
			for (var key : keys) {
				ByteBuffer value = null;
				if (map != null) {
					var v = map.get(key);
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

	public final class TableMemory implements Database.Table {
		private final String Name;
		private final HashMap<ByteBuffer, byte[]> Map = new HashMap<>();

		public TableMemory(String name) {
			Name = name;
		}

		@Override
		public DatabaseMemory getDatabase() {
			return DatabaseMemory.this;
		}

		public String getName() {
			return Name;
		}

		@Override
		public boolean isNew() {
			return true;
		}

		@Override
		public ByteBuffer Find(ByteBuffer key) {
			lock.readLock().lock();
			try {
				var value = Map.get(key);
				return value != null ? ByteBuffer.Wrap(ByteBuffer.Copy(value)) : null;
			} finally {
				lock.readLock().unlock();
			}
		}

		@Override
		public void Remove(Transaction t, ByteBuffer key) {
			((MemTrans)t).Remove(Name, key);
		}

		@Override
		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			((MemTrans)t).Replace(Name, key, value);
		}

		@Override
		public long Walk(TableWalkHandleRaw callback) {
			ByteBuffer[] keys;
			byte[][] values;
			int i = 0, n;
			lock.readLock().lock();
			try {
				n = Map.size();
				keys = new ByteBuffer[n];
				values = new byte[n][];
				for (var e : Map.entrySet()) {
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
		public long WalkKey(TableWalkKeyRaw callback) {
			ByteBuffer[] keys;
			lock.readLock().lock();
			try {
				keys = Map.keySet().toArray(new ByteBuffer[Map.size()]);
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
		public void Close() {
		}
	}
}
