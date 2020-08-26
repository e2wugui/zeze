using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Serialize;
using System.Collections.Concurrent;
using System.Data.SqlClient;
using MySql.Data.MySqlClient;
using NLog;

namespace Zeze.Transaction
{
    public interface Database
    {
        public void Close();
        public void Checkpoint(Action flushAction);
        public Database.Table OpenTable(string name);

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

        public void Checkpoint(Action flushAction)
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

        public void Close()
        {
        }

        public Database.Table OpenTable(string name)
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

        public void Checkpoint(Action flushAction)
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

        public void Close()
        {
        }

        public Database.Table OpenTable(string name)
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
        public void Checkpoint(Action flushAction)
        {
            flushAction();
        }

        public void Close()
        {
        }
        public Database.Table OpenTable(string name)
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
