using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Collections.Concurrent;
using Zeze.Serialize;
using Zeze.Net;
using System.Threading.Tasks;
using System.Net;
using System.Threading;
using System.Text;

namespace Zeze.Services
{
    public class GlobalCacheManager
    {
        public const int StateInvalid = 0;
        public const int StateShare = 1;
        public const int StateModify = 2;

        public const int AcquireShareDeadLockFound = 1;
        public const int AcquireShareAlreadyIsModify = 2;
        public const int AcquireModifyDeadLockFound = 3;
        public const int AcquireErrorState = 4;
        public const int AcquireModifyAlreadyIsModify = 5;

        public const int ReduceErrorState = 11;
        public const int ReduceShareAlreadyIsInvalid = 12;
        public const int ReduceShareAlreadyIsShare = 13;
        public const int ReduceInvalidAlreadyIsInvalid = 14;

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public static GlobalCacheManager Instance { get; } = new GlobalCacheManager();
        public ServerService Server { get; private set; }
        private AsyncSocket serverSocket;

        private ConcurrentDictionary<GlobalTableKey, CacheState> global = new ConcurrentDictionary<GlobalTableKey, CacheState>();

        private GlobalCacheManager()
        { 
        }

        public void Start(IPAddress ipaddress, int port)
        {
            lock (this)
            {
                if (Server != null)
                    return;
                Server = new ServerService();
                Server.AddFactory(new Acquire().TypeId, () => new Acquire());
                Server.AddFactory(new Reduce().TypeId, () => new Reduce());
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
                serverSocket = null;
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
                    // 安全的从global中删除，没有并发问题。
                    cs.IsRemoved = true;
                    global.TryRemove(rpc.Argument.GlobalTableKey, out var _);
                }
                return 0;
            }
        }

