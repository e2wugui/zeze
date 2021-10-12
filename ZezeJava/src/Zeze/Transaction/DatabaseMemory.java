package Zeze.Transaction;

import Zeze.Config.DatabaseConf;
import Zeze.Serialize.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** 
 Zeze.Transaction.Table.storage 为 null 时，就表示内存表了。这个实现是为了测试 checkpoint 流程。
*/
public final class DatabaseMemory extends Database {
	public DatabaseMemory(DatabaseConf conf) {
		super(conf);
		setDirectOperates(new ProceduresMemory());
	}

	public static class ProceduresMemory implements Operates {
		public final int ClearInUse(int localId, String global) {
			return 0;
		}
		public final void SetInUse(int localId, String global) {
		}

		private HashMap<ByteBuffer, DataWithVersion> DataWithVersions = new HashMap<>();

		public DataWithVersion GetDataWithVersion(ByteBuffer key) {
			synchronized (DataWithVersions) {
				return DataWithVersions.get(key);
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

	public static class MemTrans implements Transaction {
		public MemTrans(String DatabaseUrl) {
		}
		public final void Commit() {
		}
		public final void Rollback() {
		}
		public final void close() {			
		}
	}

	@Override
	public Transaction BeginTransaction() {
		return new MemTrans(getDatabaseUrl());
	}

	private static ConcurrentHashMap<String, ConcurrentHashMap<String, TableMemory>> databaseTables = new ConcurrentHashMap<>();

	@Override
	public Database.Table OpenTable(String name) {
		var tables = databaseTables.computeIfAbsent(getDatabaseUrl(),
				(urlnotused) -> new java.util.concurrent.ConcurrentHashMap<String, TableMemory>());

		return tables.computeIfAbsent(name, (tablenamenotused) -> new TableMemory(this, name));
	}

	public final static class TableMemory implements Database.Table {
		private DatabaseMemory DatabaseReal;
		public DatabaseMemory getDatabaseReal() {
			return DatabaseReal;
		}
		public Database getDatabase() {
			return getDatabaseReal();
		}
		private String Name;
		public String getName() {
			return Name;
		}

		public TableMemory(DatabaseMemory db, String name) {
			DatabaseReal = db;
			Name = name;
		}

		private ConcurrentHashMap<ByteBuffer, byte[]> Map = new ConcurrentHashMap<>();

		public ByteBuffer Find(ByteBuffer key) {
			var value = Map.get(key);
			if (null != value)
				return ByteBuffer.Wrap(value);
			return null;
		}

		public void Remove(Transaction t, ByteBuffer key) {
			Map.remove(key);
		}

		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			Map.put(ByteBuffer.Wrap(key.Copy()), value.Copy());
		}

		public long Walk(TableWalkHandleRaw callback) {
			synchronized (this) {
				// 不允许并发？
				long count = 0;
				for (var e : Map.entrySet()) {
					++count;
					if (false == callback.handle(e.getKey().Bytes, e.getValue())) {
						break;
					}
				}
				return count;
			}
		}

		public void Close() {
		}
	}
}