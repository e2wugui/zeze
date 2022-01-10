package Zeze.Transaction;

import Zeze.Config.DatabaseConf;
import Zeze.Serialize.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** 
 Zeze.Transaction.Table.storage 为 null 时，就表示内存表了。这个实现是为了测试 checkpoint 流程。
*/
public final class DatabaseMemory extends Database {
	private static ProceduresMemory ProceduresMemory = new ProceduresMemory();
	public DatabaseMemory(DatabaseConf conf) {
		super(conf);
		setDirectOperates(ProceduresMemory);
	}

	public static class ProceduresMemory implements Operates {
		public final int ClearInUse(int localId, String global) {
			return 0;
		}
		public final void SetInUse(int localId, String global) {
		}

		private final HashMap<ByteBuffer, DataWithVersion> DataWithVersions = new HashMap<>();

		public DataWithVersion GetDataWithVersion(ByteBuffer key) {
			synchronized (DataWithVersions) {
				var exist = DataWithVersions.get(key);
				if (null == exist)
					return null;
				var copy = new DataWithVersion();
				copy.Data = ByteBuffer.Wrap(exist.Data.Copy());
				copy.Version = exist.Version;
				return copy;
			}
		}

		public final Zeze.Util.KV<Long, Boolean>  SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, long version) {
			synchronized (DataWithVersions) {
				var exist = DataWithVersions.get(key);
				if (null != exist) {
					if (exist.Version != version) {
						return Zeze.Util.KV.Create(exist.Version, false);
					}
					exist.Data = ByteBuffer.Wrap(data.Copy());
					++exist.Version;
					return Zeze.Util.KV.Create(exist.Version, true);
				}
				DataWithVersion tempVar = new DataWithVersion();
				tempVar.Data = ByteBuffer.Wrap(data.Copy());
				tempVar.Version = version;
				DataWithVersions.put(ByteBuffer.Wrap(key.Copy()), tempVar);
				return Zeze.Util.KV.Create(version, true);
			}
		}
	}

	public final static byte[] NullBytes = new byte[0];

	public class MemTrans implements Transaction {
		private ConcurrentHashMap<String, ConcurrentHashMap<ByteBuffer, byte[]>> batch = new ConcurrentHashMap<>();

		public MemTrans() {
		}

		public final void Commit() {
			// 整个db同步。
			synchronized (DatabaseMemory.databaseTables) {
				for (var e : batch.entrySet()) {
					final var db = databaseTables.computeIfAbsent(DatabaseMemory.this.getDatabaseUrl(), url -> new ConcurrentHashMap<>());
					final var table = db.computeIfAbsent(e.getKey(), tn -> new TableMemory(DatabaseMemory.this, tn));
					//if (e.getValue().size() > 2)
					//	System.err.println("commit for: " + e.getKey() + " keys:" + e.getValue().keySet());
					for (var r : e.getValue().entrySet()) {
						if (r.getValue() == NullBytes) {
							table.Map.remove(r.getKey());
						} else {
							table.Map.put(r.getKey(), r.getValue());
						}
					}
				}
			}
		}

		public final void Rollback() {
		}

		public final void close() {
		}

		public final void Remove(String tableName, ByteBuffer key) {
			var table = batch.computeIfAbsent(tableName, tn -> new ConcurrentHashMap<>());
			table.put(ByteBuffer.Wrap(key.Copy()), NullBytes);
		}

		public final void Replace(String tableName, ByteBuffer key, ByteBuffer value) {
			var table = batch.computeIfAbsent(tableName, tn -> new ConcurrentHashMap<>());
			table.put(ByteBuffer.Wrap(key.Copy()), value.Copy());
		}

		// 仅支持从一个db原子的查询数据。

		// 多表原子查询。
		public Map<String, Map<ByteBuffer, ByteBuffer>> Finds(Map<String, Set<ByteBuffer>> tableKeys) {
			var result = new HashMap<String, Map<ByteBuffer, ByteBuffer>>();
			synchronized (DatabaseMemory.databaseTables) {
				for (var tks : tableKeys.entrySet()) {
					final var tableName = tks.getKey();
					final var db = databaseTables.computeIfAbsent(DatabaseMemory.this.getDatabaseUrl(), url -> new ConcurrentHashMap<>());
					final var table = db.computeIfAbsent(tableName, tn -> new TableMemory(DatabaseMemory.this, tn));
					final var tableFinds = new HashMap<ByteBuffer, ByteBuffer>();
					result.put(tableName, tableFinds);
					for (var key : tks.getValue()) {
						tableFinds.put(key, table.Find(key)); // also put null value
					}
				}
			}
			return result;
		}

		// 单表原子查询
		public Map<ByteBuffer, ByteBuffer> Finds(String tableName, Set<ByteBuffer> keys) {
			var result = new HashMap<ByteBuffer, ByteBuffer>();
			synchronized (DatabaseMemory.databaseTables) {
				//System.err.println("finds for: " + tableName + " keys.size=" +keys.size());
				final var db = databaseTables.computeIfAbsent(DatabaseMemory.this.getDatabaseUrl(), url -> new ConcurrentHashMap<>());
				final var table = db.computeIfAbsent(tableName, tn -> new TableMemory(DatabaseMemory.this, tn));
				for (var key : keys) {
					result.put(key, table.Find(key)); // also put null value
				}
			}
			return result;
		}
	}

	@Override
	public Transaction BeginTransaction() {
		return new MemTrans();
	}

	private final static ConcurrentHashMap<String, ConcurrentHashMap<String, TableMemory>> databaseTables = new ConcurrentHashMap<>();

	@Override
	public Database.Table OpenTable(String name) {
		var tables = databaseTables.computeIfAbsent(getDatabaseUrl(),
				(urlnotused) -> new java.util.concurrent.ConcurrentHashMap<>());

		return tables.computeIfAbsent(name, (tablenamenotused) -> new TableMemory(this, name));
	}

	public final static class TableMemory implements Database.Table {
		private final DatabaseMemory DatabaseReal;
		public DatabaseMemory getDatabaseReal() {
			return DatabaseReal;
		}
		public Database getDatabase() {
			return getDatabaseReal();
		}
		private final String Name;
		public String getName() {
			return Name;
		}

		public TableMemory(DatabaseMemory db, String name) {
			DatabaseReal = db;
			Name = name;
		}

		public boolean isNew() {
			return true;
		}

		final ConcurrentHashMap<ByteBuffer, byte[]> Map = new ConcurrentHashMap<>();

		public ByteBuffer Find(ByteBuffer key) {
			var value = Map.get(key);
			if (null != value)
				return ByteBuffer.Wrap(ByteBuffer.Copy(value));
			return null;
		}

		public void Remove(Transaction t, ByteBuffer key) {
			var mt = (MemTrans)t;
			mt.Remove(Name, key);
		}

		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			var mt = (MemTrans)t;
			mt.Replace(Name, key, value);
		}

		public long Walk(TableWalkHandleRaw callback) {
			// 不允许并发？
			long count = 0;
			for (var e : Map.entrySet()) {
				++count;
				if (!callback.handle(e.getKey().Bytes, e.getValue().clone())) {
					break;
				}
			}
			return count;
		}

		public void Close() {
		}
	}
}