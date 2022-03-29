using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Zeze.Serialize;
using Zeze.Transaction;

namespace Zeze.Tikv
{
    public sealed class DatabaseTikv : Database
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public DatabaseTikv(Application zeze, string databaseUrl)
            : base(zeze, databaseUrl)
        {
            DirectOperates = new OperatesTikv(this);
        }

        public override int MaxPoolSize => 100;

        public class TikvTrans : Database.ITransaction
        {
            public TikvConnection Connection { get; }
            public TikvTransaction Transaction { get; }

            public TikvTrans(string DatabaseUrl)
            {
                Connection = new TikvConnection(DatabaseUrl);
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
            return new TransactionAsync(this, new TikvTrans(DatabaseUrl));
        }

        public override TableAsync OpenTable(string name)
        {
            return new TableAsync(new TableTikv(this, name));
        }

        public sealed class OperatesTikv : IOperates
        {
            public DatabaseTikv Database { get; }

            public OperatesTikv(DatabaseTikv tikv)
            {
                Database = tikv;
            }

            public int ClearInUse(int localId, string global)
            {
                return 0;
            }

            public void SetInUse(int localId, string global)
            {
            }

            public (ByteBuffer, long) GetDataWithVersion(ByteBuffer key)
            {
                return (null, 0);
            }

            public bool SaveDataWithSameVersion(ByteBuffer key, ByteBuffer data, ref long version)
            {
                return true;
            }
        }

        public sealed class TableTikv : Database.ITable
        {
            public DatabaseTikv DatabaseReal { get; }
            public Database Database => DatabaseReal;
            public string Name { get; }
            public ByteBuffer KeyPrefix { get; }
            public bool IsNew => false; // always Enable Schemas.Check

            public TableTikv(DatabaseTikv database, string name)
            {
                DatabaseReal = database;
                Name = name;
                var nameutf8 = Encoding.UTF8.GetBytes(name);
                KeyPrefix = ByteBuffer.Allocate(nameutf8.Length + 1);
                KeyPrefix.Append(nameutf8);
                KeyPrefix.WriteByte(0);
            }

            public void Close()
            {
            }

            private ByteBuffer WithKeyspace(ByteBuffer key)
            {
                var tikvKey = ByteBuffer.Allocate(KeyPrefix.Size + key.Size);
                tikvKey.Append(KeyPrefix.Bytes, KeyPrefix.ReadIndex, KeyPrefix.Size);
                tikvKey.Append(key.Bytes, key.ReadIndex, key.Size);
                return tikvKey;
            }

            public ByteBuffer Find(ByteBuffer key)
            {
                using TikvConnection connection = new(DatabaseReal.DatabaseUrl);
                connection.Open();
                using TikvTransaction transaction = connection.BeginTransaction();
                var result = Tikv.Driver.Get(transaction.TransactionId, WithKeyspace(key));
                transaction.Commit();
                return result;
            }

            public void Remove(ITransaction t, ByteBuffer key)
            {
                var my = t as TikvTrans;
                Tikv.Driver.Delete(my.Connection.Transaction.TransactionId, WithKeyspace(key));
            }

            public void Replace(ITransaction t, ByteBuffer key, ByteBuffer value)
            {
                var my = t as TikvTrans;
                Tikv.Driver.Put(my.Connection.Transaction.TransactionId, WithKeyspace(key), value);
            }

            public long Walk(Func<byte[], byte[], bool> callback)
            {
                using TikvConnection connection = new(DatabaseReal.DatabaseUrl);
                connection.Open();
                using TikvTransaction transaction = connection.BeginTransaction();
                long result = Tikv.Driver.Scan(transaction.TransactionId, KeyPrefix, callback);
                transaction.Commit();
                return result;
            }
        }
    }
}

