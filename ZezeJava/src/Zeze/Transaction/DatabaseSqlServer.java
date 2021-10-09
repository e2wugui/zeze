package Zeze.Transaction;

import Zeze.Serialize.*;
import MySql.Data.MySqlClient.*;
import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

public final class DatabaseSqlServer extends Database {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	public DatabaseSqlServer(String url) {
		super(url);
		setDirectOperates(new OperatesSqlServer(this));
	}

	public static class SqlTrans implements Transaction {
		private SqlConnection Connection;
		public final SqlConnection getConnection() {
			return Connection;
		}
		private SqlTransaction Transaction;
		public final SqlTransaction getTransaction() {
			return Transaction;
		}

		public SqlTrans(String DatabaseUrl) {
			Connection = new SqlConnection(DatabaseUrl);
			getConnection().Open();
			Transaction = getConnection().BeginTransaction();
		}

		public final void Dispose() {
			getTransaction().Dispose();
			getConnection().Dispose();
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
		return new SqlTrans(getDatabaseUrl());
	}

	@Override
	public Database.Table OpenTable(String name) {
		return new TableSqlServer(this, name);
	}

	public final static class OperatesSqlServer implements Operates {
		private DatabaseSqlServer DatabaseReal;
		public DatabaseSqlServer getDatabaseReal() {
			return DatabaseReal;
		}
		public Database getDatabase() {
			return getDatabaseReal();
		}

		public void SetInUse(int localId, String global) {
			try (SqlConnection connection = new SqlConnection(getDatabaseReal().getDatabaseUrl())) {
				connection.Open();
				SqlCommand cmd = new SqlCommand("_ZezeSetInUse_", connection);
				cmd.CommandType = CommandType.StoredProcedure;
				cmd.Parameters.Add("@localid", SqlDbType.Int).Value = localId;
				cmd.Parameters.Add("@global", SqlDbType.VarBinary, Integer.MAX_VALUE).Value = global.getBytes(java.nio.charset.StandardCharsets.UTF_8);
				SqlParameter tempVar = new SqlParameter("@ReturnValue", SqlDbType.Int);
				tempVar.Direction = ParameterDirection.Output;
				cmd.Parameters.Add(tempVar);
				cmd.Prepare();
				cmd.ExecuteNonQuery();
				switch ((int)cmd.Parameters["@ReturnValue"].Value) {
					case 0:
						return;
					case 1:
						throw new RuntimeException("Unknown Error");
					case 2:
						throw new RuntimeException("Instance Exist");
					case 3:
						throw new RuntimeException("Insert LocalId Faild");
					case 4:
						throw new RuntimeException("Global Not Equals");
					case 5:
						throw new RuntimeException("Insert Global Faild");
					case 6:
						throw new RuntimeException("Instance Greater Than One But No Global");
					default:
						throw new RuntimeException("Unknown ReturnValue");
				}
    
			}
		}

		public int ClearInUse(int localId, String global) {
			try (SqlConnection connection = new SqlConnection(getDatabaseReal().getDatabaseUrl())) {
				connection.Open();
				SqlCommand cmd = new SqlCommand("_ZezeClearInUse_", connection);
				cmd.CommandType = CommandType.StoredProcedure;
				cmd.Parameters.Add("@localid", SqlDbType.Int).Value = localId;
				cmd.Parameters.Add("@global", SqlDbType.VarBinary, Integer.MAX_VALUE).Value = global.getBytes(java.nio.charset.StandardCharsets.UTF_8);
				SqlParameter tempVar = new SqlParameter("@ReturnValue", SqlDbType.Int);
				tempVar.Direction = ParameterDirection.Output;
				cmd.Parameters.Add(tempVar);
				cmd.Prepare();
				cmd.ExecuteNonQuery();
				// Clear 不报告错误，直接返回。
				return (int)cmd.Parameters["@ReturnValue"].Value;
			}
		}

//C# TO JAVA CONVERTER TODO TASK: Methods returning tuples are not converted by C# to Java Converter:
//		public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key)
//			{
//				using SqlConnection connection = new SqlConnection(DatabaseReal.DatabaseUrl);
//				connection.Open();
//
//				string sql = "SELECT data,version FROM _ZezeDataWithVersion_ WHERE id=@id";
//
//				SqlCommand cmd = new SqlCommand(sql, connection);
//				cmd.Parameters.Add("@id", SqlDbType.VarBinary, 767).Value = key.Copy();
//				cmd.Prepare();
//
//				using (SqlDataReader reader = cmd.ExecuteReader())
//				{
//					while (reader.Read())
//					{
//						byte[] value = (byte[])reader[0];
//						long version = reader.GetInt64(1);
//						return (ByteBuffer.Wrap(value), version);
//					}
//					return (null, 0);
//				}
//			}

