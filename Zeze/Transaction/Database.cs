using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using System.Collections.Concurrent;
#if USE_DATABASE
using System.Data.SqlClient;
using MySql.Data.MySqlClient;
using System.Data;
#endif
using System.Threading;
using System.Threading.Tasks;

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

        public Database(string url)
        {
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

        public void AddTable(Zeze.Transaction.Table table)
        {
            tables.Add(table.Name, table);
        }

        public void RemoveTable(Zeze.Transaction.Table table)
        {
            tables.Remove(table.Name);
        }

        public void Open(Zeze.Application app)
        {
            foreach (Zeze.Transaction.Table table in tables.Values)
            {
                Storage storage = table.Open(app, this);
                if (null != storage)
                    storages.Add(storage);
            }
        }

        public /*virtual*/ void Close()
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

        internal void Flush(Checkpoint sync)
        {
            Flush(sync, () =>
            {
                int countFlush = 0;
                foreach (Storage storage in storages)
                {
                    countFlush += storage.Flush();
                }
                //logger.Info("Checkpoint Flush count={0}", countFlush);
            });
        }

        internal void Cleanup()
        {
            foreach (Storage storage in storages)
            {
                storage.Cleanup();
            }
        }

        internal ManualResetEvent CommitReady { get; } = new ManualResetEvent(false);

        public abstract void Flush(Checkpoint sync, Action flushAction);
        public abstract Database.Table OpenTable(string name);

        public interface Table
        {
            public ByteBuffer Find(ByteBuffer key);
            public void Replace(ByteBuffer key, ByteBuffer value);
            public void Remove(ByteBuffer key);
            public void Walk(IWalk iw);
            public void Close();

            public interface IWalk
            {
                public bool OnRecord(byte[] key, byte[] value);
            }
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

        internal MySqlConnection CheckpointSqlConnection { get; private set; }
        internal MySqlTransaction Transaction { get; private set; }

        public DatabaseMySql(string url) : base(url)
        {
            DirectOperates = new OperatesMySql(this);
        }

        public override void Flush(Checkpoint sync, Action flushAction)
        {
            try
            {
                for (int i = 0; i < 60; ++i)
                {
                    using MySqlConnection connection = new MySqlConnection(DatabaseUrl);
                    CheckpointSqlConnection = connection;
                    connection.Open();
                    Transaction = connection.BeginTransaction();
                    try
                    {
                        flushAction();
                        if (null != sync) // null for test
                        {
                            CommitReady.Set();
                            sync.WaitAllReady();
                        }
                        Transaction.Commit();
                        return;
                    }
                    catch (Exception ex)
                    {
                        CommitReady.Reset();
                        Transaction.Rollback();
                        logger.Warn(ex, "Checkpoint error.");
                    }
                    Thread.Sleep(1000);
                }
                logger.Fatal("Checkpoint too many try.");
                Environment.Exit(54321);
            }
            finally
            {
                CheckpointSqlConnection = null;
                Transaction = null;
            }
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
            public DatabaseMySql Database { get; }
            public string Name { get; }

            public TableMysql(DatabaseMySql database, string name)
            {
                Database = database;
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
                using MySqlConnection connection = new MySqlConnection(Database.DatabaseUrl);
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

            public void Remove(ByteBuffer key)
            {
                string sql = "DELETE FROM " + Name + " WHERE id=@ID";
                MySqlCommand cmd = new MySqlCommand(sql, Database.CheckpointSqlConnection, Database.Transaction);
                cmd.Parameters.Add("@ID", MySqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Prepare();
                cmd.ExecuteNonQuery();
            }

            public void Replace(ByteBuffer key, ByteBuffer value)
            {
                string sql = "REPLACE INTO " + Name + " values(@ID,@VALUE)";
                MySqlCommand cmd = new MySqlCommand(sql, Database.CheckpointSqlConnection, Database.Transaction);
                cmd.Parameters.Add("@ID", MySqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Parameters.Add("@VALUE", MySqlDbType.VarBinary, int.MaxValue).Value = value.Copy();
                cmd.Prepare();
                cmd.ExecuteNonQuery();
            }

            public void Walk(Database.Table.IWalk iw)
            {
                using MySqlConnection connection = new MySqlConnection(Database.DatabaseUrl);
                connection.Open();

                string sql = "SELECT id,value FROM " + Name;
                MySqlCommand cmd = new MySqlCommand(sql, connection);
                cmd.Prepare();

                using (MySqlDataReader reader = cmd.ExecuteReader())
                {
                    while (reader.Read())
                    {
                        byte[] key = (byte[])reader[0];
                        byte[] value = (byte[])reader[1];
                        if (false == iw.OnRecord(key, value))
                            break;
                    }
                }
            }
        }
    }

    public sealed class DatabaseSqlServer : Database
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        internal SqlConnection CheckpointSqlConnection { get; private set; }
        internal SqlTransaction Transaction { get; private set; }

        public DatabaseSqlServer(string url) : base(url)
        {
            DirectOperates = new OperatesSqlServer(this);
        }

        public override void Flush(Checkpoint sync, Action flushAction)
        {
            try
            {
                for (int i = 0; i < 60; ++i)
                {
                    using SqlConnection connection = new SqlConnection(DatabaseUrl);
                    CheckpointSqlConnection = connection;
                    connection.Open();
                    Transaction = connection.BeginTransaction();
                    try
                    {
                        flushAction();
                        if (null != sync) // null for test
                        {
                            CommitReady.Set();
                            sync.WaitAllReady();
                        }
                        Transaction.Commit();
                        return;
                    }
                    catch (Exception ex)
                    {
                        CommitReady.Reset();
                        Transaction.Rollback();
                        logger.Warn(ex, "Checkpoint error.");
                    }
                    Thread.Sleep(1000);
                }
                logger.Fatal("Checkpoint too many try.");
                Environment.Exit(54321);
            }
            finally
            {
                CheckpointSqlConnection = null;
                Transaction = null;
            }
        }

        public override Database.Table OpenTable(string name)
        {
            return new TableSqlServer(this, name);
        }

        public sealed class OperatesSqlServer : Operates
        {
            public DatabaseSqlServer Database { get; }

            public void SetInUse(int localId, string global)
            {
                using SqlConnection connection = new SqlConnection(Database.DatabaseUrl);
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
                using SqlConnection connection = new SqlConnection(Database.DatabaseUrl);
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
                using SqlConnection connection = new SqlConnection(Database.DatabaseUrl);
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

                using SqlConnection connection = new SqlConnection(Database.DatabaseUrl);
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
                Database = database;

                using SqlConnection connection = new SqlConnection(Database.DatabaseUrl);
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
            public DatabaseSqlServer Database { get; }
            public string Name { get; }

            public TableSqlServer(DatabaseSqlServer database, string name)
            {
                Database = database;
                Name = name;

                using SqlConnection connection = new SqlConnection(Database.DatabaseUrl);
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
                using SqlConnection connection = new SqlConnection(Database.DatabaseUrl);
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

            public void Remove(ByteBuffer key)
            {
                string sql = "DELETE FROM " + Name + " WHERE id=@ID";
                SqlCommand cmd = new SqlCommand(sql, Database.CheckpointSqlConnection, Database.Transaction);
                cmd.Parameters.Add("@ID", System.Data.SqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Prepare();
                cmd.ExecuteNonQuery();
            }

            public void Replace(ByteBuffer key, ByteBuffer value)
            {
                string sql = "update " + Name + " set value=@VALUE where id=@ID"
                    + " if @@rowcount = 0 and @@error = 0 insert into " + Name + " values(@ID,@VALUE)";

                SqlCommand cmd = new SqlCommand(sql, Database.CheckpointSqlConnection, Database.Transaction);
                cmd.Parameters.Add("@ID", System.Data.SqlDbType.VarBinary, 767).Value = key.Copy();
                cmd.Parameters.Add("@VALUE", System.Data.SqlDbType.VarBinary, int.MaxValue).Value = value.Copy();
                cmd.Prepare();
                cmd.ExecuteNonQuery();
            }

            public void Walk(Database.Table.IWalk iw)
            {
                using SqlConnection connection = new SqlConnection(Database.DatabaseUrl);
                connection.Open();

                string sql = "SELECT id,value FROM " + Name;
                SqlCommand cmd = new SqlCommand(sql, connection);
                cmd.Prepare();

                using (SqlDataReader reader = cmd.ExecuteReader())
                {
                    while (reader.Read())
                    {
                        byte[] key = (byte[])reader[0];
                        byte[] value = (byte[])reader[1];
                        if (false == iw.OnRecord(key, value))
                            break;
                    }
                }
            }
        }
    }
#endif
    /// <summary>
    /// Zeze.Transaction.Table.storage 为 null 时，就表示内存表了。这个实现是为了测试 checkpoint 流程。
    /// </summary>
    public sealed class DatabaseMemory : Database
    {
        public DatabaseMemory(string url) : base(url)
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

        public override void Flush(Checkpoint sync, Action flushAction)
        {
            flushAction();
            if (null != sync) // null for test
            {
                CommitReady.Set();
                sync.WaitAllReady();
            }
        }

        private static ConcurrentDictionary<string, ConcurrentDictionary<string, TableMemory>> databaseTables
            = new ConcurrentDictionary<string, ConcurrentDictionary<string, TableMemory>>();
  
        public override Database.Table OpenTable(string name)
        {
            var tables = databaseTables.GetOrAdd(DatabaseUrl, (urlnotused) => new ConcurrentDictionary<string, TableMemory>());
            return tables.GetOrAdd(name, (tablenamenotused) => new TableMemory(name));
        }

        public sealed class TableMemory : Database.Table
        {
            public string Name { get; }

            public TableMemory(string name)
            {
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

            public void Remove(ByteBuffer key)
            {
                Map.TryRemove(key.Copy(), out var notused);
            }

            public void Replace(ByteBuffer key, ByteBuffer value)
            {
                Map[key.Copy()] = value.Copy();
            }

            public void Walk(Database.Table.IWalk iw)
            {
                lock (this)
                {
                    // 不允许并发？
                    foreach (var e in Map)
                    {
                        if (false == iw.OnRecord(e.Key, e.Value))
                            break;
                    }
                }
            }

            public void Close()
            {
            }
        }
    }

}
