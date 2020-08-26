using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using System.Collections.Concurrent;
using System.Data.SqlClient;
using MySql.Data.MySqlClient;
using NLog;
using System.Threading;

namespace Zeze.Transaction
{
    /// <summary>
    /// 数据访问的效率主要来自TableCache的命中。根据以往的经验，命中率是很高的。
    /// 所以数据库层就不要求很高的效率。马马虎虎就可以了。
    /// 考虑：
    /// 1 单个Zeze实例多数据库支持，一张表只能在一个数据库，但是不同的表可以在不同的数据库。
    /// 2 多Zeze实例支持，每个实例的表都是一样的，但是连接不同的数据库。
    /// 3 Checkpoint 时每个数据库一个Event，Commit前设置自己Event.Ready。
    ///   然后等待所有Event.Ready。最后一起Commit。
    /// </summary>
    public abstract class Database
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        private Dictionary<string, Zeze.Transaction.Table> tables = new Dictionary<string, Zeze.Transaction.Table>();
        internal List<Storage> storages = new List<Storage>();

        public void AddTable(Zeze.Transaction.Table table)
        {
            tables.Add(table.Name, table);
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

        public void Checkpoint()
        {
            // try Encode. 可以多趟。
            for (int i = 1; i <= 1; ++i)
            {
                int countEncodeN = 0;
                foreach (Storage storage in storages)
                {
                    countEncodeN += storage.EncodeN();
                }
                logger.Info("Checkpoint EncodeN {0}/{1}", i, countEncodeN);
            }
            // snapshot
            {
                int countEncode0 = 0;
                int countSnapshot = 0;
                Transaction.FlushReadWriteLock.EnterWriteLock();
                try
                {
                    foreach (Storage storage in storages)
                    {
                        countEncode0 += storage.Encode0();
                    }
                    foreach (Storage storage in storages)
                    {
                        countSnapshot += storage.Snapshot();
                    }
                }
                finally
                {
                    Transaction.FlushReadWriteLock.ExitWriteLock();
                }

                logger.Info("Checkpoint Encode0 And Snapshot countEncode0={0} countSnapshot={1}", countEncode0, countSnapshot);
            }

            // flush checkpoint
            Checkpoint(() =>
            {
                int countFlush = 0;
                foreach (Storage storage in storages)
                {
                    countFlush += storage.Flush();
                }
                logger.Info("Checkpoint Flush count={0}", countFlush);
            }
            );

            // cleanup
            foreach (Storage storage in storages)
            {
                storage.Cleanup();
            }
        }

    // 下面的代码是多数据库一起提交的大概实现。
    // 目前Storage没有交给Database管理，所以下面代码还不能工作。
    // see Zeze.cs。实现这个还需要考虑 Close Database 。
    // 这个伪码存在应用接口不明确的问题。
    // 另一个实现方案是，提供Checkpoint类，把所有Database加入。一起Checkpoint。
    // 需要重构代码，把 Storage 管理交给 Database，暴露 Database 出去。让应用决定一起提交哪几个Database。
    /*
    private static HashSet<Database> Databases = new HashSet<Database>();
    private ManualResetEvent _readyToCommit = new ManualResetEvent(false);
    public Database()
    {
        lock (Databases)
        {
            Databases.Add(this);
        }
    }

    private static void WaitAllCommitReady()
    {
        ManualResetEvent[] handles = new ManualResetEvent[Databases.Count];

        int i = 0;
        foreach (var v in Databases)
        {
            handles[i++] = v._readyToCommit;
        }

        WaitHandle.WaitAll(handles);
        // howto safe reset events.
        foreach (var v in handles)
        {
            v.Reset();
        }
    }

    protected void SetCommitReadyAndWaitAll()
    {
        _readyToCommit.Set();
        WaitAllCommitReady();
    }
    */

        public abstract void Checkpoint(Action flushAction);
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
    }

    public class DatabaseMySql : Database
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public string ConnectionString { get; }
        internal MySqlConnection CheckpointSqlConnection { get; private set; }
        internal MySqlTransaction Transaction { get; private set; }

        public DatabaseMySql(string connectionString)
        {
            ConnectionString = connectionString;
        }

        public override void Checkpoint(Action flushAction)
        {
            try
            {
                for (int i = 0; i < 50; ++i)
                {
                    using MySqlConnection connection = new MySqlConnection(ConnectionString);
                    CheckpointSqlConnection = connection;
                    connection.Open();
                    Transaction = connection.BeginTransaction();
                    try
                    {
                        flushAction();
                        Transaction.Commit();
                        return;
                    }
                    catch (Exception ex)
                    {
                        Transaction.Rollback();
                        logger.Warn(ex, "Checkpoint error.");
                    }
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

        public class TableMysql : Database.Table
        {
            public DatabaseMySql Database { get; }
            public string Name { get; }

            public TableMysql(DatabaseMySql database, string name)
            {
                Database = database;
                Name = name;

                using MySqlConnection connection = new MySqlConnection();
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
                using MySqlConnection connection = new MySqlConnection(Database.ConnectionString);
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
                using MySqlConnection connection = new MySqlConnection(Database.ConnectionString);
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

    public class DatabaseSqlServer : Database
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public string ConnectionString { get; }
        internal SqlConnection CheckpointSqlConnection { get; private set; }
        internal SqlTransaction Transaction { get; private set; }

        public DatabaseSqlServer(string connectionString)
        {
            ConnectionString = connectionString;
        }

        public override void Checkpoint(Action flushAction)
        {
            try
            {
                for (int i = 0; i < 50; ++i)
                {
                    using SqlConnection connection = new SqlConnection(ConnectionString);
                    CheckpointSqlConnection = connection;
                    connection.Open();
                    Transaction = connection.BeginTransaction();
                    try
                    {
                        flushAction();
                        Transaction.Commit();
                        return;
                    }
                    catch (Exception ex)
                    {
                        Transaction.Rollback();
                        logger.Warn(ex, "Checkpoint error.");
                    }
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

        public class TableSqlServer : Database.Table
        {
            public DatabaseSqlServer Database { get; }
            public string Name { get; }

            public TableSqlServer(DatabaseSqlServer database, string name)
            {
                Database = database;
                Name = name;

                using SqlConnection connection = new SqlConnection(Database.ConnectionString);
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
                using SqlConnection connection = new SqlConnection(Database.ConnectionString);
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
                using SqlConnection connection = new SqlConnection(Database.ConnectionString);
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

    /// <summary>
    /// Zeze.Transaction.Table.storage 为 null 时，就表示内存表了。这个实现是为了测试 checkpoint 流程。
    /// </summary>
    public class DatabaseMemory : Database
    {
        public override void Checkpoint(Action flushAction)
        {
            flushAction();
        }

        public override Database.Table OpenTable(string name)
        {
            return new TableMemory();
        }

        public class TableMemory : Database.Table
        {
            public class ByteArrayComparer : IEqualityComparer<byte[]>
            {
                public bool Equals(byte[] left, byte[] right)
                {
                    if (left == null || right == null)
                    {
                        return left == right;
                    }
                    if (left.Length != right.Length)
                    {
                        return false;
                    }
                    for (int i = 0; i < left.Length; i++)
                    {
                        if (left[i] != right[i])
                        {
                            return false;
                        }
                    }
                    return true;
                }
                public int GetHashCode(byte[] key)
                {
                    int sum = 0;
                    foreach (byte cur in key)
                    {
                        sum += cur;
                    }
                    return sum;
                }
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
                Map.Remove(key.Copy(), out var notused);
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
