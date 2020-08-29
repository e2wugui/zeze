using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Reflection;
using System.Reflection.Metadata;
using System.Text;
using System.Collections.Concurrent;
using System.Windows.Markup;
using Zeze.Serialize;
using Zeze.Transaction;
using Org.BouncyCastle.Asn1.Pkcs;
using Zeze.Net;
using System.Threading.Tasks;
using System.Net;
using NLog;

namespace Zeze.Services
{
    public class GlobalCacheManager
    {
        public const int StateInvalid = 0;
        public const int StateModify = 1;
        public const int StateShare = 2;

        public const int AcquireShareAlreadyIsModify = 1;
        //public const int AcquireInvalid = 2;
        public const int AcquireErrorState = 3;
        public const int AcquireModifyAlreadyIsModify = 4;

        public const int ReduceErrorState = 5;
        public const int ReduceShareAlreadyIsInvalid = 6;
        public const int ReduceShareAlreadyIsShare = 7;
        public const int ReduceInvalidAlreadyIsInvalid = 8;

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public static GlobalCacheManager Instance { get; } = new GlobalCacheManager();
        public ServerService Server { get; private set; }
        private AsyncSocket serverSocket;

        private ConcurrentDictionary<GlobalTableKey, CacheState> global = new ConcurrentDictionary<GlobalTableKey, CacheState>();

        public void Start(IPAddress ipaddress, int port)
        {
            lock (this)
            {
                if (Server != null)
                    return;
                Server = new ServerService();
                Server.AddFactory(new Acquire().TypeId, () => new Acquire());
                Server.AddHandle(new Acquire().TypeRpcRequestId, Service.MakeHandle<Acquire>(this, GetType().GetMethod(nameof(ProcessAcquireRequest))));
                serverSocket = Server.NewServerSocket(ipaddress, port);
            }
        }

        public void Stop()
        {
            lock (this)
            {
                if (null == Server)
                    return;
                serverSocket.Dispose();
                Server.Close();
                Server = null;
            }
        }

        public int ProcessAcquireRequest(Acquire rpc)
        {
            switch (rpc.Argument.State)
            {
                case StateInvalid: // realease
                    return Release(rpc);

                case StateShare:
                    return AcquireShare(rpc);

                case StateModify:
                    return AcquireModify(rpc);

                default:
                    rpc.Result = rpc.Argument;
                    rpc.SendResultCode(AcquireErrorState);
                    return 0;
            }
        }
        private int Release(Acquire rpc)
        {
            CacheHolder holder = (CacheHolder)rpc.Sender.UserState;
            CacheState cs = global.GetOrAdd(rpc.Argument.GlobalTableKey, (tabkeKeyNotUsed) => new CacheState());
            lock (cs)
            {
                if (cs.Modify == holder)
                    cs.Modify = null;
                cs.Share.Remove(holder); // always try remove

                rpc.Result = rpc.Argument;
                rpc.SendResult();
                if (cs.Modify == null && cs.Share.Count == 0)
                {
                    // TODO 怎么安全的从global中删除，没有并发问题。
                }
                return 0;
            }
        }

        private int AcquireShare(Acquire rpc)
        {
            CacheHolder holder = (CacheHolder)rpc.Sender.UserState;
            CacheState cs = global.GetOrAdd(rpc.Argument.GlobalTableKey, (tabkeKeyNotUsed) => new CacheState());
            lock (cs)
            {
                if (cs.Modify != null)
                {
                    if (cs.Modify == holder)
                    {
                        rpc.Result = rpc.Argument;
                        rpc.Result.State = StateModify;
                        rpc.SendResultCode(AcquireShareAlreadyIsModify);
                        return 0;
                    }

                    if (StateShare == cs.Modify.Reduce(rpc.Argument.GlobalTableKey, StateShare))
                        cs.Share.Add(cs.Modify); // 降级成功，有可能降到 Invalid，此时就不需要加入 Share 了。

                    cs.Modify = null;
                    cs.Share.Add(holder);

                    rpc.Result = rpc.Argument;
                    rpc.SendResult();
                    return 0;
                }

                cs.Share.Add(holder);

                rpc.Result = rpc.Argument;
                rpc.SendResult();
                return 0;
            }
        }

        private int AcquireModify(Acquire rpc)
        {
            CacheHolder holder = (CacheHolder)rpc.Sender.UserState;
            CacheState cs = global.GetOrAdd(rpc.Argument.GlobalTableKey, (tabkeKeyNotUsed) => new CacheState());
            lock (cs)
            {
                // TODO 检查死锁。
                if (cs.Modify != null)
                {
                    if (cs.Modify == holder)
                    {
                        logger.Warn("AcquireModifyAlreadyIsModify");

                        rpc.Result = rpc.Argument;
                        rpc.SendResultCode(AcquireModifyAlreadyIsModify);
                        return 0;
                    }

                    cs.Modify.Reduce(rpc.Argument.GlobalTableKey, StateInvalid);
                    cs.Modify = holder;

                    rpc.Result = rpc.Argument;
                    rpc.SendResult();
                    return 0;
                }

                List<Reduce> reduces = new List<Reduce>();
                // 先把降级请求全部发送给出去。
                foreach (CacheHolder c in cs.Share)
                {
                    if (c == holder)
                        continue;
                    Reduce reduce = c.ReduceWaitLater(rpc.Argument.GlobalTableKey, StateInvalid);
                    if (null != reduce)
                        reduces.Add(reduce);
                }
                // 一个个等待是否成功。WaitAll 碰到错误很难处理。
                foreach (Reduce reduce in reduces)
                {
                    try
                    {
                        reduce.Future.Task.Wait();
                    }
                    catch (Exception ex)
                    {
                        // 等待失败看作降级成功，对方可能已经不存在。
                        logger.Error(ex, "AcquireModify Reduce {0}", rpc.Argument.GlobalTableKey);
                    }
                }

                cs.Share.Clear();
                cs.Modify = holder;

                rpc.Result = rpc.Argument;
                rpc.SendResult();
                return 0;
            }
        }
    }