		public boolean SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, tangible.RefObject<Long> version) {
			if (key.getSize() == 0) {
				throw new RuntimeException("key is empty.");
			}

			try (SqlConnection connection = new SqlConnection(getDatabaseReal().getDatabaseUrl())) {
				connection.Open();
				SqlCommand cmd = new SqlCommand("_ZezeSaveDataWithSameVersion_", connection);
				cmd.CommandType = CommandType.StoredProcedure;
				cmd.Parameters.Add("@id", SqlDbType.VarBinary, 767).Value = key.Copy();
				cmd.Parameters.Add("@data", SqlDbType.VarBinary, Integer.MAX_VALUE).Value = data.Copy();
				SqlParameter tempVar = new SqlParameter("@version", SqlDbType.BigInt);
				tempVar.Direction = ParameterDirection.InputOutput;
				tempVar.Value = version.refArgValue;
				cmd.Parameters.Add(tempVar);
				SqlParameter tempVar2 = new SqlParameter("@ReturnValue", SqlDbType.Int);
				tempVar2.Direction = ParameterDirection.Output;
				cmd.Parameters.Add(tempVar2);
				cmd.Prepare();
				cmd.ExecuteNonQuery();
				switch ((int)cmd.Parameters["@ReturnValue"].Value) {
					case 0:
						version.refArgValue = (long)cmd.Parameters["@version"].Value;
						return true;
					case 2:
						return false;
					default:
						throw new RuntimeException("Procedure SaveDataWithSameVersion Exec Error.");
				}
			}
		}

