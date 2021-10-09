package Zeze.Tikv;

import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.io.*;

public final class DatabaseTikv extends Database {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	public DatabaseTikv(String databaseUrl) {
		super(databaseUrl);
		setDirectOperates(new OperatesTikv(this));
	}

	public static class TikvTrans implements Database.Transaction {
		private TikvConnection Connection;
		public final TikvConnection getConnection() {
			return Connection;
		}
		private TikvTransaction Transaction;
		public final TikvTransaction getTransaction() {
			return Transaction;
		}

		public TikvTrans(String DatabaseUrl) {
			Connection = new TikvConnection(DatabaseUrl);
			getConnection().Open();
			Transaction = getConnection().BeginTransaction();
		}

		public final void close() throws IOException {
			getTransaction().close();
			getConnection().close();
		}


		public final void Commit() {
			getTransaction().Commit();
		}

		public final void Rollback() {
			getTransaction().Rollback();
		}
	}

	@Override
	public Transaction BeginTransaction() {
		return new TikvTrans(getDatabaseUrl());
	}

	@Override
	public Table OpenTable(String name) {
		return new TableTikv(this, name);
	}

	public final static class OperatesTikv implements Operates {
		private DatabaseTikv Database;
		public DatabaseTikv getDatabase() {
			return Database;
		}

		public OperatesTikv(DatabaseTikv tikv) {
			Database = tikv;
		}

		public int ClearInUse(int localId, String global) {
			return 0;
		}

		public void SetInUse(int localId, String global) {
		}

//C# TO JAVA CONVERTER TODO TASK: Methods returning tuples are not converted by C# to Java Converter:
//		public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key)
//			{
//				return (null, 0);
//			}

		public boolean SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, tangible.RefObject<Long> version) {
			return true;
		}
	}

	public final static class TableTikv implements Database.Table {
		private DatabaseTikv DatabaseReal;
		public DatabaseTikv getDatabaseReal() {
			return DatabaseReal;
		}
		public Database getDatabase() {
			return getDatabaseReal();
		}
		private String Name;
		public String getName() {
			return Name;
		}
		private ByteBuffer KeyPrefix;
		public ByteBuffer getKeyPrefix() {
			return KeyPrefix;
		}

		public TableTikv(DatabaseTikv database, String name) {
			DatabaseReal = database;
			Name = name;
			var nameutf8 = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
			KeyPrefix = ByteBuffer.Allocate(nameutf8.Length + 1);
			getKeyPrefix().Append(nameutf8);
			getKeyPrefix().WriteByte((byte)((byte)0));
		}

		public void Close() {
		}

		private ByteBuffer WithKeyspace(ByteBuffer key) {
			var tikvKey = ByteBuffer.Allocate(getKeyPrefix().getSize() + key.getSize());
			tikvKey.Append(getKeyPrefix().getBytes(), getKeyPrefix().getReadIndex(), getKeyPrefix().getSize());
			tikvKey.Append(key.getBytes(), key.getReadIndex(), key.getSize());
			return tikvKey;
		}

		public ByteBuffer Find(ByteBuffer key) {
			try (TikvConnection connection = new TikvConnection(getDatabaseReal().getDatabaseUrl())) {
				connection.Open();
				try (TikvTransaction transaction = connection.BeginTransaction()) {
					var result = Tikv.Driver.Get(transaction.getTransactionId(), WithKeyspace(key));
					transaction.Commit();
					return result;
				}
			}
		}

		public void Remove(Transaction t, ByteBuffer key) {
			var my = t instanceof TikvTrans ? (TikvTrans)t : null;
			Tikv.Driver.Delete(my.getConnection().getTransaction().getTransactionId(), WithKeyspace(key));
		}

		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			var my = t instanceof TikvTrans ? (TikvTrans)t : null;
			Tikv.Driver.Put(my.getConnection().getTransaction().getTransactionId(), WithKeyspace(key), value);
		}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public long Walk(Func<byte[], byte[], bool> callback)
		public long Walk(tangible.Func2Param<byte[], byte[], Boolean> callback) {
			try (TikvConnection connection = new TikvConnection(getDatabaseReal().getDatabaseUrl())) {
				connection.Open();
				try (TikvTransaction transaction = connection.BeginTransaction()) {
					long result = Tikv.Driver.Scan(transaction.getTransactionId(), getKeyPrefix(), callback);
					transaction.Commit();
					return result;
				}
			}
		}
	}
}