    public class CacheState
    {
        internal CacheHolder Modify { get; set; }
        internal HashSet<CacheHolder> Share { get; } = new HashSet<CacheHolder>();
    }

    public class CacheHolder
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public long SessionId { get; }

        public CacheHolder(long sessionId)
        {
            SessionId = sessionId;
        }

        public int Reduce(GlobalTableKey gkey, int state)
        {
            try
            {
                Reduce reduce = ReduceWaitLater(gkey, state);
                if (null != reduce)
                {
                    reduce.Future.Task.Wait();
                    return reduce.Result.State;
                }
            }
            catch (Exception ex)
            {
                logger.Error(ex, "Reduce Error {0}", gkey);
            }
            return state;
        }

        public Reduce ReduceWaitLater(GlobalTableKey gkey, int state)
        {
            try
            {
                AsyncSocket peer = GlobalCacheManager.Instance.Server.GetSocket(SessionId);
                // 逻辑服务器网络连接关闭，表示自动释放所有锁。所有Reduce都看作成功。
                if (null != peer)
                {
                    Reduce reduce = new Reduce(gkey, state);
                    reduce.SendForWait(peer);
                    return reduce;
                }
            }
            catch (Exception ex)
            {
                logger.Error(ex, "ReduceWaitLater Error {0}", gkey);
            }
            return null;
        }
    }

    public class Param : Zeze.Transaction.Bean
    {
        public GlobalTableKey GlobalTableKey { get; set; } // 没有初始化，使用时注意
        public int State { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            if (null == GlobalTableKey)
               GlobalTableKey = new GlobalTableKey();
            GlobalTableKey.Decode(bb);
            State = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
            GlobalTableKey.Encode(bb);
            bb.WriteInt(State);
        }

        protected override void InitChildrenTableKey(Transaction.TableKey root)
        {
            throw new NotImplementedException();
        }

        public override string ToString()
        {
            return GlobalTableKey.ToString() + ":" + State;
        }
    }
    public class Acquire : Zeze.Net.Rpc<Param, Param>
    {
        public override int ModuleId => 0;
        public override int ProtocolId => 1;

        public Acquire()
        {
        }

        public Acquire(GlobalTableKey gkey, int state)
        {
            Argument.GlobalTableKey = gkey;
            Argument.State = state;
        }
    }

    public class Reduce : Zeze.Net.Rpc<Param, Param>
    {
        public override int ModuleId => 0;
        public override int ProtocolId => 2;

        public Reduce()
        { 
        }

        public Reduce(GlobalTableKey gkey, int state)
        {
            Argument.GlobalTableKey = gkey;
            Argument.State = state;
        }
    }

    public class ServerService : Zeze.Net.Service
    {
        public override void DispatchProtocol(Protocol p)
        {
            if (Handles.TryGetValue(p.TypeId, out var handle))
            {
                Task.Run(() => handle(p));
            }
            else
            {
                throw new Exception("Protocol Handle Not Found. " + p);
            }
        }

        public override void OnSocketAccept(AsyncSocket so)
        {
            so.UserState = new CacheHolder(so.SessionId);
            base.OnSocketAccept(so);
        }
    }

    public class GlobalTableKey : IComparable<GlobalTableKey>, Serializable
    {
        public string TableName { get; private set; }
        public byte[] Key { get; private set; }

        public GlobalTableKey()
        { 
        }

        public GlobalTableKey(string tableName, ByteBuffer key) : this(tableName, key.Copy())
        {
        }

        public GlobalTableKey(string tableName, byte[] key)
        {
            TableName = tableName;
            Key = key;
        }

        public int CompareTo([AllowNull] GlobalTableKey other)
        {
            int c = this.TableName.CompareTo(other.TableName);
            if (c != 0)
                return c;

            return Helper.Compare(Key, other.Key);
        }

        public override bool Equals(object obj)
        {
            if (this == obj)
                return true;

            if (obj is GlobalTableKey another)
                return TableName.Equals(another.TableName) && Helper.Equals(Key, another.Key);

            return false;
        }

        public override int GetHashCode()
        {
            return TableName.GetHashCode() + Helper.GetHashCode(Key);
        }

        public override string ToString()
        {
            return $"({TableName},{BitConverter.ToString(Key)})";
        }

        public void Decode(ByteBuffer bb)
        {
            TableName = bb.ReadString();
            Key = bb.ReadBytes();
        }

        public void Encode(ByteBuffer bb)
        {
            bb.WriteString(TableName);
            bb.WriteBytes(Key);
        }
    }
}
