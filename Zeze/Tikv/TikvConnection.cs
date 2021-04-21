using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Tikv
{
    public class TikvConnection : IDisposable
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public long ClientId { get; }
        public TikvTransaction Transaction { get; private set; }
        public bool Disposed { get; private set; } = false;

        public const int MinPoolSize = 16;
        public const int MaxPoolSize = 256;

        private static BlockingCollection<long> Pools = new BlockingCollection<long>();
        public static Zeze.Util.AtomicLong UsingCount { get; } = new Util.AtomicLong();

        public TikvConnection(string databaseUrl)
        {
            UsingCount.IncrementAndGet();
            while (true)
            {
                if (false == Pools.TryTake(out var clientId))
                    break;
                ClientId = clientId;
                if (ClientId >= 0)
                    return;
            }
            while (UsingCount.Get() > MaxPoolSize)
            {
                // 使用timeout，更可靠点。
                if (Pools.TryTake(out var clientId, 1000))
                {
                    ClientId = clientId;
                    if (ClientId >= 0)
                        return;
                }
            }
            ClientId = Tikv.Driver.NewClient(databaseUrl);
        }

        public void Open()
        {
            // 不需要实现，和Sql,Mysql的一致，先保留。
        }

        public void Dispose()
        {
            if (this.Disposed)
                return;
            this.Disposed = true;
            UsingCount.AddAndGet(-1);

            // 没做事情或者事务成功时，保存到Pool中。其他情况都关闭连接。
            if ((null == Transaction || Transaction.CommitDone)
                && Pools.Count < MinPoolSize)
            {
                Pools.Add(ClientId);
                return;
            }
            // 不加入池子也要添加一个id到Pools中，用于唤醒Take并进行UsingCount的判断。
            // see 上面的构造函数。 
            Pools.Add(-1);
            try
            {
                Tikv.Driver.CloseClient(ClientId);
            }
            catch (Exception ex)
            {
                // log close error only.
                logger.Error(ex, "Tikv Connection Close");
            }
        }

        public TikvTransaction BeginTransaction()
        {
            if (null != Transaction)
                throw new Exception("Transaction Has Begin.");
            Transaction = new TikvTransaction(this);
            return Transaction;
        }
    }
}