		public OperatesSqlServer(DatabaseSqlServer database) {
			DatabaseReal = database;

			try (SqlConnection connection = new SqlConnection(getDatabaseReal().getDatabaseUrl())) {
				connection.Open();
    
				String TableDataWithVersion = "if not exists (select * from sysobjects where name='_ZezeDataWithVersion_' and xtype='U')" + " CREATE TABLE _ZezeDataWithVersion_ (id VARBINARY(767) NOT NULL PRIMARY KEY, data VARBINARY(MAX) NOT NULL, version bigint NOT NULL)";
				(new SqlCommand(TableDataWithVersion, connection)).ExecuteNonQuery();
    
				String ProcSaveDataWithSameVersion = "Create or Alter procedure _ZezeSaveDataWithSameVersion_" + "\r\n" + 
"                        @id VARBINARY(767)," + "\r\n" + 
"                        @data VARBINARY(MAX)," + "\r\n" + 
"                        @version bigint output," + "\r\n" + 
"                        @ReturnValue int output" + "\r\n" + 
"                    as" + "\r\n" + 
"                    begin" + "\r\n" + 
"                        BEGIN TRANSACTION" + "\r\n" + 
"                        set @ReturnValue=1" + "\r\n" + 
"                        DECLARE @currentversion bigint" + "\r\n" + 
"                        select @currentversion=version from _ZezeDataWithVersion_ where id = @id" + "\r\n" + 
"                        if @@ROWCOUNT > 0" + "\r\n" + 
"                        begin" + "\r\n" + 
"                            if @currentversion <> @version" + "\r\n" + 
"                            begin" + "\r\n" + 
"                                set @ReturnValue=2" + "\r\n" + 
"                                ROLLBACK TRANSACTION" + "\r\n" + 
"                                return 2" + "\r\n" + 
"                            end" + "\r\n" + 
"                            set @currentversion = @currentversion + 1" + "\r\n" + 
"                            update _ZezeDataWithVersion_ set data = @data, version = @currentversion where id = @id" + "\r\n" + 
"                            if @@rowcount = 1" + "\r\n" + 
"                            begin" + "\r\n" + 
"                                set @version = @currentversion" + "\r\n" + 
"                                set @ReturnValue=0" + "\r\n" + 
"                                COMMIT TRANSACTION" + "\r\n" + 
"                                return 0" + "\r\n" + 
"                            end" + "\r\n" + 
"                            set @ReturnValue=3" + "\r\n" + 
"                            ROLLBACK TRANSACTION" + "\r\n" + 
"                            return 3" + "\r\n" + 
"                        end" + "\r\n" + 
"\r\n" + 
"                        insert into _ZezeDataWithVersion_ values(@id,@data,@version)" + "\r\n" + 
"                        if @@rowcount = 1" + "\r\n" + 
"                        begin" + "\r\n" + 
"                            set @ReturnValue=0" + "\r\n" + 
"                            COMMIT TRANSACTION" + "\r\n" + 
"                            return 0" + "\r\n" + 
"                        end" + "\r\n" + 
"                        set @ReturnValue=4" + "\r\n" + 
"                        ROLLBACK TRANSACTION" + "\r\n" + 
"                        return 4" + "\r\n" + 
"                    end";
				(new SqlCommand(ProcSaveDataWithSameVersion, connection)).ExecuteNonQuery();
    
				String TableInstances = "if not exists (select * from sysobjects where name='_ZezeInstances_' and xtype='U')" + " CREATE TABLE _ZezeInstances_ (localid int NOT NULL PRIMARY KEY)";
				(new SqlCommand(TableInstances, connection)).ExecuteNonQuery();
				// zeze_global 使用 _ZezeDataWithVersion_ 存储。
    
				String ProcSetInUse = "Create or Alter procedure _ZezeSetInUse_" + "\r\n" + 
"                        @localid int," + "\r\n" + 
"                        @global VARBINARY(MAX)," + "\r\n" + 
"                        @ReturnValue int output" + "\r\n" + 
"                    as" + "\r\n" + 
"                    begin" + "\r\n" + 
"                        BEGIN TRANSACTION" + "\r\n" + 
"                        set @ReturnValue=1" + "\r\n" + 
"                        if exists (select localid from _ZezeInstances_ where localid = @localid)" + "\r\n" + 
"                        begin" + "\r\n" + 
"                            set @ReturnValue=2" + "\r\n" + 
"                            ROLLBACK TRANSACTION" + "\r\n" + 
"                            return 2" + "\r\n" + 
"                        end" + "\r\n" + 
"                        insert into _ZezeInstances_ values(@localid)" + "\r\n" + 
"                        if @@rowcount = 0" + "\r\n" + 
"                        begin" + "\r\n" + 
"                            set @ReturnValue=3" + "\r\n" + 
"                            ROLLBACK TRANSACTION" + "\r\n" + 
"                            return 3" + "\r\n" + 
"                        end" + "\r\n" + 
"                        DECLARE @currentglobal VARBINARY(MAX)" + "\r\n" + 
"                        declare @emptybinary varbinary(max)" + "\r\n" + 
"                        set @emptybinary = convert(varbinary(max), '')" + "\r\n" + 
"                        select @currentglobal=data from _ZezeDataWithVersion_ where id=@emptybinary" + "\r\n" + 
"                        if @@rowcount > 0" + "\r\n" + 
"                        begin" + "\r\n" + 
"                            if @currentglobal <> @global" + "\r\n" + 
"                            begin" + "\r\n" + 
"                                set @ReturnValue=4" + "\r\n" + 
"                                ROLLBACK TRANSACTION" + "\r\n" + 
"                                return 4" + "\r\n" + 
"                            end" + "\r\n" + 
"                        end" + "\r\n" + 
"                        else" + "\r\n" + 
"                        begin" + "\r\n" + 
"                            insert into _ZezeDataWithVersion_ values(@emptybinary, @global, 0)" + "\r\n" + 
"                            if @@rowcount <> 1" + "\r\n" + 
"                            begin" + "\r\n" + 
"                                set @ReturnValue=5" + "\r\n" + 
"                                ROLLBACK TRANSACTION" + "\r\n" + 
"                                return 5" + "\r\n" + 
"                            end" + "\r\n" + 
"                        end" + "\r\n" + 
"                        DECLARE @InstanceCount int" + "\r\n" + 
"                        set @InstanceCount=0" + "\r\n" + 
"                        select @InstanceCount=count(*) from _ZezeInstances_" + "\r\n" + 
"                        if @InstanceCount = 1" + "\r\n" + 
"                        begin" + "\r\n" + 
"                            set @ReturnValue=0" + "\r\n" + 
"                            COMMIT TRANSACTION" + "\r\n" + 
"                            return 0" + "\r\n" + 
"                        end" + "\r\n" + 
"                        if DATALENGTH(@global)=0" + "\r\n" + 
"                        begin" + "\r\n" + 
"                            set @ReturnValue=6" + "\r\n" + 
"                            ROLLBACK TRANSACTION" + "\r\n" + 
"                            return 6" + "\r\n" + 
"                        end" + "\r\n" + 
"                        set @ReturnValue=0" + "\r\n" + 
"                        COMMIT TRANSACTION" + "\r\n" + 
"                        return 0" + "\r\n" + 
"                    end";
				(new SqlCommand(ProcSetInUse, connection)).ExecuteNonQuery();
    
				String ProcClearInUse = "Create or Alter procedure _ZezeClearInUse_" + "\r\n" + 
"                        @localid int," + "\r\n" + 
"                        @global VARBINARY(MAX)," + "\r\n" + 
"                        @ReturnValue int output" + "\r\n" + 
"                    as" + "\r\n" + 
"                    begin" + "\r\n" + 
"                        BEGIN TRANSACTION" + "\r\n" + 
"                        set @ReturnValue=1" + "\r\n" + 
"                        delete from _ZezeInstances_ where localid=@localid" + "\r\n" + 
"                        if @@rowcount = 0" + "\r\n" + 
"                        begin" + "\r\n" + 
"                            set @ReturnValue=2" + "\r\n" + 
"                            ROLLBACK TRANSACTION" + "\r\n" + 
"                            return 2" + "\r\n" + 
"                        end" + "\r\n" + 
"                        DECLARE @InstanceCount int" + "\r\n" + 
"                        set @InstanceCount=0" + "\r\n" + 
"                        select @InstanceCount=count(*) from _ZezeInstances_" + "\r\n" + 
"                        if @InstanceCount = 0" + "\r\n" + 
"                        begin" + "\r\n" + 
"                            declare @emptybinary varbinary(max)" + "\r\n" + 
"                            set @emptybinary = convert(varbinary(max), '')" + "\r\n" + 
"                            delete from _ZezeDataWithVersion_ where id=@emptybinary" + "\r\n" + 
"                        end" + "\r\n" + 
"                        set @ReturnValue=0" + "\r\n" + 
"                        COMMIT TRANSACTION" + "\r\n" + 
"                        return 0" + "\r\n" + 
"                    end";
				(new SqlCommand(ProcClearInUse, connection)).ExecuteNonQuery();
			}
		}
	}

