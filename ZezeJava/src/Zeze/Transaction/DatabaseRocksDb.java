package Zeze.Transaction;

import Zeze.Serialize.*;
import MySql.Data.MySqlClient.*;
import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public class DatabaseRocksDb extends Database {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	public DatabaseRocksDb(String url) {
		super(url);
		setDirectOperates(new OperatesRocksDb(this));
	}

	public static class RockdsDbTrans implements Transaction {
		public RockdsDbTrans(String DatabaseUrl) {
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
		return new RockdsDbTrans(getDatabaseUrl());
	}

	@Override
	public Table OpenTable(String name) {
		return new TableRocksDb(this, name);
	}

	public final static class TableRocksDb implements Database.Table {
		private DatabaseRocksDb DatabaseReal;
		public DatabaseRocksDb getDatabaseReal() {
			return DatabaseReal;
		}
		public Database getDatabase() {
			return getDatabaseReal();
		}
		private String Name;
		public String getName() {
			return Name;
		}
		private RocksDbSharp.RocksDb RocksDb;
		private RocksDbSharp.RocksDb getRocksDb() {
			return RocksDb;
		}
		private void setRocksDb(RocksDbSharp.RocksDb value) {
			RocksDb = value;
		}

		public TableRocksDb(DatabaseRocksDb database, String name) {
			DatabaseReal = database;
			Name = name;

			var options = (new RocksDbSharp.DbOptions()).SetCreateIfMissing(true);
			var path = Paths.get(getDatabaseReal().getDatabaseUrl()).resolve(name).toString();
			setRocksDb(RocksDbSharp.RocksDb.Open(options, path));
		}

		public void Close() {
			synchronized (this) {
				if (getRocksDb() != null) {
					getRocksDb().Dispose();
				}
				setRocksDb(null);
			}
		}

//C# TO JAVA CONVERTER TODO TASK: Methods returning tuples are not converted by C# to Java Converter:
//		private (byte[], int) GetBytes(ByteBuffer bb)
//			{
//				byte[] bytes;
//				int byteslen;
//				if (bb.ReadIndex == 0)
//				{
//					bytes = bb.Bytes;
//					byteslen = bb.Size;
//				}
//				else
//				{
//					bytes = bb.Copy();
//					byteslen = bytes.Length;
//				}
//				return (bytes, byteslen);
//			}

		public ByteBuffer Find(ByteBuffer _key) {
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# deconstruction declarations:
			var(key, keylen) = GetBytes(_key);
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] value = RocksDb.Get(key, keylen);
			byte[] value = getRocksDb().Get(key, keylen);
			if (null == value) {
				return null;
			}
			return ByteBuffer.Wrap(value);
		}

		public void Remove(Transaction t, ByteBuffer _key) {
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# deconstruction declarations:
			var(key, keylen) = GetBytes(_key);
			getRocksDb().Remove(key, keylen);
			// 多表 Transaction TODO
		}

		public void Replace(Transaction t, ByteBuffer _key, ByteBuffer _value) {
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# deconstruction declarations:
			var(key, keylen) = GetBytes(_key);
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# deconstruction declarations:
			var(value, valuelen) = GetBytes(_value);
			getRocksDb().Put(key, keylen, value, valuelen);
			// 多表 Transaction TODO
		}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public long Walk(Func<byte[], byte[], bool> callback)
		public long Walk(tangible.Func2Param<byte[], byte[], Boolean> callback) {
			long countWalked = 0;
			try (var it = getRocksDb().NewIterator()) {
				it.SeekToFirst();
				while (it.Valid()) {
					++countWalked;
					if (false == callback.invoke(it.Key(), it.Value())) {
						return countWalked;
					}
					it.Next();
				}
				return countWalked;
			}
		}
	}

	public final static class OperatesRocksDb implements Operates {
		private DatabaseRocksDb DatabaseReal;
		public DatabaseRocksDb getDatabaseReal() {
			return DatabaseReal;
		}
		public Database getDatabase() {
			return getDatabaseReal();
		}

		public OperatesRocksDb(DatabaseRocksDb database) {
			DatabaseReal = database;
		}

		public int ClearInUse(int localId, String global) {
			return 0;
		}

//C# TO JAVA CONVERTER TODO TASK: Methods returning tuples are not converted by C# to Java Converter:
//		public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key)
//			{
//				return (null, 0);
//			}

		public boolean SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, tangible.RefObject<Long> version) {
			return true;
		}

		public void SetInUse(int localId, String global) {
		}
	}
}