package Zeze.Transaction;

import Zeze.Serialize.*;
import MySql.Data.MySqlClient.*;
import Zeze.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;

//C# TO JAVA CONVERTER TODO TASK: There is no preprocessor in Java:
//#if USE_DATABASE
public final class DatabaseMySql extends Database {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	public DatabaseMySql(String url) {
		super(url);
		setDirectOperates(new OperatesMySql(this));
	}

	public static class MySqlTrans implements Transaction {
		private MySqlConnection Connection;
		public final MySqlConnection getConnection() {
			return Connection;
		}
		private MySqlTransaction Transaction;
		public final MySqlTransaction getTransaction() {
			return Transaction;
		}

		public MySqlTrans(String DatabaseUrl) {
			Connection = new MySqlConnection(DatabaseUrl);
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
		return new MySqlTrans(getDatabaseUrl());
	}

	@Override
	public Database.Table OpenTable(String name) {
		return new TableMysql(this, name);
	}

	public final static class OperatesMySql implements Operates {
		private DatabaseMySql Database;
		public DatabaseMySql getDatabase() {
			return Database;
		}

		public void SetInUse(int localId, String global) {
			try (MySqlConnection connection = new MySqlConnection(getDatabase().getDatabaseUrl())) {
				connection.Open();
				MySqlCommand cmd = new MySqlCommand("_ZezeSetInUse_", connection);
				cmd.CommandType = CommandType.StoredProcedure;
				cmd.Parameters.Add("@in_localid", MySqlDbType.Int32).Value = localId;
				cmd.Parameters.Add("@in_global", MySqlDbType.VarBinary, Integer.MAX_VALUE).Value = global.getBytes(java.nio.charset.StandardCharsets.UTF_8);
				MySqlParameter tempVar = new MySqlParameter("@ReturnValue", MySqlDbType.Int32);
				tempVar.Direction = ParameterDirection.Output;
				cmd.Parameters.add(tempVar);
				cmd.Prepare();
				cmd.ExecuteNonQuery();
				switch ((int)cmd.Parameters["@ReturnValue"].Value) {
					case 0:
						return;
					case 1:
						throw new RuntimeException("Unknown Error");
					case 2:
						throw new RuntimeException("Instance Exist.");
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
			try (MySqlConnection connection = new MySqlConnection(getDatabase().getDatabaseUrl())) {
				connection.Open();
				MySqlCommand cmd = new MySqlCommand("_ZezeClearInUse_", connection);
				cmd.CommandType = CommandType.StoredProcedure;
				cmd.Parameters.Add("@in_localid", MySqlDbType.Int32).Value = localId;
				cmd.Parameters.Add("@in_global", MySqlDbType.VarBinary, Integer.MAX_VALUE).Value = global.getBytes(java.nio.charset.StandardCharsets.UTF_8);
				MySqlParameter tempVar = new MySqlParameter("@ReturnValue", MySqlDbType.Int32);
				tempVar.Direction = ParameterDirection.Output;
				cmd.Parameters.add(tempVar);
				cmd.Prepare();
				cmd.ExecuteNonQuery();
				// Clear 不报告错误，直接返回。
				return (int)cmd.Parameters["@ReturnValue"].Value;
			}
		}

//C# TO JAVA CONVERTER TODO TASK: Methods returning tuples are not converted by C# to Java Converter:
//		public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key)
//			{
//				using MySqlConnection connection = new MySqlConnection(Database.DatabaseUrl);
//				connection.Open();
//
//				string sql = "SELECT data,version FROM _ZezeDataWithVersion_ WHERE id=@id";
//
//				MySqlCommand cmd = new MySqlCommand(sql, connection);
//				cmd.Parameters.Add("@id", MySqlDbType.VarBinary, 767).Value = key.Copy();
//				cmd.Prepare();
//
//				using (MySqlDataReader reader = cmd.ExecuteReader())
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

			try (MySqlConnection connection = new MySqlConnection(getDatabase().getDatabaseUrl())) {
				connection.Open();
				MySqlCommand cmd = new MySqlCommand("_ZezeSaveDataWithSameVersion_", connection);
				cmd.CommandType = CommandType.StoredProcedure;
				cmd.Parameters.Add("@in_id", MySqlDbType.VarBinary, 767).Value = key.Copy();
				cmd.Parameters.Add("@in_data", MySqlDbType.VarBinary, Integer.MAX_VALUE).Value = data.Copy();
				MySqlParameter tempVar = new MySqlParameter("@inout_version", MySqlDbType.Int64);
				tempVar.Direction = ParameterDirection.InputOutput;
				tempVar.Value = version.refArgValue;
				cmd.Parameters.add(tempVar);
				MySqlParameter tempVar2 = new MySqlParameter("@ReturnValue", MySqlDbType.Int32);
				tempVar2.Direction = ParameterDirection.Output;
				cmd.Parameters.add(tempVar2);
				cmd.Prepare();
				cmd.ExecuteNonQuery();
				switch ((int)cmd.Parameters["@ReturnValue"].Value) {
					case 0:
						version.refArgValue = (long)cmd.Parameters["@inout_version"].Value;
						return true;
					case 2:
						return false;
					default:
						throw new RuntimeException("Procedure SaveDataWithSameVersion Exec Error.");
				}
			}
		}

		public OperatesMySql(DatabaseMySql database) {
			Database = database;

			try (MySqlConnection connection = new MySqlConnection(getDatabase().getDatabaseUrl())) {
				connection.Open();
    
				String TableDataWithVersion = "CREATE TABLE IF NOT EXISTS _ZezeDataWithVersion_ (" + "\r\n" + 
"                        id VARBINARY(767) NOT NULL PRIMARY KEY," + "\r\n" + 
"                        data MEDIUMBLOB NOT NULL," + "\r\n" + 
"                        version bigint NOT NULL" + "\r\n" + 
"                    )ENGINE=INNODB";
				(new MySqlCommand(TableDataWithVersion, connection)).ExecuteNonQuery();
    
				(new MySqlCommand("DROP PROCEDURE IF EXISTS _ZezeSaveDataWithSameVersion_", connection)).ExecuteNonQuery();
				String ProcSaveDataWithSameVersion = "Create procedure _ZezeSaveDataWithSameVersion_ (" + "\r\n" + 
"                        IN    in_id VARBINARY(767)," + "\r\n" + 
"                        IN    in_data MEDIUMBLOB," + "\r\n" + 
"                        INOUT inout_version bigint," + "\r\n" + 
"                        OUT   ReturnValue int" + "\r\n" + 
"                    )" + "\r\n" + 
"                    return_label:begin" + "\r\n" + 
"                        DECLARE oldversionexsit BIGINT;" + "\r\n" + 
"                        DECLARE ROWCOUNT int;" + "\r\n" + 
"\r\n" + 
"                        START TRANSACTION;" + "\r\n" + 
"                        set ReturnValue=1;" + "\r\n" + 
"                        select version INTO oldversionexsit from _ZezeDataWithVersion_ where id=in_id;" + "\r\n" + 
"                        select FOUND_ROWS() into ROWCOUNT;" + "\r\n" + 
"                        if ROWCOUNT > 0 then" + "\r\n" + 
"                            if oldversionexsit <> inout_version then" + "\r\n" + 
"                                set ReturnValue=2;" + "\r\n" + 
"                                ROLLBACK;" + "\r\n" + 
"                                LEAVE return_label;" + "\r\n" + 
"                            end if;" + "\r\n" + 
"                            set oldversionexsit = oldversionexsit + 1;" + "\r\n" + 
"                            update _ZezeDataWithVersion_ set data=in_data, version=oldversionexsit where id=in_id;" + "\r\n" + 
"                            select ROW_COUNT() into ROWCOUNT;" + "\r\n" + 
"                            if ROWCOUNT = 1 then" + "\r\n" + 
"                                set inout_version = oldversionexsit;" + "\r\n" + 
"                                set ReturnValue=0;" + "\r\n" + 
"                                COMMIT;" + "\r\n" + 
"                                LEAVE return_label;" + "\r\n" + 
"                            end if;" + "\r\n" + 
"                            set ReturnValue=3;" + "\r\n" + 
"                            ROLLBACK;" + "\r\n" + 
"                            LEAVE return_label;" + "\r\n" + 
"                        end if;" + "\r\n" + 
"\r\n" + 
"                        insert into _ZezeDataWithVersion_ values(in_id,in_data,inout_version);" + "\r\n" + 
"                        select ROW_COUNT() into ROWCOUNT;" + "\r\n" + 
"                        if ROWCOUNT = 1 then" + "\r\n" + 
"                            set ReturnValue=0;" + "\r\n" + 
"                            COMMIT;" + "\r\n" + 
"                            LEAVE return_label;" + "\r\n" + 
"                        end if;" + "\r\n" + 
"                        set ReturnValue=4;" + "\r\n" + 
"                        ROLLBACK;" + "\r\n" + 
"                        LEAVE return_label;" + "\r\n" + 
"                    end;";
				(new MySqlCommand(ProcSaveDataWithSameVersion, connection)).ExecuteNonQuery();
    
				String TableInstances = "CREATE TABLE IF NOT EXISTS _ZezeInstances_ (localid int NOT NULL PRIMARY KEY)ENGINE=INNODB";
				(new MySqlCommand(TableInstances, connection)).ExecuteNonQuery();
				// zeze_global 使用 _ZezeDataWithVersion_ 存储。
    
				(new MySqlCommand("DROP PROCEDURE IF EXISTS _ZezeSetInUse_", connection)).ExecuteNonQuery();
				String ProcSetInUse = "Create procedure _ZezeSetInUse_ (" + "\r\n" + 
"                        in in_localid int," + "\r\n" + 
"                        in in_global MEDIUMBLOB," + "\r\n" + 
"                        out ReturnValue int" + "\r\n" + 
"                    )" + "\r\n" + 
"                    return_label:begin" + "\r\n" + 
"                        DECLARE currentglobal MEDIUMBLOB;" + "\r\n" + 
"                        declare emptybinary MEDIUMBLOB;" + "\r\n" + 
"                        DECLARE InstanceCount int;" + "\r\n" + 
"                        DECLARE ROWCOUNT int;" + "\r\n" + 
"\r\n" + 
"                        START TRANSACTION;" + "\r\n" + 
"                        set ReturnValue=1;" + "\r\n" + 
"                        if exists (select localid from _ZezeInstances_ where localid=in_localid) then" + "\r\n" + 
"                            set ReturnValue=2;" + "\r\n" + 
"                            ROLLBACK;" + "\r\n" + 
"                            LEAVE return_label;" + "\r\n" + 
"                        end if;" + "\r\n" + 
"                        insert into _ZezeInstances_ values(in_localid);" + "\r\n" + 
"                        select ROW_COUNT() into ROWCOUNT;" + "\r\n" + 
"                        if ROWCOUNT = 0 then" + "\r\n" + 
"                            set ReturnValue=3;" + "\r\n" + 
"                            ROLLBACK;" + "\r\n" + 
"                            LEAVE return_label;" + "\r\n" + 
"                        end if;" + "\r\n" + 
"                        set emptybinary = BINARY '';" + "\r\n" + 
"                        select data into currentglobal from _ZezeDataWithVersion_ where id=emptybinary;" + "\r\n" + 
"                        select FOUND_ROWS() into ROWCOUNT;" + "\r\n" + 
"                        if ROWCOUNT > 0 then" + "\r\n" + 
"                            if currentglobal <> in_global then" + "\r\n" + 
"                                set ReturnValue=4;" + "\r\n" + 
"                                ROLLBACK;" + "\r\n" + 
"                                LEAVE return_label;" + "\r\n" + 
"                            end if;" + "\r\n" + 
"                        else" + "\r\n" + 
"                            insert into _ZezeDataWithVersion_ values(emptybinary, in_global, 0);" + "\r\n" + 
"                            select ROW_COUNT() into ROWCOUNT;" + "\r\n" + 
"                            if ROWCOUNT <> 1 then" + "\r\n" + 
"                                set ReturnValue=5;" + "\r\n" + 
"                                ROLLBACK;" + "\r\n" + 
"                                LEAVE return_label;" + "\r\n" + 
"                            end if;" + "\r\n" + 
"                        end if;" + "\r\n" + 
"                        set InstanceCount=0;" + "\r\n" + 
"                        select count(*) INTO InstanceCount from _ZezeInstances_;" + "\r\n" + 
"                        if InstanceCount = 1 then" + "\r\n" + 
"                            set ReturnValue=0;" + "\r\n" + 
"                            COMMIT;" + "\r\n" + 
"                            LEAVE return_label;" + "\r\n" + 
"                       end if;" + "\r\n" + 
"                       if LENGTH(in_global)=0 then" + "\r\n" + 
"                            set ReturnValue=6;" + "\r\n" + 
"                            ROLLBACK;" + "\r\n" + 
"                            LEAVE return_label;" + "\r\n" + 
"                        end if;" + "\r\n" + 
"                        set ReturnValue=0;" + "\r\n" + 
"                        COMMIT;" + "\r\n" + 
"                        LEAVE return_label;" + "\r\n" + 
"                    end";
				(new MySqlCommand(ProcSetInUse, connection)).ExecuteNonQuery();
    
				(new MySqlCommand("DROP PROCEDURE IF EXISTS _ZezeClearInUse_", connection)).ExecuteNonQuery();
				String ProcClearInUse = "Create procedure _ZezeClearInUse_ (" + "\r\n" + 
"                        in in_localid int," + "\r\n" + 
"                        in in_global MEDIUMBLOB," + "\r\n" + 
"                        out ReturnValue int" + "\r\n" + 
"                    )" + "\r\n" + 
"                    return_label:begin" + "\r\n" + 
"                        DECLARE InstanceCount int;" + "\r\n" + 
"                        declare emptybinary MEDIUMBLOB;" + "\r\n" + 
"                        DECLARE ROWCOUNT INT;" + "\r\n" + 
"\r\n" + 
"                        START TRANSACTION;" + "\r\n" + 
"                        set ReturnValue=1;" + "\r\n" + 
"                        delete from _ZezeInstances_ where localid=in_localid;" + "\r\n" + 
"                        select ROW_COUNT() into ROWCOUNT;" + "\r\n" + 
"                        if ROWCOUNT = 0 then" + "\r\n" + 
"                            set ReturnValue=2;" + "\r\n" + 
"                            ROLLBACK;" + "\r\n" + 
"                            LEAVE return_label;" + "\r\n" + 
"                        end if;" + "\r\n" + 
"                        set InstanceCount=0;" + "\r\n" + 
"                        select count(*) INTO InstanceCount from _ZezeInstances_;" + "\r\n" + 
"                        if InstanceCount = 0 then" + "\r\n" + 
"                            set emptybinary = BINARY '';" + "\r\n" + 
"                            delete from _ZezeDataWithVersion_ where id=emptybinary;" + "\r\n" + 
"                        end if;" + "\r\n" + 
"                        set ReturnValue=0;" + "\r\n" + 
"                        COMMIT;" + "\r\n" + 
"                        LEAVE return_label;" + "\r\n" + 
"                    end";
				(new MySqlCommand(ProcClearInUse, connection)).ExecuteNonQuery();
			}
		}
	}