	public final static class TableSqlServer implements Database.Table {
		private DatabaseSqlServer DatabaseReal;
		public DatabaseSqlServer getDatabaseReal() {
			return DatabaseReal;
		}
		public Database getDatabase() {
			return getDatabaseReal();
		}
		private String Name;
		public String getName() {
			return Name;
		}

		public TableSqlServer(DatabaseSqlServer database, String name) {
			DatabaseReal = database;
			Name = name;

			try (SqlConnection connection = new SqlConnection(getDatabaseReal().getDatabaseUrl())) {
				connection.Open();
    
				String sql = "if not exists (select * from sysobjects where name='" + getName() + "' and xtype='U') CREATE TABLE " + getName() + "(id VARBINARY(767) NOT NULL PRIMARY KEY, value VARBINARY(MAX) NOT NULL)";
				SqlCommand cmd = new SqlCommand(sql, connection);
				cmd.ExecuteNonQuery();
			}
		}

		public void Close() {
		}

		public ByteBuffer Find(ByteBuffer key) {
			try (SqlConnection connection = new SqlConnection(getDatabaseReal().getDatabaseUrl())) {
				connection.Open();
    
				String sql = "SELECT value FROM " + getName() + " WHERE id = @ID";
    
				// 是否可以重用 SqlCommand
				SqlCommand cmd = new SqlCommand(sql, connection);
				cmd.Parameters.Add("@ID", System.Data.SqlDbType.VarBinary, 767).Value = key.Copy();
				cmd.Prepare();
    
				try (SqlDataReader reader = cmd.ExecuteReader()) {
					while (reader.Read()) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] value = (byte[])reader[0];
						byte[] value = (byte[])reader[0];
						return ByteBuffer.Wrap(value);
					}
					return null;
				}
			}
		}

		public void Remove(Transaction t, ByteBuffer key) {
			var my = t instanceof SqlTrans ? (SqlTrans)t : null;
			String sql = "DELETE FROM " + getName() + " WHERE id=@ID";
			SqlCommand cmd = new SqlCommand(sql, my.getConnection(), my.getTransaction());
			cmd.Parameters.Add("@ID", System.Data.SqlDbType.VarBinary, 767).Value = key.Copy();
			cmd.Prepare();
			cmd.ExecuteNonQuery();
		}

		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			var my = t instanceof SqlTrans ? (SqlTrans)t : null;
			String sql = "update " + getName() + " set value=@VALUE where id=@ID" + " if @@rowcount = 0 and @@error = 0 insert into " + getName() + " values(@ID,@VALUE)";

			SqlCommand cmd = new SqlCommand(sql, my.getConnection(), my.getTransaction());
			cmd.Parameters.Add("@ID", System.Data.SqlDbType.VarBinary, 767).Value = key.Copy();
			cmd.Parameters.Add("@VALUE", System.Data.SqlDbType.VarBinary, Integer.MAX_VALUE).Value = value.Copy();
			cmd.Prepare();
			cmd.ExecuteNonQuery();
		}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public long Walk(Func<byte[], byte[], bool> callback)
		public long Walk(tangible.Func2Param<byte[], byte[], Boolean> callback) {
			try (SqlConnection connection = new SqlConnection(getDatabaseReal().getDatabaseUrl())) {
				connection.Open();
    
				String sql = "SELECT id,value FROM " + getName();
				SqlCommand cmd = new SqlCommand(sql, connection);
				cmd.Prepare();
    
				long count = 0;
				try (SqlDataReader reader = cmd.ExecuteReader()) {
					while (reader.Read()) {
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] key = (byte[])reader[0];
						byte[] key = (byte[])reader[0];
//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: byte[] value = (byte[])reader[1];
						byte[] value = (byte[])reader[1];
						++count;
						if (false == callback.invoke(key, value)) {
							break;
						}
					}
				}
				return count;
			}
		}
	}
}