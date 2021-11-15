using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using System.Collections.Concurrent;
#if USE_DATABASE
using System.Data.SqlClient;
using MySql.Data.MySqlClient;
using RocksDbSharp;
using System.Runtime.InteropServices;
using System.Data;
#endif // USE_DATABASE

namespace Zeze.Transaction
{
    /// <summary>
    /// 数据访问的效率主要来自TableCache的命中。根据以往的经验，命中率是很高的。
    /// 所以数据库层就不要求很高的效率。马马虎虎就可以了。
    /// </summary>
    public abstract class Database
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private Dictionary<string, Zeze.Transaction.Table> tables = new Dictionary<string, Zeze.Transaction.Table>();
        internal List<Storage> storages = new List<Storage>();
        internal ICollection<Zeze.Transaction.Table> Tables => tables.Values;

        public string DatabaseUrl { get; }
        public Application Zeze { get; }

        public Database(Application zeze, string url)
        {
            Zeze = zeze;
            this.DatabaseUrl = url;
        }

        public Zeze.Transaction.Table GetTable(string name)
        {
            if (tables.TryGetValue(name, out var table))
            {
                return table;
            }
            return null;
        }

        public abstract Transaction BeginTransaction();

        public void AddTable(Zeze.Transaction.Table table)
        {
            tables.Add(table.Name, table);
        }

        public void RemoveTable(Zeze.Transaction.Table table)
        {
            tables.Remove(table.Name);
        }

        public virtual void Open(Zeze.Application app)
        {
            foreach (Zeze.Transaction.Table table in tables.Values)
            {
                Storage storage = table.Open(app, this);
                if (null != storage)
                    storages.Add(storage);
            }
        }

        public virtual void Close()
        {
            foreach (Zeze.Transaction.Table table in tables.Values)
            {
                table.Close();
            }
            tables.Clear();
            storages.Clear();
        }

        internal void EncodeN()
        {
            // try Encode. 可以多趟。
            for (int i = 1; i <= 1; ++i)
            {
                int countEncodeN = 0;
                foreach (Storage storage in storages)
                {
                    countEncodeN += storage.EncodeN();
                }
                //logger.Info("Checkpoint EncodeN {0}/{1}", i, countEncodeN);
            }
        }

        internal void Snapshot()
        {
            int countEncode0 = 0;
            int countSnapshot = 0;
            foreach (Storage storage in storages)
            {
                countEncode0 += storage.Encode0();
            }
            foreach (Storage storage in storages)
            {
                countSnapshot += storage.Snapshot();
            }

            //logger.Info("Checkpoint Encode0 And Snapshot countEncode0={0} countSnapshot={1}", countEncode0, countSnapshot);
        }

        internal void Flush(Transaction trans)
        {
            int countFlush = 0;
            foreach (Storage storage in storages)
            {
                countFlush += storage.Flush(trans);
            }
            //logger.Info("Checkpoint Flush count={0}", countFlush);
        }

        internal void Cleanup()
        {
            foreach (Storage storage in storages)
            {
                storage.Cleanup();
            }
        }

        public abstract Database.Table OpenTable(string name);

        public interface Transaction : IDisposable
        {
            public void Commit();
            public void Rollback();
        }

        public interface Table
        {
            public Database Database { get; }
            public ByteBuffer Find(ByteBuffer key);
            public void Replace(Transaction t, ByteBuffer key, ByteBuffer value);
            public void Remove(Transaction t, ByteBuffer key);
            /// <summary>
            /// 每一条记录回调。回调返回true继续遍历，false中断遍历。
            /// </summary>
            /// <param name="callback"></param>
            /// <returns>返回已经遍历的数量</returns>
            public long Walk(Func<byte[], byte[], bool> callback);
            public void Close();
        }

        /// <summary>
        /// 由后台数据库直接支持的存储过程。
        /// 直接操作后台数据库，不经过cache。
        /// </summary>
        public interface Operates
        {
            /*
             table zeze_global {string global} 一条记录
             table zeze_instances {int localId} 每个启动的gs一条记录
             SetInUse(localId, global) // 没有启用cache-sync时，global是""
               if (false == zeze_instances.insert(localId))
               {
                 rollback;
                 return false; // 同一个localId只能启动一个。
               }
               globalNow = zeze_global.getOrAdd(global); // sql 应该是没有这样的方法的
               if (globalNow != global)
               {
                 // 不管是否启用cache-sync，global都必须一致
                 rollback;
                 return false;
               }
               if (zeze_instances.count == 1)
                 return true; // 只有一个实例，肯定成功。
               if (global.Count == 0)
               {
                 // 没有启用global，但是实例超过1。
                 rollback;
                 return false;
               }
               commit;
               return true;
             */
            public void SetInUse(int localId, string global);
            public int ClearInUse(int localId, string global);

            /// <summary>
            /// if (Exist(key))
            /// {
            ///     if (CurrentVersion != version)
            ///         return false;
            ///     UpdateData(data);
            ///     ++CurrentVersion;
            ///     version = CurrentVersion;
            ///     return true;
            /// }
            /// InsertData(data);
            /// CurrentVersion = version;
            /// return true;
            /// </summary>
            /// <param name="data"></param>
            /// <param name="version"></param>
            /// <returns></returns>
            public bool SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, ref long version);
            public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key);
        }

        public Operates DirectOperates { get; protected set; }
    }