        private int AcquireShare(Acquire rpc)
        {
            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;
            rpc.Result = rpc.Argument;
            while (true)
            {
                CacheState cs = global.GetOrAdd(rpc.Argument.GlobalTableKey, (tabkeKeyNotUsed) => new CacheState());
                lock (cs)
                {
                    if (cs.IsRemoved)
                        continue;

                    while (cs.AcquireStatePending != StateInvalid)
                    {
                        switch (cs.AcquireStatePending)
                        {
                            case StateShare:
                                if (cs.Modify == sender)
                                {
                                    logger.Debug("1 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = StateInvalid;
                                    rpc.SendResultCode(AcquireShareDeadLockFound);
                                    return 0;
                                }
                                break;
                            case StateModify:
                                if (cs.Modify == sender || cs.Share.Contains(sender))
                                {
                                    logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = StateInvalid;
                                    rpc.SendResultCode(AcquireShareDeadLockFound);
                                    return 0;
                                }
                                break;
                        }
                        logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);
                    }
                    cs.AcquireStatePending = StateShare;

                    if (cs.Modify != null)
                    {
                        if (cs.Modify == sender)
                        {
                            cs.AcquireStatePending = StateInvalid;
                            logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = StateModify;
                            rpc.SendResultCode(AcquireShareAlreadyIsModify);
                            return 0;
                        }

                        Task.Run(() =>
                        {
                            if (StateShare == cs.Modify.Reduce(rpc.Argument.GlobalTableKey, StateShare))
                                cs.Share.Add(cs.Modify); // 降级成功，有可能降到 Invalid，此时就不需要加入 Share 了。

                            lock (cs)
                            {
                                Monitor.PulseAll(cs);
                            }
                        });
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);

                        cs.Modify = null;
                        cs.Share.Add(sender);
                        cs.AcquireStatePending = StateInvalid;
                        Monitor.Pulse(cs);
                        logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.SendResult();
                        return 0;
                    }

                    cs.Share.Add(sender);
                    cs.AcquireStatePending = StateInvalid;
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.SendResult();
                    return 0;
                }
            }
        }

        private int AcquireModify(Acquire rpc)
        {
            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;
            rpc.Result = rpc.Argument;

            while (true)
            {
                CacheState cs = global.GetOrAdd(rpc.Argument.GlobalTableKey, (tabkeKeyNotUsed) => new CacheState());
                lock (cs)
                {
                    if (cs.IsRemoved)
                        continue;

                    while (cs.AcquireStatePending != StateInvalid)
                    {
                        switch (cs.AcquireStatePending)
                        {
                            case StateShare:
                                if (cs.Modify == sender)
                                {
                                    logger.Debug("1 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = StateInvalid;
                                    rpc.SendResultCode(AcquireModifyDeadLockFound);
                                    return 0;
                                }
                                break;
                            case StateModify:
                                if (cs.Modify == sender || cs.Share.Contains(sender))
                                {
                                    logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = StateInvalid;
                                    rpc.SendResultCode(AcquireModifyDeadLockFound);
                                    return 0;
                                }
                                break;
                        }
                        logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);
                    }
                    cs.AcquireStatePending = StateModify;

                    if (cs.Modify != null)
                    {
                        if (cs.Modify == sender)
                        {
                            logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.SendResultCode(AcquireModifyAlreadyIsModify);
                            cs.AcquireStatePending = StateInvalid;
                            return 0;
                        }

                        Task.Run(() =>
                        {
                            cs.Modify.Reduce(rpc.Argument.GlobalTableKey, StateInvalid);
                            lock (cs)
                            {
                                Monitor.PulseAll(cs);
                            }
                        });
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);

                        cs.Modify = sender;

                        cs.AcquireStatePending = StateInvalid;
                        Monitor.Pulse(cs);
                        logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.SendResult();
                        return 0;
                    }

                    List<Reduce> reduces = new List<Reduce>();
                    // 先把降级请求全部发送给出去。
                    foreach (CacheHolder c in cs.Share)
                    {
                        if (c == sender)
                            continue;
                        Reduce reduce = c.ReduceWaitLater(rpc.Argument.GlobalTableKey, StateInvalid);
                        if (null != reduce)
                            reduces.Add(reduce);
                    }
                    Task.Run(() =>
                    {
                        // 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，应该也会等待所有任务结束（包括错误）。
                        foreach (Reduce reduce in reduces)
                        {
                            try
                            {
                                reduce.Future.Task.Wait();
                            }
                            catch (Exception ex)
                            {
                                // 等待失败看作降级成功，对方可能已经不存在。
                                logger.Error(ex, "Reduce {0} {1} {2} {3}", sender, rpc.Argument.State, cs, reduce.Argument);
                            }
                        }
                        lock (cs)
                        {
                            Monitor.PulseAll(cs); // 需要唤醒等待任务结束的，但没法指定，只能全部唤醒。
                        }
                    });
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    Monitor.Wait(cs);

                    cs.Share.Clear();
                    cs.Modify = sender;
                    cs.AcquireStatePending = StateInvalid;
                    Monitor.Pulse(cs); // Pending 结束，唤醒一个进来就可以了。
                    logger.Debug("8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.SendResult();
                    return 0;
                }
            }
        }
    }

    public class CacheState
    {
        internal CacheHolder Modify { get; set; }
        internal int AcquireStatePending { get; set; } = GlobalCacheManager.StateInvalid;
        internal bool IsRemoved { get; set; }
        internal HashSet<CacheHolder> Share { get; } = new HashSet<CacheHolder>();
        public override string ToString()
        {
            StringBuilder sb = new StringBuilder();
            Helper.BuildString(sb, Share);
            return $"P{AcquireStatePending} M{Modify} S{sb}";
        }
    }

    public class CacheHolder
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        public long SessionId { get; }

        public CacheHolder(long sessionId)
        {
            SessionId = sessionId;
        }

        public override string ToString()
        {
            return "" + SessionId;
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
                logger.Error(ex, "Reduce Error {0} target={1}", state, SessionId);
            }
            return GlobalCacheManager.StateInvalid; // 访问失败，统统返回Invalid
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
                    reduce.SendForWait(peer, 10000);
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
