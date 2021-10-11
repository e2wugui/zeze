package Zeze.Transaction;

import Zeze.Serialize.*;
import MySql.Data.MySqlClient.*;
import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

//#endif
/** 
 Zeze.Transaction.Table.storage 为 null 时，就表示内存表了。这个实现是为了测试 checkpoint 流程。
*/
public final class DatabaseMemory extends Database {
	public DatabaseMemory(String url) {
		super(url);
		setDirectOperates(new ProceduresMemory());
	}

	public static class ByteArrayComparer implements IComparetor<byte[]> {
		public final boolean equals(byte[] left, byte[] right) {
			return ByteBuffer.Equals(left, right);
		}

		public final int hashCode(byte[] key) {
			return ByteBuffer.calc_hashnr(key, 0, key.length);
		}
	}

	public static class ProceduresMemory implements Operates {
		public final int ClearInUse(int localId, String global) {
			return 0;
		}
		public final void SetInUse(int localId, String global) {
		}

		private final static class DataWithVersion {
			private ByteBuffer Data;
			public ByteBuffer getData() {
				return Data;
			}
			public void setData(ByteBuffer value) {
				Data = value;
			}
			private long Version;
			public long getVersion() {
				return Version;
			}
			public void setVersion(long value) {
				Version = value;
			}
		}

		private HashMap<byte[], DataWithVersion> DataWithVersions = new HashMap<>(Arrays.asList(new ByteArrayComparer()));

//C# TO JAVA CONVERTER TODO TASK: Methods returning tuples are not converted by C# to Java Converter:
//		public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key)
//			{
//				lock (DataWithVersions)
//				{
//					if (DataWithVersions.TryGetValue(key.Copy(), out var exist))
//					{
//						return (exist.Data, exist.Version);
//					}
//					return (null, 0);
//				}
//			}

		public final boolean SaveDataWithSameVersion(ByteBuffer _key, ByteBuffer data, tangible.RefObject<Long> version) {
			synchronized (DataWithVersions) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] key = _key.Copy();
				byte[] key = _key.Copy();
				TValue exist;
				if (DataWithVersions.containsKey(key) && (exist = DataWithVersions.get(key)) == exist) {
					if (exist.Version != version.refArgValue) {
						return false;
					}

					exist.Data = ByteBuffer.Wrap(data.Copy());
					++exist.Version;
					version.refArgValue = exist.Version;
					return true;
				}
				DataWithVersion tempVar = new DataWithVersion();
				tempVar.setData(ByteBuffer.Wrap(data.Copy()));
				tempVar.setVersion(version.refArgValue);
				DataWithVersions.put(key, tempVar);
				return true;
			}
		}
	}

	public static class MemTrans implements Transaction {
		public MemTrans(String DatabaseUrl) {

		}

		public final void Dispose() {

		}


		public final void Commit() {
		}

		public final void Rollback() {
		}
	}

	@Override
	public Transaction BeginTransaction() {
		return new MemTrans(getDatabaseUrl());
	}

	private static java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, TableMemory>> databaseTables = new java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, TableMemory>>();

	@Override
	public Database.Table OpenTable(String name) {
		var tables = databaseTables.putIfAbsent(getDatabaseUrl(), (urlnotused) -> new java.util.concurrent.ConcurrentHashMap<String, TableMemory>());

		return tables.GetOrAdd(name, (tablenamenotused) -> new TableMemory(this, name));
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

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: private ConcurrentDictionary<byte[], byte[]> Map = new ConcurrentDictionary<byte[], byte[]> (new ByteArrayComparer());
		private java.util.concurrent.ConcurrentHashMap<byte[], byte[]> Map = new java.util.concurrent.ConcurrentHashMap<byte[], byte[]> (new ByteArrayComparer());
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public ConcurrentDictionary<byte[], byte[]> getMap()
		public java.util.concurrent.ConcurrentHashMap<byte[], byte[]> getMap() {
			return Map;
		}

		public ByteBuffer Find(ByteBuffer key) {
			TValue value;
			tangible.OutObject<TValue> tempOut_value = new tangible.OutObject<TValue>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
			if (getMap().TryGetValue(key.Copy(), tempOut_value)) {
			value = tempOut_value.outArgValue;
				return ByteBuffer.Wrap(value);
			}
		else {
			value = tempOut_value.outArgValue;
		}
			return null;
		}

		public void Remove(Transaction t, ByteBuffer key) {
			TValue notused;
			tangible.OutObject<byte[]> tempOut_notused = new tangible.OutObject<byte[]>();
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: Map.TryRemove(key.Copy(), out var notused);
			getMap().TryRemove(key.Copy(), tempOut_notused);
		notused = tempOut_notused.outArgValue;
		}

		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			getMap().put(key.Copy(), value.Copy());
		}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public long Walk(Func<byte[], byte[], bool> callback)
		public long Walk(tangible.Func2Param<byte[], byte[], Boolean> callback) {
			synchronized (this) {
				// 不允许并发？
				long count = 0;
				for (var e : getMap()) {
					++count;
					if (false == callback.invoke(e.Key, e.Value)) {
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