#if USE_DATABASE
    public sealed class DatabaseMySql : Database
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public DatabaseMySql(Application zeze, string url) : base(zeze, url)
        {
            DirectOperates = new OperatesMySql(this);
        }

        public class MySqlTrans : Transaction
        {
            public MySqlConnection Connection { get; }
            public MySqlTransaction Transaction { get; }

            public MySqlTrans(string DatabaseUrl)
            {
                Connection = new MySqlConnection(DatabaseUrl);
                Connection.Open();
                Transaction = Connection.BeginTransaction();
            }

            public void Dispose()
            {
                Transaction.Dispose();
                Connection.Dispose();
            }

            public void Commit()
            {
                Transaction.Commit();
            }

            public void Rollback()
            {
                Transaction.Rollback();
            }
        }

        public override Transaction BeginTransaction()
        {
            return new MySqlTrans(DatabaseUrl);
        }

        public override Database.Table OpenTable(string name)
        {
            return new TableMysql(this, name);
        }

        public sealed class OperatesMySql : Operates
        {
            public DatabaseMySql Database { get; }

            public void SetInUse(int localId, string global)
            {
                using MySqlConnection connection = new MySqlConnection(Database.DatabaseUrl);
                connection.Open();
                MySqlCommand cmd = new MySqlCommand("_ZezeSetInUse_", connection);
                cmd.CommandType = CommandType.StoredProcedure;
                cmd.Parameters.Add("@in_localid", MySqlDbType.Int32).Value = localId;
                cmd.Parameters.Add("@in_global", MySqlDbType.VarBinary, int.MaxValue).Value = Encoding.UTF8.GetBytes(global);
                cmd.Parameters.Add(new MySqlParameter("@ReturnValue", MySqlDbType.Int32)
                {
                    Direction = ParameterDirection.Output,
                });
                cmd.Prepare();
                cmd.ExecuteNonQuery();
                switch ((int)cmd.Parameters["@ReturnValue"].Value)
                {
                    case 0:
                        return;
                    case 1:
                        throw new Exception("Unknown Error");
                    case 2:
                        throw new Exception("Instance Exist.");
                    case 3:
                        throw new Exception("Insert LocalId Faild");
                    case 4:
                        throw new Exception("Global Not Equals");
                    case 5:
                        throw new Exception("Insert Global Faild");
                    case 6:
                        throw new Exception("Instance Greater Than One But No Global");
                    default:
                        throw new Exception("Unknown ReturnValue");
                }

            }

            public int ClearInUse(int localId, string global)
            {
                using MySqlConnection connection = new MySqlConnection(Database.DatabaseUrl);
                connection.Open();
                MySqlCommand cmd = new MySqlCommand("_ZezeClearInUse_", connection);
                cmd.CommandType = CommandType.StoredProcedure;
                cmd.Parameters.Add("@in_localid", MySqlDbType.Int32).Value = localId;
                cmd.Parameters.Add("@in_global", MySqlDbType.VarBinary, int.MaxValue).Value = Encoding.UTF8.GetBytes(global);
                cmd.Parameters.Add(new MySqlParameter("@ReturnValue", MySqlDbType.Int32)
                {
                    Direction = ParameterDirection.Output,
                });
                cmd.Prepare();
                cmd.ExecuteNonQuery();
                // Clear 不报告错误，直接返回。
                return (int)cmd.Parameters["@ReturnValue"].Value;
            }

            public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key)
            {
                using MySqlConnection connection = new MySqlConnection(Database.DatabaseUrl);
                connection.Open();

                string sql = "SELECT data,version FROM _ZezeDataWithVersion_ WHERE id=@id";

                MySqlCommand cmd = new MySqlCommand(sql, connection);
                cmd.Parameters.Add("@id", MySqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Prepare();

                using (MySqlDataReader reader = cmd.ExecuteReader())
                {
                    while (reader.Read())
                    {
                        byte[] value = (byte[])reader[0];
                        long version = reader.GetInt64(1);
                        return (ByteBuffer.Wrap(value), version);
                    }
                    return (null, 0);
                }
            }

            public bool SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, ref long version)
            {
                if (key.Size == 0)
                    throw new Exception("key is empty.");

                using MySqlConnection connection = new MySqlConnection(Database.DatabaseUrl);
                connection.Open();
                MySqlCommand cmd = new MySqlCommand("_ZezeSaveDataWithSameVersion_", connection);
                cmd.CommandType = CommandType.StoredProcedure;
                cmd.Parameters.Add("@in_id", MySqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Parameters.Add("@in_data", MySqlDbType.VarBinary, int.MaxValue).Value = data.Copy();
                cmd.Parameters.Add(new MySqlParameter("@inout_version", MySqlDbType.Int64)
                {
                    Direction = ParameterDirection.InputOutput,
                    Value = version,
                });
                cmd.Parameters.Add(new MySqlParameter("@ReturnValue", MySqlDbType.Int32)
                {
                    Direction = ParameterDirection.Output,
                });
                cmd.Prepare();
                cmd.ExecuteNonQuery();
                switch ((int)cmd.Parameters["@ReturnValue"].Value)
                {
                    case 0:
                        version = (long)cmd.Parameters["@inout_version"].Value;
                        return true;
                    case 2:
                        return false;
                    default:
                        throw new Exception("Procedure SaveDataWithSameVersion Exec Error.");
                }
            }

            public OperatesMySql(DatabaseMySql database)
            {
                Database = database;

                using MySqlConnection connection = new MySqlConnection(Database.DatabaseUrl);
                connection.Open();

                string TableDataWithVersion =
                    @"CREATE TABLE IF NOT EXISTS _ZezeDataWithVersion_ (
                        id VARBINARY(767) NOT NULL PRIMARY KEY,
                        data MEDIUMBLOB NOT NULL,
                        version bigint NOT NULL
                    )ENGINE=INNODB";
                new MySqlCommand(TableDataWithVersion, connection).ExecuteNonQuery();

                new MySqlCommand("DROP PROCEDURE IF EXISTS _ZezeSaveDataWithSameVersion_", connection).ExecuteNonQuery();
                string ProcSaveDataWithSameVersion =
                    @"Create procedure _ZezeSaveDataWithSameVersion_ (
                        IN    in_id VARBINARY(767),
                        IN    in_data MEDIUMBLOB,
                        INOUT inout_version bigint,
                        OUT   ReturnValue int
                    )
                    return_label:begin
                        DECLARE oldversionexsit BIGINT;
                        DECLARE ROWCOUNT int;

                        START TRANSACTION;
                        set ReturnValue=1;
                        select version INTO oldversionexsit from _ZezeDataWithVersion_ where id=in_id;
                        select FOUND_ROWS() into ROWCOUNT;
                        if ROWCOUNT > 0 then
                            if oldversionexsit <> inout_version then
                                set ReturnValue=2;
                                ROLLBACK;
                                LEAVE return_label;
                            end if;
                            set oldversionexsit = oldversionexsit + 1;
                            update _ZezeDataWithVersion_ set data=in_data, version=oldversionexsit where id=in_id;
                            select ROW_COUNT() into ROWCOUNT;
                            if ROWCOUNT = 1 then
                                set inout_version = oldversionexsit;
                                set ReturnValue=0;
                                COMMIT;
                                LEAVE return_label;
                            end if;
                            set ReturnValue=3;
                            ROLLBACK;
                            LEAVE return_label;
                        end if;

                        insert into _ZezeDataWithVersion_ values(in_id,in_data,inout_version);
                        select ROW_COUNT() into ROWCOUNT;
                        if ROWCOUNT = 1 then
                            set ReturnValue=0;
                            COMMIT;
                            LEAVE return_label;
                        end if;
                        set ReturnValue=4;
                        ROLLBACK;
                        LEAVE return_label;
                    end;";
                new MySqlCommand(ProcSaveDataWithSameVersion, connection).ExecuteNonQuery();

                string TableInstances =
                    @"CREATE TABLE IF NOT EXISTS _ZezeInstances_ (localid int NOT NULL PRIMARY KEY)ENGINE=INNODB";
                new MySqlCommand(TableInstances, connection).ExecuteNonQuery();
                // zeze_global 使用 _ZezeDataWithVersion_ 存储。

                new MySqlCommand("DROP PROCEDURE IF EXISTS _ZezeSetInUse_", connection).ExecuteNonQuery();
                string ProcSetInUse =
                    @"Create procedure _ZezeSetInUse_ (
                        in in_localid int,
                        in in_global MEDIUMBLOB,
                        out ReturnValue int
                    )
                    return_label:begin
                        DECLARE currentglobal MEDIUMBLOB;
                        declare emptybinary MEDIUMBLOB;
                        DECLARE InstanceCount int;
                        DECLARE ROWCOUNT int;

                        START TRANSACTION;
                        set ReturnValue=1;
                        if exists (select localid from _ZezeInstances_ where localid=in_localid) then
                            set ReturnValue=2;
                            ROLLBACK;
                            LEAVE return_label;
                        end if;
                        insert into _ZezeInstances_ values(in_localid);
                        select ROW_COUNT() into ROWCOUNT;
                        if ROWCOUNT = 0 then
                            set ReturnValue=3;
                            ROLLBACK;
                            LEAVE return_label;
                        end if;
                        set emptybinary = BINARY '';
                        select data into currentglobal from _ZezeDataWithVersion_ where id=emptybinary;
                        select FOUND_ROWS() into ROWCOUNT;
                        if ROWCOUNT > 0 then
                            if currentglobal <> in_global then
                                set ReturnValue=4;
                                ROLLBACK;
                                LEAVE return_label;
                            end if;
                        else
                            insert into _ZezeDataWithVersion_ values(emptybinary, in_global, 0);
                            select ROW_COUNT() into ROWCOUNT;
                            if ROWCOUNT <> 1 then
                                set ReturnValue=5;
                                ROLLBACK;
                                LEAVE return_label;
                            end if;
                        end if;
                        set InstanceCount=0;
                        select count(*) INTO InstanceCount from _ZezeInstances_;
                        if InstanceCount = 1 then
                            set ReturnValue=0;
                            COMMIT;
                            LEAVE return_label;
                       end if;
                       if LENGTH(in_global)=0 then
                            set ReturnValue=6;
                            ROLLBACK;
                            LEAVE return_label;
                        end if;
                        set ReturnValue=0;
                        COMMIT;
                        LEAVE return_label;
                    end";
                new MySqlCommand(ProcSetInUse, connection).ExecuteNonQuery();

                new MySqlCommand("DROP PROCEDURE IF EXISTS _ZezeClearInUse_", connection).ExecuteNonQuery();
                string ProcClearInUse =
                    @"Create procedure _ZezeClearInUse_ (
                        in in_localid int,
                        in in_global MEDIUMBLOB,
                        out ReturnValue int
                    )
                    return_label:begin
                        DECLARE InstanceCount int;
                        declare emptybinary MEDIUMBLOB;
                        DECLARE ROWCOUNT INT;

                        START TRANSACTION;
                        set ReturnValue=1;
                        delete from _ZezeInstances_ where localid=in_localid;
                        select ROW_COUNT() into ROWCOUNT;
                        if ROWCOUNT = 0 then
                            set ReturnValue=2;
                            ROLLBACK;
                            LEAVE return_label;
                        end if;
                        set InstanceCount=0;
                        select count(*) INTO InstanceCount from _ZezeInstances_;
                        if InstanceCount = 0 then
                            set emptybinary = BINARY '';
                            delete from _ZezeDataWithVersion_ where id=emptybinary;
                        end if;
                        set ReturnValue=0;
                        COMMIT;
                        LEAVE return_label;
                    end";
                new MySqlCommand(ProcClearInUse, connection).ExecuteNonQuery();
            }
        }

        public sealed class TableMysql : Database.Table
        {
            public DatabaseMySql DatabaseReal { get; }
            public Database Database => DatabaseReal;
            public string Name { get; }

            public TableMysql(DatabaseMySql database, string name)
            {
                DatabaseReal = database;
                Name = name;

                using MySqlConnection connection = new MySqlConnection(database.DatabaseUrl);
                connection.Open();
                string sql = "CREATE TABLE IF NOT EXISTS " + Name
                    + "(id VARBINARY(767) NOT NULL PRIMARY KEY, value MEDIUMBLOB NOT NULL)ENGINE=INNODB";
                MySqlCommand cmd = new MySqlCommand(sql, connection);
                cmd.ExecuteNonQuery();
            }

            public void Close()
            {
            }

            public ByteBuffer Find(ByteBuffer key)
            {
                using MySqlConnection connection = new MySqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string sql = "SELECT value FROM " + Name + " WHERE id = @ID";
                // 是否可以重用 SqlCommand
                MySqlCommand cmd = new MySqlCommand(sql, connection);
                cmd.Parameters.Add("@ID", MySqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Prepare();

                using (MySqlDataReader reader = cmd.ExecuteReader())
                {
                    while (reader.Read())
                    {
                        byte[] value = (byte[])reader[0];
                        return ByteBuffer.Wrap(value);
                    }
                    return null;
                }
            }

            public void Remove(Transaction t, ByteBuffer key)
            {
                var my = t as MySqlTrans;
                string sql = "DELETE FROM " + Name + " WHERE id=@ID";
                MySqlCommand cmd = new MySqlCommand(sql, my.Connection, my.Transaction);
                cmd.Parameters.Add("@ID", MySqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Prepare();
                cmd.ExecuteNonQuery();
            }

            public void Replace(Transaction t, ByteBuffer key, ByteBuffer value)
            {
                var my = t as MySqlTrans;
                string sql = "REPLACE INTO " + Name + " values(@ID,@VALUE)";
                MySqlCommand cmd = new MySqlCommand(sql, my.Connection, my.Transaction);
                cmd.Parameters.Add("@ID", MySqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Parameters.Add("@VALUE", MySqlDbType.VarBinary, int.MaxValue).Value = value.Copy();
                cmd.Prepare();
                cmd.ExecuteNonQuery();
            }

            public long Walk(Func<byte[], byte[], bool> callback)
            {
                using MySqlConnection connection = new MySqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string sql = "SELECT id,value FROM " + Name;
                MySqlCommand cmd = new MySqlCommand(sql, connection);
                cmd.Prepare();

                long count = 0;
                using (MySqlDataReader reader = cmd.ExecuteReader())
                {
                    while (reader.Read())
                    {
                        byte[] key = (byte[])reader[0];
                        byte[] value = (byte[])reader[1];
                        ++count;
                        if (false == callback(key, value))
                            break;
                    }
                }
                return count;
            }
        }
    }

    public sealed class DatabaseSqlServer : Database
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public DatabaseSqlServer(Application zeze, string url) : base(zeze, url)
        {
            DirectOperates = new OperatesSqlServer(this);
        }

        public class SqlTrans : Transaction
        {
            public SqlConnection Connection { get; }
            public SqlTransaction Transaction { get; }

            public SqlTrans(string DatabaseUrl)
            {
                Connection = new SqlConnection(DatabaseUrl);
                Connection.Open();
                Transaction = Connection.BeginTransaction();
            }

            public void Dispose()
            {
                Transaction.Dispose();
                Connection.Dispose();
            }

            public void Commit()
            {
                Transaction.Commit();
            }

            public void Rollback()
            {
                Transaction.Rollback();
            }
        }

        public override Transaction BeginTransaction()
        {
            return new SqlTrans(DatabaseUrl);
        }

        public override Database.Table OpenTable(string name)
        {
            return new TableSqlServer(this, name);
        }

        public sealed class OperatesSqlServer : Operates
        {
            public DatabaseSqlServer DatabaseReal { get; }
            public Database Database => DatabaseReal;

            public void SetInUse(int localId, string global)
            {
                using SqlConnection connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();
                SqlCommand cmd = new SqlCommand("_ZezeSetInUse_", connection);
                cmd.CommandType = CommandType.StoredProcedure;
                cmd.Parameters.Add("@localid", SqlDbType.Int).Value = localId;
                cmd.Parameters.Add("@global", SqlDbType.VarBinary, int.MaxValue).Value = Encoding.UTF8.GetBytes(global);
                cmd.Parameters.Add(new SqlParameter("@ReturnValue", SqlDbType.Int)
                {
                    Direction = ParameterDirection.Output,
                });
                cmd.Prepare();
                cmd.ExecuteNonQuery();
                switch ((int)cmd.Parameters["@ReturnValue"].Value)
                {
                    case 0:
                        return;
                    case 1:
                        throw new Exception("Unknown Error");
                    case 2:
                        throw new Exception("Instance Exist");
                    case 3:
                        throw new Exception("Insert LocalId Faild");
                    case 4:
                        throw new Exception("Global Not Equals");
                    case 5:
                        throw new Exception("Insert Global Faild");
                    case 6:
                        throw new Exception("Instance Greater Than One But No Global");
                    default:
                        throw new Exception("Unknown ReturnValue");
                }

            }

            public int ClearInUse(int localId, string global)
            {
                using SqlConnection connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();
                SqlCommand cmd = new SqlCommand("_ZezeClearInUse_", connection);
                cmd.CommandType = CommandType.StoredProcedure;
                cmd.Parameters.Add("@localid", SqlDbType.Int).Value = localId;
                cmd.Parameters.Add("@global", SqlDbType.VarBinary, int.MaxValue).Value = Encoding.UTF8.GetBytes(global);
                cmd.Parameters.Add(new SqlParameter("@ReturnValue", SqlDbType.Int)
                {
                    Direction = ParameterDirection.Output,
                });
                cmd.Prepare();
                cmd.ExecuteNonQuery();
                // Clear 不报告错误，直接返回。
                return (int)cmd.Parameters["@ReturnValue"].Value;
            }

            public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key)
            {
                using SqlConnection connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string sql = "SELECT data,version FROM _ZezeDataWithVersion_ WHERE id=@id";

                SqlCommand cmd = new SqlCommand(sql, connection);
                cmd.Parameters.Add("@id", SqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Prepare();

                using (SqlDataReader reader = cmd.ExecuteReader())
                {
                    while (reader.Read())
                    {
                        byte[] value = (byte[])reader[0];
                        long version = reader.GetInt64(1);
                        return (ByteBuffer.Wrap(value), version);
                    }
                    return (null, 0);
                }
            }

            public bool SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, ref long version)
            {
                if (key.Size == 0)
                    throw new Exception("key is empty.");

                using SqlConnection connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();
                SqlCommand cmd = new SqlCommand("_ZezeSaveDataWithSameVersion_", connection);
                cmd.CommandType = CommandType.StoredProcedure;
                cmd.Parameters.Add("@id", SqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Parameters.Add("@data", SqlDbType.VarBinary, int.MaxValue).Value = data.Copy();
                cmd.Parameters.Add(new SqlParameter("@version", SqlDbType.BigInt)
                {
                    Direction = ParameterDirection.InputOutput,
                    Value = version,
                });
                cmd.Parameters.Add(new SqlParameter("@ReturnValue", SqlDbType.Int)
                {
                    Direction = ParameterDirection.Output,
                });
                cmd.Prepare();
                cmd.ExecuteNonQuery();
                switch ((int)cmd.Parameters["@ReturnValue"].Value)
                {
                    case 0:
                        version = (long)cmd.Parameters["@version"].Value;
                        return true;
                    case 2:
                        return false;
                    default:
                        throw new Exception("Procedure SaveDataWithSameVersion Exec Error.");
                }
            }

            public OperatesSqlServer(DatabaseSqlServer database)
            {
                DatabaseReal = database;

                using SqlConnection connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string TableDataWithVersion
                    = "if not exists (select * from sysobjects where name='_ZezeDataWithVersion_' and xtype='U')"
                    + " CREATE TABLE _ZezeDataWithVersion_ (id VARBINARY(767) NOT NULL PRIMARY KEY, data VARBINARY(MAX) NOT NULL, version bigint NOT NULL)";
                new SqlCommand(TableDataWithVersion, connection).ExecuteNonQuery();

                string ProcSaveDataWithSameVersion =
                    @"Create or Alter procedure _ZezeSaveDataWithSameVersion_
                        @id VARBINARY(767),
                        @data VARBINARY(MAX),
                        @version bigint output,
                        @ReturnValue int output
                    as
                    begin
                        BEGIN TRANSACTION
                        set @ReturnValue=1
                        DECLARE @currentversion bigint
                        select @currentversion=version from _ZezeDataWithVersion_ where id = @id
                        if @@ROWCOUNT > 0
                        begin
                            if @currentversion <> @version
                            begin
                                set @ReturnValue=2
                                ROLLBACK TRANSACTION
                                return 2
                            end
                            set @currentversion = @currentversion + 1
                            update _ZezeDataWithVersion_ set data = @data, version = @currentversion where id = @id
                            if @@rowcount = 1
                            begin
                                set @version = @currentversion
                                set @ReturnValue=0
                                COMMIT TRANSACTION
                                return 0
                            end
                            set @ReturnValue=3
                            ROLLBACK TRANSACTION
                            return 3
                        end

                        insert into _ZezeDataWithVersion_ values(@id,@data,@version)
                        if @@rowcount = 1
                        begin
                            set @ReturnValue=0
                            COMMIT TRANSACTION
                            return 0
                        end
                        set @ReturnValue=4
                        ROLLBACK TRANSACTION
                        return 4
                    end";
                new SqlCommand(ProcSaveDataWithSameVersion, connection).ExecuteNonQuery();

                string TableInstances
                    = "if not exists (select * from sysobjects where name='_ZezeInstances_' and xtype='U')"
                    + " CREATE TABLE _ZezeInstances_ (localid int NOT NULL PRIMARY KEY)";
                new SqlCommand(TableInstances, connection).ExecuteNonQuery();
                // zeze_global 使用 _ZezeDataWithVersion_ 存储。

                string ProcSetInUse =
                    @"Create or Alter procedure _ZezeSetInUse_
                        @localid int,
                        @global VARBINARY(MAX),
                        @ReturnValue int output
                    as
                    begin
                        BEGIN TRANSACTION
                        set @ReturnValue=1
                        if exists (select localid from _ZezeInstances_ where localid = @localid)
                        begin
                            set @ReturnValue=2
                            ROLLBACK TRANSACTION
                            return 2
                        end
                        insert into _ZezeInstances_ values(@localid)
                        if @@rowcount = 0
                        begin
                            set @ReturnValue=3
                            ROLLBACK TRANSACTION
                            return 3
                        end
                        DECLARE @currentglobal VARBINARY(MAX)
                        declare @emptybinary varbinary(max)
                        set @emptybinary = convert(varbinary(max), '')
                        select @currentglobal=data from _ZezeDataWithVersion_ where id=@emptybinary
                        if @@rowcount > 0
                        begin
                            if @currentglobal <> @global
                            begin
                                set @ReturnValue=4
                                ROLLBACK TRANSACTION
                                return 4
                            end
                        end
                        else
                        begin
                            insert into _ZezeDataWithVersion_ values(@emptybinary, @global, 0)
                            if @@rowcount <> 1
                            begin
                                set @ReturnValue=5
                                ROLLBACK TRANSACTION
                                return 5
                            end
                        end
                        DECLARE @InstanceCount int
                        set @InstanceCount=0
                        select @InstanceCount=count(*) from _ZezeInstances_
                        if @InstanceCount = 1
                        begin
                            set @ReturnValue=0
                            COMMIT TRANSACTION
                            return 0
                        end
                        if DATALENGTH(@global)=0
                        begin
                            set @ReturnValue=6
                            ROLLBACK TRANSACTION
                            return 6
                        end
                        set @ReturnValue=0
                        COMMIT TRANSACTION
                        return 0
                    end";
                new SqlCommand(ProcSetInUse, connection).ExecuteNonQuery();

                string ProcClearInUse =
                    @"Create or Alter procedure _ZezeClearInUse_
                        @localid int,
                        @global VARBINARY(MAX),
                        @ReturnValue int output
                    as
                    begin
                        BEGIN TRANSACTION
                        set @ReturnValue=1
                        delete from _ZezeInstances_ where localid=@localid
                        if @@rowcount = 0
                        begin
                            set @ReturnValue=2
                            ROLLBACK TRANSACTION
                            return 2
                        end
                        DECLARE @InstanceCount int
                        set @InstanceCount=0
                        select @InstanceCount=count(*) from _ZezeInstances_
                        if @InstanceCount = 0
                        begin
                            declare @emptybinary varbinary(max)
                            set @emptybinary = convert(varbinary(max), '')
                            delete from _ZezeDataWithVersion_ where id=@emptybinary
                        end
                        set @ReturnValue=0
                        COMMIT TRANSACTION
                        return 0
                    end";
                new SqlCommand(ProcClearInUse, connection).ExecuteNonQuery();
            }
        }

        public sealed class TableSqlServer : Database.Table
        {
            public DatabaseSqlServer DatabaseReal { get; }
            public Database Database => DatabaseReal;
            public string Name { get; }

            public TableSqlServer(DatabaseSqlServer database, string name)
            {
                DatabaseReal = database;
                Name = name;

                using SqlConnection connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string sql = "if not exists (select * from sysobjects where name='" + Name
                    + "' and xtype='U') CREATE TABLE " + Name
                    + "(id VARBINARY(767) NOT NULL PRIMARY KEY, value VARBINARY(MAX) NOT NULL)";
                SqlCommand cmd = new SqlCommand(sql, connection);
                cmd.ExecuteNonQuery();
            }

            public void Close()
            {
            }

            public ByteBuffer Find(ByteBuffer key)
            {
                using SqlConnection connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string sql = "SELECT value FROM " + Name + " WHERE id = @ID";

                // 是否可以重用 SqlCommand
                SqlCommand cmd = new SqlCommand(sql, connection);
                cmd.Parameters.Add("@ID", System.Data.SqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Prepare();

                using (SqlDataReader reader = cmd.ExecuteReader())
                {
                    while (reader.Read())
                    {
                        byte[] value = (byte[])reader[0];
                        return ByteBuffer.Wrap(value);
                    }
                    return null;
                }
            }

            public void Remove(Transaction t, ByteBuffer key)
            {
                var my = t as SqlTrans;
                string sql = "DELETE FROM " + Name + " WHERE id=@ID";
                SqlCommand cmd = new SqlCommand(sql, my.Connection, my.Transaction);
                cmd.Parameters.Add("@ID", System.Data.SqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Prepare();
                cmd.ExecuteNonQuery();
            }

            public void Replace(Transaction t, ByteBuffer key, ByteBuffer value)
            {
                var my = t as SqlTrans;
                string sql = "update " + Name + " set value=@VALUE where id=@ID"
                    + " if @@rowcount = 0 and @@error = 0 insert into " + Name + " values(@ID,@VALUE)";

                SqlCommand cmd = new SqlCommand(sql, my.Connection, my.Transaction);
                cmd.Parameters.Add("@ID", System.Data.SqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Parameters.Add("@VALUE", System.Data.SqlDbType.VarBinary, int.MaxValue).Value = value.Copy();
                cmd.Prepare();
                cmd.ExecuteNonQuery();
            }

            public long Walk(Func<byte[], byte[], bool> callback)
            {
                using SqlConnection connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string sql = "SELECT id,value FROM " + Name;
                SqlCommand cmd = new SqlCommand(sql, connection);
                cmd.Prepare();

                long count = 0;
                using (SqlDataReader reader = cmd.ExecuteReader())
                {
                    while (reader.Read())
                    {
                        byte[] key = (byte[])reader[0];
                        byte[] value = (byte[])reader[1];
                        ++count;
                        if (false == callback(key, value))
                            break;
                    }
                }
                return count;
            }
        }
    }

    public class DatabaseRocksDb : Database
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private RocksDb Db;
        private WriteOptions WriteOptions = new WriteOptions();
        private ReadOptions ReadOptions = new ReadOptions();
        private ColumnFamilyOptions CfOptions = new ColumnFamilyOptions();
        private DbOptions DbOptions = new DbOptions();
        private ConcurrentDictionary<string, string> ColumnFamilies = new ConcurrentDictionary<string, string>();

        public DatabaseRocksDb(Application zeze, string url) : base(zeze, url)
        {
            if (false == string.IsNullOrEmpty(zeze.Config.GlobalCacheManagerHostNameOrAddress))
            {
                throw new Exception("RocksDb Can Not Work With GlobalCacheManager.");
            }
            DbOptions.SetCreateIfMissing(true);
            var columnFamilies = new ColumnFamilies();
            foreach (var cf in RocksDb.ListColumnFamilies(DbOptions, DatabaseUrl))
            {
                columnFamilies.Add(cf, CfOptions);
                ColumnFamilies[cf] = cf;
            }
            // DirectOperates 依赖 Db，所以只能在这里打开。要不然，放在Open里面更加合理。
            Db = RocksDb.Open(DbOptions, url, columnFamilies);
            DirectOperates = new OperatesRocksDb(this);
        }

        public override void Close()
        {
            base.Close();
            Db.Dispose();
        }

        public class RockdsDbTrans : Transaction
        {
            private DatabaseRocksDb Database;
            private WriteBatch Batch;

            public RockdsDbTrans(DatabaseRocksDb database)
            {
                Database = database;
                Batch = new WriteBatch();
            }

            public void Dispose()
            {
            }

            internal void Put(byte[] key, int keylen, byte[] value, int valuelen, ColumnFamilyHandle family)
            {
                Batch.Put(key, (ulong)keylen, value, (ulong)valuelen, family);
            }

            internal void Remove(byte[] key, int keylen, ColumnFamilyHandle family)
            {
                Batch.Delete(key, (ulong)(keylen), family);
            }

            public void Commit()
            {
                Database.Db.Write(Batch, Database.WriteOptions);
            }

            public void Rollback()
            {
            }
        }

        public override Transaction BeginTransaction()
        {
            return new RockdsDbTrans(this);
        }

        public override Table OpenTable(string name)
        {
            ColumnFamilies.GetOrAdd(name, (key) =>
            {
                Db.CreateColumnFamily(CfOptions, key);
                return key;
            });
            return new TableRocksDb(this, name);
        }

        public sealed class TableRocksDb : Database.Table
        {
            public DatabaseRocksDb DatabaseReal { get; }
            public Database Database => DatabaseReal;
            public string Name { get; }
            private ColumnFamilyHandle ColumnFamily { get; }

            public TableRocksDb(DatabaseRocksDb database, string name)
            {
                DatabaseReal = database;
                Name = name;
                ColumnFamily = DatabaseReal.Db.GetColumnFamily(name);
            }

            public void Close()
            {
            }

            private (byte[], int) GetBytes(ByteBuffer bb)
            {
                byte[] bytes;
                int byteslen;
                if (bb.ReadIndex == 0)
                {
                    bytes = bb.Bytes;
                    byteslen = bb.Size;
                }
                else
                {
                    bytes = bb.Copy();
                    byteslen = bytes.Length;
                }
                return (bytes, byteslen);
            }

            public ByteBuffer Find(ByteBuffer _key)
            {
                var (key, keylen) = GetBytes(_key);
                var value = DatabaseReal.Db.Get(key, keylen, ColumnFamily, DatabaseReal.ReadOptions);
                if (null == value)
                    return null;
                return ByteBuffer.Wrap(value);
            }

            public void Remove(Transaction t, ByteBuffer _key)
            {
                var txn = t as RockdsDbTrans;
                var (key, keylen) = GetBytes(_key);
                txn.Remove(key, keylen, ColumnFamily);
            }

            public void Replace(Transaction t, ByteBuffer _key, ByteBuffer _value)
            {
                var txn = t as RockdsDbTrans;
                var (key, keylen) = GetBytes(_key);
                var (value, valuelen) = GetBytes(_value);
                txn.Put(key, keylen, value, valuelen, ColumnFamily);
            }

            public long Walk(Func<byte[], byte[], bool> callback)
            {
                var it = DatabaseReal.Db.NewIterator(ColumnFamily, DatabaseReal.ReadOptions);
                try
                {
                    long countWalked = 0;
                    it.SeekToFirst();
                    while (it.Valid())
                    {
                        ++countWalked;
                        if (false == callback(it.Key(), it.Value()))
                            return countWalked;
                        it.Next();
                    }
                    return countWalked;
                }
                finally
                {
                    it.Dispose();
                }
            }
        }

        public sealed class OperatesRocksDb : Operates
        {
            public DatabaseRocksDb DatabaseReal { get; }
            public Database Database => DatabaseReal;

            public OperatesRocksDb(DatabaseRocksDb database)
            {
                DatabaseReal = database;
            }

            public int ClearInUse(int localId, string global)
            {
                return 0;
            }

            public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key)
            {
                return (null, 0);
            }

            public bool SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, ref long version)
            {
                return true;
            }

            public void SetInUse(int localId, string global)
            {
            }
        }
    }
#endif // USE_DATABASE
    /// <summary>
    /// Zeze.Transaction.Table.storage 为 null 时，就表示内存表了。这个实现是为了测试 checkpoint 流程。
    /// </summary>
    public sealed class DatabaseMemory : Database
    {
        public DatabaseMemory(Application zeze, string url) : base(zeze, url)
        {
            DirectOperates = new ProceduresMemory();
        }

        public class ByteArrayComparer : IEqualityComparer<byte[]>
        {
            public bool Equals(byte[] left, byte[] right)
            {
                return ByteBuffer.Equals(left, right);
            }

            public int GetHashCode(byte[] key)
            {
                return ByteBuffer.calc_hashnr(key, 0, key.Length);
            }
        }

        public class ProceduresMemory : Operates
        {
            public int ClearInUse(int localId, string global)
            {
                return 0;
            }
            public void SetInUse(int localId, string global)
            {
            }

            sealed class DataWithVersion
            {
                public ByteBuffer Data { get; set; }
                public long Version { get; set; }
            }

            private Dictionary<byte[], DataWithVersion> DataWithVersions = new Dictionary<byte[], DataWithVersion>(new ByteArrayComparer());

            public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key)
            {
                lock (DataWithVersions)
                {
                    if (DataWithVersions.TryGetValue(key.Copy(), out var exist))
                    {
                        return (exist.Data, exist.Version);
                    }
                    return (null, 0);
                }
            }

            public bool SaveDataWithSameVersion(ByteBuffer _key, ByteBuffer data, ref long version)
            {
                lock (DataWithVersions)
                {
                    byte[] key = _key.Copy();
                    if (DataWithVersions.TryGetValue(key, out var exist))
                    {
                        if (exist.Version != version)
                            return false;

                        exist.Data = ByteBuffer.Wrap(data.Copy());
                        ++exist.Version;
                        version = exist.Version;
                        return true;
                    }
                    DataWithVersions.Add(key, new DataWithVersion()
                    {
                        Data = ByteBuffer.Wrap(data.Copy()),
                        Version = version,
                    });
                    return true;
                }
            }
        }

        public class MemTrans : Transaction
        {
            public MemTrans(string DatabaseUrl)
            {

            }

            public void Dispose()
            {

            }


            public void Commit()
            {
            }

            public void Rollback()
            {
            }
        }

        public override Transaction BeginTransaction()
        {
            return new MemTrans(DatabaseUrl);
        }

        private static ConcurrentDictionary<string, ConcurrentDictionary<string, TableMemory>> databaseTables
            = new ConcurrentDictionary<string, ConcurrentDictionary<string, TableMemory>>();
  
        public override Database.Table OpenTable(string name)
        {
            var tables = databaseTables.GetOrAdd(DatabaseUrl,
                (urlnotused) => new ConcurrentDictionary<string, TableMemory>());

            return tables.GetOrAdd(name, (tablenamenotused) => new TableMemory(this, name));
        }

        public sealed class TableMemory : Database.Table
        {
            public DatabaseMemory DatabaseReal { get; }
            public Database Database => DatabaseReal;
            public string Name { get; }

            public TableMemory(DatabaseMemory db, string name)
            {
                DatabaseReal = db;
                Name = name;
            }

            public ConcurrentDictionary<byte[], byte[]> Map { get; } = new ConcurrentDictionary<byte[], byte[]>(new ByteArrayComparer());

            public ByteBuffer Find(ByteBuffer key)
            {
                if (Map.TryGetValue(key.Copy(), out var value))
                {
                    return ByteBuffer.Wrap(value);
                }
                return null;
            }

            public void Remove(Transaction t, ByteBuffer key)
            {
                Map.TryRemove(key.Copy(), out var notused);
            }

            public void Replace(Transaction t, ByteBuffer key, ByteBuffer value)
            {
                Map[key.Copy()] = value.Copy();
            }

            public long Walk(Func<byte[], byte[], bool> callback)
            {
                lock (this)
                {
                    // 不允许并发？
                    long count = 0;
                    foreach (var e in Map)
                    {
                        ++count;
                        if (false == callback(e.Key, e.Value))
                            break;
                    }
                    return count;
                }
            }

            public void Close()
            {
            }
        }
    }

}
