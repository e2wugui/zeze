using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using System.Collections.Concurrent;
using System.Threading.Tasks;
using Zeze.Util;
#if USE_DATABASE
using Microsoft.Data.SqlClient;
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
        // 当数据库对Key长度有限制时，使用这个常量。这个数字来自 PorlarDb-X 其中MySql 8是3072。
        // 以后需要升级时，修改这个常量。但是对于已经存在的表，需要自己完成Alter。
        public const int eMaxKeyLength = 3070;

        private readonly ConcurrentDictionary<string, Zeze.Transaction.Table> tables = new();
        internal readonly List<Storage> storages = new();
        internal ICollection<Zeze.Transaction.Table> Tables => tables.Values;

        public string DatabaseUrl { get; }
        public Config.DatabaseConf DatabaseConf { get; }

        public Application Zeze { get; }

        public Database(Application zeze, Config.DatabaseConf databaseConf)
        {
            Zeze = zeze;
            this.DatabaseConf = databaseConf;
            this.DatabaseUrl = databaseConf.DatabaseUrl;
            Executor = new (() => MaxPoolSize);
        }

        public Zeze.Transaction.Table GetTable(string name)
        {
            if (tables.TryGetValue(name, out var table))
            {
                return table;
            }
            return null;
        }

        public abstract int MaxPoolSize { get; }

        public abstract TransactionAsync BeginTransaction();

        public void AddTable(Zeze.Transaction.Table table)
        {
            if (false == tables.TryAdd(table.Name, table))
                throw new Exception($"duplicate table={table.Name}");
        }

        public void RemoveTable(Zeze.Transaction.Table table)
        {
            table.Close();
            tables.TryRemove(table.Name, out _);
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

        public void OpenDynamicTable(Application app, Zeze.Transaction.Table table)
        {
            var storage = table.Open(app, this);
            if (null != storage)
                storages.Add(storage);
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

        internal async Task Flush(ITransaction trans, Dictionary<Database, Database.TransactionAsync> tss)
        {
            int countFlush = 0;
            foreach (Storage storage in storages)
            {
                countFlush += await storage.Flush(trans, tss);
            }
            //logger.Info("Checkpoint Flush count={0}", countFlush);
        }

        internal async Task Cleanup()
        {
            foreach (Storage storage in storages)
            {
                await storage.Cleanup();
            }
        }

        public abstract TableAsync OpenTable(string name);

        public interface ITransaction : IDisposable
        {
            public void Commit();
            public void Rollback();
        }

        public interface ITable
        {
            public Database Database { get; }
            public ByteBuffer Find(ByteBuffer key);
            public void Replace(ITransaction t, ByteBuffer key, ByteBuffer value);
            public void Remove(ITransaction t, ByteBuffer key);
            /// <summary>
            /// 每一条记录回调。回调返回true继续遍历，false中断遍历。
            /// </summary>
            /// <param name="callback"></param>
            /// <returns>返回已经遍历的数量</returns>
            public long Walk(Func<byte[], byte[], bool> callback);
            public long WalkKey(Func<byte[], bool> callback);
            public void Close();
            public abstract bool IsNew { get; }
        }

        public Util.AsyncExecutor Executor { get; }

        public class TransactionAsync : IDisposable
        {
            public Database Database { get; }
            public ITransaction ITransaction { get; }

            public TransactionAsync(Database d, ITransaction t)
            {
                Database = d;
                ITransaction = t;
            }

            public async Task CommitAsync()
            {
                await Database.Executor.RunAsync(() => ITransaction.Commit());
            }

            public async Task RollbackAsync()
            {
                await Database.Executor.RunAsync(() => ITransaction.Rollback());
            }

            public void Dispose()
            {
                GC.SuppressFinalize(this);

                ITransaction.Dispose();
            }
        }

        public class TableAsync
        {
            public ITable ITable { get; }
            public Database Database => ITable.Database;

            public bool IsNew => ITable.IsNew;

            public TableAsync(ITable itable)
            {
                ITable = itable;
            }

            public void Close()
            {
                ITable.Close();
            }

            public async Task<ByteBuffer> FindAsync(ByteBuffer key)
            {
                ByteBuffer value = null;
                await Database.Executor.RunAsync(() => value = ITable.Find(key));
                return value;
            }

            public async Task RemoveAsync(ITransaction t, ByteBuffer key)
            {
                await Database.Executor.RunAsync(() => ITable.Remove(t, key));
            }

            public async Task ReplaceAsync(ITransaction t, ByteBuffer key, ByteBuffer value)
            {
                await Database.Executor.RunAsync(() => ITable.Replace(t, key, value));
            }

            public async Task<long> WalkAsync(Func<byte[], byte[], bool> callback)
            {
                long count = 0;
                await Database.Executor.RunAsync(() => count = ITable.Walk(callback));
                return count;
            }

            public async Task<long> WalkKeyAsync(Func<byte[], bool> callback)
            {
                long count = 0;
                await Database.Executor.RunAsync(() => count = ITable.WalkKey(callback));
                return count;
            }
        }

        /// <summary>
        /// 由后台数据库直接支持的存储过程。
        /// 直接操作后台数据库，不经过cache。
        /// </summary>
        public interface IOperates
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

        private IOperates _DirectOperates;
        public IOperates DirectOperates
        {
            get
            {
                return _DirectOperates;
            }

            protected set
            {
                _DirectOperates = DatabaseConf.DisableOperates ? new NullOperates() : value;
            }
        }

        public class NullOperates : IOperates
        {
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

#if USE_DATABASE
    public sealed class DatabaseMySql : Database
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(DatabaseMySql));

        public MySqlConnectionStringBuilder MySqlConnectionStringBuilder { get; }

        public DatabaseMySql(Application zeze, Config.DatabaseConf databaseConf) : base(zeze, databaseConf)
        {
            DirectOperates = new OperatesMySql(this);
            MySqlConnectionStringBuilder = new MySqlConnectionStringBuilder(databaseConf.DatabaseUrl);
        }

        public override int MaxPoolSize => (int)MySqlConnectionStringBuilder.MaximumPoolSize;

        public class MySqlTrans : ITransaction
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
                GC.SuppressFinalize(this);

                try
                {
                    Transaction.Dispose();
                }
                catch(Exception ex)
                {
                    logger.Error(ex);
                }
                try
                {
                    Connection.Dispose();
                }
                catch (Exception ex)
                {
                    logger.Error(ex);
                }
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

        public override TransactionAsync BeginTransaction()
        {
            return new TransactionAsync(this, new MySqlTrans(DatabaseUrl));
        }

        public override TableAsync OpenTable(string name)
        {
            return new TableAsync(new TableMysql(this, name));
        }

        public sealed class OperatesMySql : IOperates
        {
            public DatabaseMySql Database { get; }

            public void SetInUse(int localId, string global)
            {
                while (true)
                {
                    try
                    {
                        using var connection = new MySqlConnection(Database.DatabaseUrl);
                        connection.Open();
                        var cmd = new MySqlCommand("_ZezeSetInUse_", connection)
                        {
                            CommandType = CommandType.StoredProcedure
                        };
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
                                throw new Exception("Insert LocalId Failed");
                            case 4:
                                throw new Exception("Global Not Equals");
                            case 5:
                                throw new Exception("Insert Global Failed");
                            case 6:
                                throw new Exception("Instance Greater Than One But No Global");
                            default:
                                throw new Exception("Unknown ReturnValue");
                        }
                    }
                    catch (SqlException ex)
                    {
                        if (false == ex.Message.Contains("Deadlock"))
                            throw;
                    }
                }
            }

            public int ClearInUse(int localId, string global)
            {
                using var connection = new MySqlConnection(Database.DatabaseUrl);
                connection.Open();
                var cmd = new MySqlCommand("_ZezeClearInUse_", connection)
                {
                    CommandType = CommandType.StoredProcedure
                };
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
                using var connection = new MySqlConnection(Database.DatabaseUrl);
                connection.Open();

                string sql = "SELECT data,version FROM _ZezeDataWithVersion_ WHERE id=@id";

                var cmd = new MySqlCommand(sql, connection);
                cmd.Parameters.Add("@id", MySqlDbType.VarBinary, int.MaxValue).Value = key.Copy();
                cmd.Prepare();

                using MySqlDataReader reader = cmd.ExecuteReader();
                while (reader.Read())
                {
                    byte[] value = (byte[])reader[0];
                    long version = reader.GetInt64(1);
                    return (ByteBuffer.Wrap(value), version);
                }
                return (null, 0);
            }

            public bool SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, ref long version)
            {
                if (key.Size == 0)
                    throw new Exception("key is empty.");

                using var connection = new MySqlConnection(Database.DatabaseUrl);
                connection.Open();
                var cmd = new MySqlCommand("_ZezeSaveDataWithSameVersion_", connection)
                {
                    CommandType = CommandType.StoredProcedure
                };
                cmd.Parameters.Add("@in_id", MySqlDbType.VarBinary, int.MaxValue).Value = key.Copy();
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

                using var connection = new MySqlConnection(Database.DatabaseUrl);
                connection.Open();

                string TableDataWithVersion =
                    @$"CREATE TABLE IF NOT EXISTS _ZezeDataWithVersion_ (
                        id VARBINARY({eMaxKeyLength}) NOT NULL PRIMARY KEY,
                        data LONGBLOB NOT NULL,
                        version bigint NOT NULL
                    )";
                new MySqlCommand(TableDataWithVersion, connection).ExecuteNonQuery();

                string ProcSaveDataWithSameVersion =
                    @$"Create procedure IF NOT EXISTS _ZezeSaveDataWithSameVersion_ (
                        IN    in_id VARBINARY({eMaxKeyLength}),
                        IN    in_data LONGBLOB,
                        INOUT inout_version bigint,
                        OUT   ReturnValue int
                    )
                    return_label:begin
                        DECLARE oldversionexsit BIGINT;
                        DECLARE ROWCOUNT int;

                        START TRANSACTION;
                        set ReturnValue=1;
                        select version INTO oldversionexsit from _ZezeDataWithVersion_ where id=in_id;
                        select COUNT(*) into ROWCOUNT from _ZezeDataWithVersion_ where id=in_id;
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

                        insert IGNORE into _ZezeDataWithVersion_ values(in_id,in_data,inout_version);
                        select ROW_COUNT() into ROWCOUNT;
                        if ROWCOUNT = 1 then
                            set ReturnValue=0;
                            COMMIT;
                            LEAVE return_label;
                        end if;
                        set ReturnValue=4;
                        if 1=1 then
                            ROLLBACK;
                        end if;
                        LEAVE return_label;
                    end;";
                new MySqlCommand(ProcSaveDataWithSameVersion, connection).ExecuteNonQuery();

                string TableInstances =
                    @"CREATE TABLE IF NOT EXISTS _ZezeInstances_ (localid int NOT NULL PRIMARY KEY)";
                new MySqlCommand(TableInstances, connection).ExecuteNonQuery();
                // zeze_global 使用 _ZezeDataWithVersion_ 存储。

                string ProcSetInUse =
                    @"Create procedure IF NOT EXISTS _ZezeSetInUse_ (
                        in in_localid int,
                        in in_global LONGBLOB,
                        out ReturnValue int
                    )
                    return_label:begin
                        DECLARE currentglobal LONGBLOB;
                        declare emptybinary LONGBLOB;
                        DECLARE InstanceCount int;
                        DECLARE ROWCOUNT int;

                        START TRANSACTION;
                        set ReturnValue=1;
                        if exists (select localid from _ZezeInstances_ where localid=in_localid) then
                            set ReturnValue=2;
                            ROLLBACK;
                            LEAVE return_label;
                        end if;
                        insert IGNORE into _ZezeInstances_ values(in_localid);
                        select ROW_COUNT() into ROWCOUNT;
                        if ROWCOUNT = 0 then
                            set ReturnValue=3;
                            ROLLBACK;
                            LEAVE return_label;
                        end if;
                        set emptybinary = BINARY '';
                        select data into currentglobal from _ZezeDataWithVersion_ where id=emptybinary;
                        select COUNT(*) into ROWCOUNT from _ZezeDataWithVersion_ where id=emptybinary;
                        if ROWCOUNT > 0 then
                            if currentglobal <> in_global then
                                set ReturnValue=4;
                                ROLLBACK;
                                LEAVE return_label;
                            end if;
                        else
                            insert IGNORE into _ZezeDataWithVersion_ values(emptybinary, in_global, 0);
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
                        if 1=1 then
                            COMMIT;
                        end if;
                        LEAVE return_label;
                    end";
                new MySqlCommand(ProcSetInUse, connection).ExecuteNonQuery();

                string ProcClearInUse =
                    @"Create procedure IF NOT EXISTS _ZezeClearInUse_ (
                        in in_localid int,
                        in in_global LONGBLOB,
                        out ReturnValue int
                    )
                    return_label:begin
                        DECLARE InstanceCount int;
                        declare emptybinary LONGBLOB;
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
                        if 1=1 then
                            COMMIT;
                        end if;
                        LEAVE return_label;
                    end";
                new MySqlCommand(ProcClearInUse, connection).ExecuteNonQuery();
            }
        }

        public sealed class TableMysql : Database.ITable
        {
            public DatabaseMySql DatabaseReal { get; }
            public Database Database => DatabaseReal;
            public string Name { get; }
            private readonly bool isNew;
            public bool IsNew => isNew;

            public TableMysql(DatabaseMySql database, string name)
            {
                DatabaseReal = database;
                Name = name;
                {
                    using var conn = new MySqlConnection(database.DatabaseUrl);
                    conn.Open();
                    string sql = $"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '{conn.Database}' AND table_name = '{name}'";
                    var cmd = new MySqlCommand(sql, conn);
                    isNew = (long)cmd.ExecuteScalar() == 0;
                }
                {
                    using var connection = new MySqlConnection(database.DatabaseUrl);
                    connection.Open();
                    string sql = @$"CREATE TABLE IF NOT EXISTS {Name}
                                    (id VARBINARY({eMaxKeyLength}) NOT NULL PRIMARY KEY, value LONGBLOB NOT NULL)";
                    var cmd = new MySqlCommand(sql, connection);
                    cmd.ExecuteNonQuery();
                }
            }

            public void Close()
            {
            }

            public ByteBuffer Find(ByteBuffer key)
            {
                using var connection = new MySqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string sql = "SELECT value FROM " + Name + " WHERE id = @ID";
                // 是否可以重用 SqlCommand
                var cmd = new MySqlCommand(sql, connection);
                cmd.Parameters.Add("@ID", MySqlDbType.VarBinary, int.MaxValue).Value = key.Copy();
                cmd.Prepare();

                using MySqlDataReader reader = cmd.ExecuteReader();
                while (reader.Read())
                {
                    byte[] value = (byte[])reader[0];
                    return ByteBuffer.Wrap(value);
                }
                return null;
            }

            public void Remove(ITransaction t, ByteBuffer key)
            {
                var my = t as MySqlTrans;
                string sql = "DELETE FROM " + Name + " WHERE id=@ID";
                var cmd = new MySqlCommand(sql, my.Connection, my.Transaction);
                cmd.Parameters.Add("@ID", MySqlDbType.VarBinary, int.MaxValue).Value = key.Copy();
                cmd.Prepare();
                cmd.ExecuteNonQuery();
            }

            public void Replace(ITransaction t, ByteBuffer key, ByteBuffer value)
            {
                var my = t as MySqlTrans;
                string sql = "REPLACE INTO " + Name + " values(@ID,@VALUE)";
                var cmd = new MySqlCommand(sql, my.Connection, my.Transaction);
                cmd.Parameters.Add("@ID", MySqlDbType.VarBinary, int.MaxValue).Value = key.Copy();
                cmd.Parameters.Add("@VALUE", MySqlDbType.VarBinary, int.MaxValue).Value = value.Copy();
                cmd.Prepare();
                cmd.ExecuteNonQuery();
            }

            public long Walk(Func<byte[], byte[], bool> callback)
            {
                using var connection = new MySqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string sql = "SELECT id,value FROM " + Name;
                var cmd = new MySqlCommand(sql, connection);
                cmd.Prepare();

                long count = 0;
                using MySqlDataReader reader = cmd.ExecuteReader();
                while (reader.Read())
                {
                    byte[] key = (byte[])reader[0];
                    byte[] value = (byte[])reader[1];
                    ++count;
                    if (false == callback(key, value))
                        break;
                }
                return count;
            }

            public long WalkKey(Func<byte[], bool> callback)
            {
                using var connection = new MySqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string sql = "SELECT id FROM " + Name;
                var cmd = new MySqlCommand(sql, connection);
                cmd.Prepare();

                long count = 0;
                using MySqlDataReader reader = cmd.ExecuteReader();
                while (reader.Read())
                {
                    byte[] key = (byte[])reader[0];
                    ++count;
                    if (false == callback(key))
                        break;
                }
                return count;
            }
        }
    }

    public sealed class DatabaseSqlServer : Database
    {
        private static readonly ILogger logger = LogManager.GetLogger(typeof(DatabaseSqlServer));

        public SqlConnectionStringBuilder SqlConnectionStringBuilder { get; }

        public DatabaseSqlServer(Application zeze, Config.DatabaseConf databaseConf) : base(zeze, databaseConf)
        {
            DirectOperates = new OperatesSqlServer(this);
            SqlConnectionStringBuilder = new SqlConnectionStringBuilder(databaseConf.DatabaseUrl);
        }

        public override int MaxPoolSize => SqlConnectionStringBuilder.MaxPoolSize;

        public class SqlTrans : ITransaction
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
                GC.SuppressFinalize(this);

                try
                {
                    Transaction.Dispose();
                }
                catch (Exception ex)
                {
                    logger.Error(ex);
                }
                try
                {
                    Connection.Dispose();
                }
                catch (Exception ex)
                {
                    logger.Error(ex);
                }
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

        public override TransactionAsync BeginTransaction()
        {
            return new TransactionAsync(this, new SqlTrans(DatabaseUrl));
        }

        public override TableAsync OpenTable(string name)
        {
            return new TableAsync(new TableSqlServer(this, name));
        }

        public sealed class OperatesSqlServer : IOperates
        {
            public DatabaseSqlServer DatabaseReal { get; }
            public Database Database => DatabaseReal;

            public void SetInUse(int localId, string global)
            {
                using var connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();
                var cmd = new SqlCommand("_ZezeSetInUse_", connection)
                {
                    CommandType = CommandType.StoredProcedure
                };
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
                        throw new Exception("Insert LocalId Failed");
                    case 4:
                        throw new Exception("Global Not Equals");
                    case 5:
                        throw new Exception("Insert Global Failed");
                    case 6:
                        throw new Exception("Instance Greater Than One But No Global");
                    default:
                        throw new Exception("Unknown ReturnValue");
                }

            }

            public int ClearInUse(int localId, string global)
            {
                using var connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();
                var cmd = new SqlCommand("_ZezeClearInUse_", connection)
                {
                    CommandType = CommandType.StoredProcedure
                };
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
                using var connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string sql = "SELECT data,version FROM _ZezeDataWithVersion_ WHERE id=@id";

                var cmd = new SqlCommand(sql, connection);
                cmd.Parameters.Add("@id", SqlDbType.VarBinary, int.MaxValue).Value = key.Copy();
                cmd.Prepare();

                using SqlDataReader reader = cmd.ExecuteReader();
                while (reader.Read())
                {
                    byte[] value = (byte[])reader[0];
                    long version = reader.GetInt64(1);
                    return (ByteBuffer.Wrap(value), version);
                }
                return (null, 0);
            }

            public bool SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, ref long version)
            {
                if (key.Size == 0)
                    throw new Exception("key is empty.");

                using var connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();
                var cmd = new SqlCommand("_ZezeSaveDataWithSameVersion_", connection)
                {
                    CommandType = CommandType.StoredProcedure
                };
                cmd.Parameters.Add("@id", SqlDbType.VarBinary, int.MaxValue).Value = key.Copy();
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

                using var connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string TableDataWithVersion
                    = @$"if not exists (select * from sysobjects where name='_ZezeDataWithVersion_' and xtype='U')
                        CREATE TABLE _ZezeDataWithVersion_ (id VARBINARY({eMaxKeyLength}) NOT NULL PRIMARY KEY, 
                        data VARBINARY(MAX) NOT NULL, version bigint NOT NULL)";
                new SqlCommand(TableDataWithVersion, connection).ExecuteNonQuery();

                string ProcSaveDataWithSameVersion =
                    @$"Create or Alter procedure _ZezeSaveDataWithSameVersion_
                        @id VARBINARY({eMaxKeyLength}),
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

insert into _ZezeDataWithVersion_ values(@id,@data,@version) select id,data,version from _ZezeDataWithVersion_ where NOT EXISTS (select id where id=@id)

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
insert into _ZezeInstances_ values(@localid) select localid from _ZezeInstances_ where NOT EXISTS (select localid from _ZezeInstances_ where localid=@localid)
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
insert into _ZezeDataWithVersion_ values(@emptybinary,@global,0) select id,data,version from _ZezeDataWithVersion_ where NOT EXISTS (select id where id=@emptybinary)
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

        public sealed class TableSqlServer : Database.ITable
        {
            public DatabaseSqlServer DatabaseReal { get; }
            public Database Database => DatabaseReal;
            public string Name { get; }
            private readonly bool isNew;
            public bool IsNew => isNew;

            public TableSqlServer(DatabaseSqlServer database, string name)
            {
                DatabaseReal = database;
                Name = name;
                {
                    using var conn = new SqlConnection(DatabaseReal.DatabaseUrl);
                    conn.Open();
                    var sql = $"SELECT count(*) FROM dbo.sysobjects where id = object_id('[dbo].[{name}]')";
                    var cmd = new SqlCommand(sql, conn);
                    isNew = (int)cmd.ExecuteScalar() == 0;
                }
                {
                    using SqlConnection connection = new(DatabaseReal.DatabaseUrl);
                    connection.Open();

                    string sql = @$"if not exists (select * from sysobjects where name='{Name}' and xtype='U') CREATE TABLE {Name}
                                    (id VARBINARY({eMaxKeyLength}) NOT NULL PRIMARY KEY, value VARBINARY(MAX) NOT NULL)";
                    var cmd = new SqlCommand(sql, connection);
                    cmd.ExecuteNonQuery();
                }
            }

            public void Close()
            {
            }

            public ByteBuffer Find(ByteBuffer key)
            {
                using var connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string sql = "SELECT value FROM " + Name + " WHERE id = @ID";

                // 是否可以重用 SqlCommand
                var cmd = new SqlCommand(sql, connection);
                cmd.Parameters.Add("@ID", System.Data.SqlDbType.VarBinary, int.MaxValue).Value = key.Copy();
                cmd.Prepare();

                using SqlDataReader reader = cmd.ExecuteReader();
                while (reader.Read())
                {
                    byte[] value = (byte[])reader[0];
                    return ByteBuffer.Wrap(value);
                }
                return null;
            }

            public void Remove(ITransaction t, ByteBuffer key)
            {
                var my = t as SqlTrans;
                string sql = "DELETE FROM " + Name + " WHERE id=@ID";
                var cmd = new SqlCommand(sql, my.Connection, my.Transaction);
                cmd.Parameters.Add("@ID", System.Data.SqlDbType.VarBinary, int.MaxValue).Value = key.Copy();
                cmd.Prepare();
                cmd.ExecuteNonQuery();
            }

            public void Replace(ITransaction t, ByteBuffer key, ByteBuffer value)
            {
                var my = t as SqlTrans;
                string sql = "update " + Name + " set value=@VALUE where id=@ID"
                    + " if @@rowcount = 0 and @@error = 0 insert into " + Name + " values(@ID,@VALUE)";

                var cmd = new SqlCommand(sql, my.Connection, my.Transaction);
                cmd.Parameters.Add("@ID", System.Data.SqlDbType.VarBinary, int.MaxValue).Value = key.Copy();
                cmd.Parameters.Add("@VALUE", System.Data.SqlDbType.VarBinary, int.MaxValue).Value = value.Copy();
                cmd.Prepare();
                cmd.ExecuteNonQuery();
            }

            public long Walk(Func<byte[], byte[], bool> callback)
            {
                using var connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string sql = "SELECT id,value FROM " + Name;
                var cmd = new SqlCommand(sql, connection);
                cmd.Prepare();

                long count = 0;
                using SqlDataReader reader = cmd.ExecuteReader();
                while (reader.Read())
                {
                    byte[] key = (byte[])reader[0];
                    byte[] value = (byte[])reader[1];
                    ++count;
                    if (false == callback(key, value))
                        break;
                }
                return count;
            }

            public long WalkKey(Func<byte[], bool> callback)
            {
                using var connection = new SqlConnection(DatabaseReal.DatabaseUrl);
                connection.Open();

                string sql = "SELECT id FROM " + Name;
                var cmd = new SqlCommand(sql, connection);
                cmd.Prepare();

                long count = 0;
                using SqlDataReader reader = cmd.ExecuteReader();
                while (reader.Read())
                {
                    byte[] key = (byte[])reader[0];
                    ++count;
                    if (false == callback(key))
                        break;
                }
                return count;
            }
        }
    }

    public class DatabaseRocksDb : Database
    {
        private readonly RocksDb Db;
        private readonly WriteOptions WriteOptions = new();
        private readonly ReadOptions ReadOptions = new();
        private readonly ColumnFamilyOptions CfOptions = new();
        private readonly DbOptions DbOptions = new();
        private readonly ConcurrentDictionary<string, string> ColumnFamilies = new();

        public DatabaseRocksDb(Application zeze, Config.DatabaseConf databaseConf) : base(zeze, databaseConf)
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
            Db = RocksDb.Open(DbOptions, databaseConf.DatabaseUrl, columnFamilies);
            DirectOperates = new OperatesRocksDb(this);
        }

        public override int MaxPoolSize => 100;

        public override void Close()
        {
            base.Close();
            Db.Dispose();
        }

        public class RocksDbTrans : ITransaction
        {
            private readonly DatabaseRocksDb Database;
            private readonly WriteBatch Batch;

            public RocksDbTrans(DatabaseRocksDb database)
            {
                Database = database;
                Batch = new WriteBatch();
            }

            public void Dispose()
            {
                GC.SuppressFinalize(this);

                Batch?.Dispose();
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

        public override TransactionAsync BeginTransaction()
        {
            return new TransactionAsync(this, new RocksDbTrans(this));
        }

        public override TableAsync OpenTable(string name)
        {
            bool isNew = false;
            ColumnFamilies.GetOrAdd(name, (key) =>
            {
                isNew = true;
                Db.CreateColumnFamily(CfOptions, key);
                return key;
            });
            return new TableAsync(new TableRocksDb(this, name, isNew));
        }

        public sealed class TableRocksDb : Database.ITable
        {
            public DatabaseRocksDb DatabaseReal { get; }
            public Database Database => DatabaseReal;
            public string Name { get; }
            private ColumnFamilyHandle ColumnFamily { get; }
            private readonly bool isNew;
            public bool IsNew => isNew;

            public TableRocksDb(DatabaseRocksDb database, string name, bool isNew)
            {
                DatabaseReal = database;
                Name = name;
                ColumnFamily = DatabaseReal.Db.GetColumnFamily(name);
                this.isNew = isNew;
            }

            public void Close()
            {
            }

            private static (byte[], int) GetBytes(ByteBuffer bb)
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

            public void Remove(ITransaction t, ByteBuffer _key)
            {
                var txn = t as RocksDbTrans;
                var (key, keylen) = GetBytes(_key);
                txn.Remove(key, keylen, ColumnFamily);
            }

            public void Replace(ITransaction t, ByteBuffer _key, ByteBuffer _value)
            {
                var txn = t as RocksDbTrans;
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

            public long WalkKey(Func<byte[], bool> callback)
            {
                var it = DatabaseReal.Db.NewIterator(ColumnFamily, DatabaseReal.ReadOptions);
                try
                {
                    long countWalked = 0;
                    it.SeekToFirst();
                    while (it.Valid())
                    {
                        ++countWalked;
                        if (false == callback(it.Key()))
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

        public sealed class OperatesRocksDb : IOperates
        {
            public DatabaseRocksDb DatabaseReal { get; }
            public Database Database => DatabaseReal;
            public const string ColumnFamilyName = "zeze.OperatesRocksDb.Schemas";
            private readonly ColumnFamilyHandle ColumnFamily;

            public OperatesRocksDb(DatabaseRocksDb database)
            {
                DatabaseReal = database;
                if (false == DatabaseReal.ColumnFamilies.ContainsKey(ColumnFamilyName))
                {
                    DatabaseReal.Db.CreateColumnFamily(DatabaseReal.CfOptions, ColumnFamilyName);
                }
                ColumnFamily = DatabaseReal.Db.GetColumnFamily(ColumnFamilyName);
            }

            public int ClearInUse(int localId, string global)
            {
                // rocksdb 独占由它自己打开的时候保证。
                return 0;
            }

            private class DataWithVersion : Zeze.Serialize.Serializable
            {
                public ByteBuffer Data { get; set; }
                public long Version { get; set; }

                public void Decode(ByteBuffer bb)
                {
                    Data = ByteBuffer.Wrap(bb.ReadBytes());
                    Version = bb.ReadLong();
                }

                public void Encode(ByteBuffer bb)
                {
                    bb.WriteByteBuffer(Data);
                    bb.WriteLong(Version);
                }

                public static DataWithVersion Decode(byte[] bytes)
                {
                    if (null == bytes)
                        return new DataWithVersion();
                    var dv = new DataWithVersion();
                    dv.Decode(ByteBuffer.Wrap(bytes));
                    return dv;
                }

                public byte[] Encode()
                {
                    var bb = ByteBuffer.Allocate();
                    this.Encode(bb);
                    return bb.Copy();
                }
            }

            public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key)
            {
                lock (this)
                {
                    var dv = DataWithVersion.Decode(DatabaseReal.Db.Get(key.Copy(), ColumnFamily));
                    return (dv.Data, dv.Version);
                }
            }

            public bool SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, ref long version)
            {
                lock (this)
                {
                    var dv = DataWithVersion.Decode(DatabaseReal.Db.Get(key.Copy(), ColumnFamily));
                    if (dv.Version != version)
                        return false;

                    version++;
                    dv.Version = version;
                    dv.Data = data;
                    DatabaseReal.Db.Put(key.Copy(), dv.Encode(), ColumnFamily);
                    return true;
                }
            }

            public void SetInUse(int localId, string global)
            {
                // rocksdb 独占由它自己打开的时候保证。
            }
        }
    }
#endif // USE_DATABASE
    /// <summary>
    /// Zeze.Transaction.Table.storage 为 null 时，就表示内存表了。这个实现是为了测试 checkpoint 流程。
    /// </summary>
    public sealed class DatabaseMemory : Database
    {
        private readonly ProceduresMemory _ProceduresMemory = new();

        public DatabaseMemory(Application zeze, Config.DatabaseConf databaseConf) : base(zeze, databaseConf)
        {
            DirectOperates = _ProceduresMemory;
        }

        public override int MaxPoolSize => 100;

        public class ByteArrayComparer : IEqualityComparer<byte[]>
        {
            public bool Equals(byte[] left, byte[] right)
            {
                return ByteBuffer.Equals(left, right);
            }

            public int GetHashCode(byte[] key)
            {
                return FixedHash.calc_hashnr(key, 0, key.Length);
            }
        }

        public class ProceduresMemory : IOperates
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

            private readonly Dictionary<byte[], DataWithVersion> DataWithVersions = new(new ByteArrayComparer());

            public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key)
            {
                lock (DataWithVersions)
                {
                    if (DataWithVersions.TryGetValue(key.Copy(), out var exist))
                    {
                        return (ByteBuffer.Wrap(exist.Data.Copy()), exist.Version);
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

        internal static byte[] NullBytes = Array.Empty<byte>();

        public class MemTrans : ITransaction
        {
            private readonly DatabaseMemory Database;
            private readonly ConcurrentDictionary<string, ConcurrentDictionary<ByteBuffer, byte[]>> batch = new();

            public MemTrans(DatabaseMemory db)
            {
                Database = db;
            }

            public void Dispose()
            {
                GC.SuppressFinalize(this);
            }


            public void Commit()
            {
                // 整个db同步。
                lock(databaseTables)
                {
                    foreach (var e in batch)
                    {
                        var db = databaseTables.GetOrAdd(Database.DatabaseUrl, url => new());
                        var tableasync = db.GetOrAdd(e.Key, tn => new TableAsync(new TableMemory(Database, tn)));
                        var table = tableasync.ITable as TableMemory;
                        foreach (var r in e.Value)
                        {
                            if (r.Value == NullBytes)
                            {
                                table.Map.TryRemove(r.Key, out _);
                            }
                            else
                            {
                                table.Map[r.Key] = r.Value;
                            }
                        }
                    }
                }
            }

            public void Remove(string tableName, ByteBuffer key)
            {
                var table = batch.GetOrAdd(tableName, tn => new ConcurrentDictionary<ByteBuffer, byte[]>());
                table[ByteBuffer.Wrap(key.Copy())] = NullBytes;
            }

            public void Replace(string tableName, ByteBuffer key, ByteBuffer value)
            {
                var table = batch.GetOrAdd(tableName, tn => new ConcurrentDictionary<ByteBuffer, byte[]>());
                table[ByteBuffer.Wrap(key.Copy())] = value.Copy();
            }

            // 仅支持从一个db原子的查询数据。

            // 多表原子查询。
            public IDictionary<string, IDictionary<ByteBuffer, ByteBuffer>> Finds(IDictionary<string, ISet<ByteBuffer>> tableKeys)
            {
                var result = new Dictionary<string, IDictionary<ByteBuffer, ByteBuffer>>();
                lock (databaseTables)
                {
                    foreach (var tks in tableKeys)
                    {
                        var tableName = tks.Key;
                        var db = databaseTables.GetOrAdd(Database.DatabaseUrl, url => new ());
                        var table = db.GetOrAdd(tableName, tn => new TableAsync(new TableMemory(Database, tn)));
                        var tableFinds = new Dictionary<ByteBuffer, ByteBuffer>();
                        result.Add(tableName, tableFinds);
                        foreach (var key in tks.Value)
                        {
                            tableFinds.Add(key, table.ITable.Find(key)); // also put null value
                        }
                    }
                }
                return result;
            }

            // 单表原子查询
            public IDictionary<ByteBuffer, ByteBuffer> Finds(string tableName, ISet<ByteBuffer> keys)
            {
                var result = new Dictionary<ByteBuffer, ByteBuffer>();
                lock (databaseTables)
                {
                    var db = databaseTables.GetOrAdd(Database.DatabaseUrl, url => new());
                    var table = db.GetOrAdd(tableName, tn => new TableAsync(new TableMemory(Database, tn)));
                    foreach (var key in keys)
                    {
                        result.Add(key, table.ITable.Find(key)); // also put null value
                    }
                }
                return result;
            }

            public void Rollback()
            {
            }
        }

        public override TransactionAsync BeginTransaction()
        {
            return new TransactionAsync(this, new MemTrans(this));
        }

        private static readonly ConcurrentDictionary<string, ConcurrentDictionary<string, TableAsync>> databaseTables = new();
  
        public override TableAsync OpenTable(string name)
        {
            var tables = databaseTables.GetOrAdd(DatabaseUrl, (_) => new());
            return tables.GetOrAdd(name, (tablenamenotused) => new TableAsync(new TableMemory(this, name)));
        }

        public sealed class TableMemory : Database.ITable
        {
            public DatabaseMemory DatabaseReal { get; }
            public Database Database => DatabaseReal;
            public string Name { get; }
            public bool IsNew => true;
            public TableMemory(DatabaseMemory db, string name)
            {
                DatabaseReal = db;
                Name = name;
            }

            public ConcurrentDictionary<ByteBuffer, byte[]> Map { get; } = new ConcurrentDictionary<ByteBuffer, byte[]>();

            public ByteBuffer Find(ByteBuffer key)
            {
                if (Map.TryGetValue(key, out var value))
                {
                    return ByteBuffer.Wrap(ByteBuffer.Copy(value));
                }
                return null;
            }

            public void Remove(ITransaction t, ByteBuffer key)
            {
                var mt = (MemTrans)t;
                mt.Remove(Name, key);
            }

            public void Replace(ITransaction t, ByteBuffer key, ByteBuffer value)
            {
                var mt = (MemTrans)t;
                mt.Replace(Name, key, value);
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
                        if (false == callback(e.Key.Copy(), ByteBuffer.Copy(e.Value)))
                            break;
                    }
                    return count;
                }
            }

            public long WalkKey(Func<byte[], bool> callback)
            {
                lock (this)
                {
                    // 不允许并发？
                    long count = 0;
                    foreach (var e in Map)
                    {
                        ++count;
                        if (false == callback(e.Key.Copy()))
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