	public final static class TableMysql implements Database.Table {
		private DatabaseMySql DatabaseReal;
		public DatabaseMySql getDatabaseReal() {
			return DatabaseReal;
		}
		public Database getDatabase() {
			return getDatabaseReal();
		}
		private String Name;
		public String getName() {
			return Name;
		}

		public TableMysql(DatabaseMySql database, String name) {
			DatabaseReal = database;
			Name = name;

			try (MySqlConnection connection = new MySqlConnection(database.getDatabaseUrl())) {
				connection.Open();
				String sql = "CREATE TABLE IF NOT EXISTS " + getName() + "(id VARBINARY(767) NOT NULL PRIMARY KEY, value MEDIUMBLOB NOT NULL)ENGINE=INNODB";
				MySqlCommand cmd = new MySqlCommand(sql, connection);
				cmd.ExecuteNonQuery();
			}
		}

		public void Close() {
		}

		public ByteBuffer Find(ByteBuffer key) {
			try (MySqlConnection connection = new MySqlConnection(getDatabaseReal().getDatabaseUrl())) {
				connection.Open();
    
				String sql = "SELECT value FROM " + getName() + " WHERE id = @ID";
				// 是否可以重用 SqlCommand
				MySqlCommand cmd = new MySqlCommand(sql, connection);
				cmd.Parameters.Add("@ID", MySqlDbType.VarBinary, 767).Value = key.Copy();
				cmd.Prepare();
    
				try (MySqlDataReader reader = cmd.ExecuteReader()) {
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
			var my = t instanceof MySqlTrans ? (MySqlTrans)t : null;
			String sql = "DELETE FROM " + getName() + " WHERE id=@ID";
			MySqlCommand cmd = new MySqlCommand(sql, my.getConnection(), my.getTransaction());
			cmd.Parameters.Add("@ID", MySqlDbType.VarBinary, 767).Value = key.Copy();
			cmd.Prepare();
			cmd.ExecuteNonQuery();
		}

		public void Replace(Transaction t, ByteBuffer key, ByteBuffer value) {
			var my = t instanceof MySqlTrans ? (MySqlTrans)t : null;
			String sql = "REPLACE INTO " + getName() + " values(@ID,@VALUE)";
			MySqlCommand cmd = new MySqlCommand(sql, my.getConnection(), my.getTransaction());
			cmd.Parameters.Add("@ID", MySqlDbType.VarBinary, 767).Value = key.Copy();
			cmd.Parameters.Add("@VALUE", MySqlDbType.VarBinary, Integer.MAX_VALUE).Value = value.Copy();
			cmd.Prepare();
			cmd.ExecuteNonQuery();
		}

//C# TO JAVA CONVERTER WARNING: Unsigned integer types have no direct equivalent in Java:
//ORIGINAL LINE: public long Walk(Func<byte[], byte[], bool> callback)
		public long Walk(tangible.Func2Param<byte[], byte[], Boolean> callback) {
			try (MySqlConnection connection = new MySqlConnection(getDatabaseReal().getDatabaseUrl())) {
				connection.Open();
    
				String sql = "SELECT id,value FROM " + getName();
				MySqlCommand cmd = new MySqlCommand(sql, connection);
				cmd.Prepare();
    
				long count = 0;
				try (MySqlDataReader reader = cmd.ExecuteReader()) {
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