package Zeze.Transaction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Config.DatabaseConf;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.KV;

/**
 * Zeze.Transaction.Table.storage 为 null 时，就表示内存表了。这个实现是为了测试 checkpoint 流程。
 */
public final class DatabaseMemory extends Database {
	private static final ProceduresMemory ProceduresMemory = new ProceduresMemory();
	private static final byte[] Removed = new byte[0];
	private static final ConcurrentHashMap<String, ConcurrentHashMap<String, TableMemory>> databaseTables = new ConcurrentHashMap<>();
	private static final ReentrantLock lock = new ReentrantLock();

	public DatabaseMemory(DatabaseConf conf) {
		super(conf);
		setDirectOperates(ProceduresMemory);
	}

	public static final class ProceduresMemory implements Operates {
		private final HashMap<ByteBuffer, DataWithVersion> DataWithVersions = new HashMap<>();

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
	}

	public final class MemTrans implements Transaction {
		private final ConcurrentHashMap<String, ConcurrentHashMap<ByteBuffer, byte[]>> batch = new ConcurrentHashMap<>();

		@Override
		public void Commit() {
			// 整个db同步。
			lock.lock();
			try {
				for (var e : batch.entrySet()) {
					var db = databaseTables.computeIfAbsent(getDatabaseUrl(), url -> new ConcurrentHashMap<>());
					var table = db.computeIfAbsent(e.getKey(), tn -> new TableMemory(DatabaseMemory.this, tn));
					//if (e.getValue().size() > 2)
					//	System.err.println("commit for: " + e.getKey() + " keys:" + e.getValue().keySet());
					for (var r : e.getValue().entrySet()) {
						if (r.getValue() == Removed)
							table.Map.remove(r.getKey());
						else
							table.Map.put(r.getKey(), r.getValue());
					}
				}
			} finally {
				lock.unlock();
			}
		}

		@Override
		public void Rollback() {
		}

		@Override
		public void close() {
		}

		public void Remove(String tableName, ByteBuffer key) {
			var table = batch.computeIfAbsent(tableName, tn -> new ConcurrentHashMap<>());
			table.put(ByteBuffer.Wrap(key.Copy()), Removed);
		}

		public void Replace(String tableName, ByteBuffer key, ByteBuffer value) {
			var table = batch.computeIfAbsent(tableName, tn -> new ConcurrentHashMap<>());
			table.put(ByteBuffer.Wrap(key.Copy()), value.Copy());
		}

		// 仅支持从一个db原子的查询数据。

		// 多表原子查询。
		public Map<String, Map<ByteBuffer, ByteBuffer>> Finds(Map<String, Set<ByteBuffer>> tableKeys) {
			var result = new HashMap<String, Map<ByteBuffer, ByteBuffer>>();
			lock.lock();
			try {
				for (var tks : tableKeys.entrySet()) {
					var tableName = tks.getKey();
					var db = databaseTables.computeIfAbsent(getDatabaseUrl(), __ -> new ConcurrentHashMap<>());
					var table = db.computeIfAbsent(tableName, tn -> new TableMemory(DatabaseMemory.this, tn));
					var tableFinds = new HashMap<ByteBuffer, ByteBuffer>();
					result.put(tableName, tableFinds);
					for (var key : tks.getValue())
						tableFinds.put(key, table.Find(key)); // also put null value
				}
			} finally {
				lock.unlock();
			}
			return result;
		}

		// 单表原子查询
		public Map<ByteBuffer, ByteBuffer> Finds(String tableName, Set<ByteBuffer> keys) {
			var result = new HashMap<ByteBuffer, ByteBuffer>();
			lock.lock();
			try {
				//System.err.println("finds for: " + tableName + " keys.size=" +keys.size());
				var db = databaseTables.computeIfAbsent(getDatabaseUrl(), __ -> new ConcurrentHashMap<>());
				var table = db.computeIfAbsent(tableName, tn -> new TableMemory(DatabaseMemory.this, tn));
				for (var key : keys)
					result.put(key, table.Find(key)); // also put null value
			} finally {
				lock.unlock();
			}
			return result;
		}
	}

	@Override
	public Transaction BeginTransaction() {
		return new MemTrans();
	}

	@Override
	public Database.Table OpenTable(String name) {
		var tables = databaseTables.computeIfAbsent(getDatabaseUrl(), __ -> new ConcurrentHashMap<>());
		return tables.computeIfAbsent(name, tableName -> new TableMemory(this, tableName));
	}

	public static final class TableMemory implements Database.Table {
		private final DatabaseMemory DatabaseReal;
		private final String Name;
		private final ConcurrentHashMap<ByteBuffer, byte[]> Map = new ConcurrentHashMap<>();

		public TableMemory(DatabaseMemory db, String name) {
			DatabaseReal = db;
			Name = name;
		}

		@Override
		public Database getDatabase() {
			return DatabaseReal;
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
			var value = Map.get(key);
			return value != null ? ByteBuffer.Wrap(ByteBuffer.Copy(value)) : null;
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
			// 不允许并发？
			long count = 0;
			for (var e : Map.entrySet()) {
				count++;
				if (!callback.handle(e.getKey().Copy(), e.getValue().clone()))
					break;
			}
			return count;
		}

		@Override
		public long WalkKey(TableWalkKeyRaw callback) {
			// 不允许并发？
			long count = 0;
			for (var k : Map.keySet()) {
				count++;
				if (!callback.handle(k.Copy()))
					break;
			}
			return count;
		}

		@Override
		public void Close() {
		}
	}
}
