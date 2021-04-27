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
using Zeze.Transaction;

namespace Zeze.Services
{
    public sealed class GlobalCacheManager
    {
        public const int StateInvalid = 0;
        public const int StateShare = 1;
        public const int StateModify = 2;
        public const int StateRemoved = -1; // 从容器(Cache或Global)中删除后设置的状态，最后一个状态。
        public const int StateReduceRpcTimeout = -2; // 用来表示 reduce 超时失败。不是状态。
        public const int StateReduceException = -3; // 用来表示 reduce 异常失败。不是状态。
        public const int StateReduceNetError = -4;  // 用来表示 reduce 网络失败。不是状态。

        public const int AcquireShareDeadLockFound = 1;
        public const int AcquireShareAlreadyIsModify = 2;
        public const int AcquireModifyDeadLockFound = 3;
        public const int AcquireErrorState = 4;
        public const int AcquireModifyAlreadyIsModify = 5;
        public const int AcquireShareFaild = 6;
        public const int AcquireModifyFaild = 7;

        public const int ReduceErrorState = 11;
        public const int ReduceShareAlreadyIsInvalid = 12;
        public const int ReduceShareAlreadyIsShare = 13;
        public const int ReduceInvalidAlreadyIsInvalid = 14;

        public const int AcquireNotLogin = 20;

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public static GlobalCacheManager Instance { get; } = new GlobalCacheManager();
        public ServerService Server { get; private set; }
        private AsyncSocket ServerSocket;

        public const int DefaultConcurrencyLevel = 1024;
        public const int DefaultCapacity = 100000000; // 设置了这么大，开始使用后，大概会占用700M的内存，作为全局服务器，先这么大吧。

        private ConcurrentDictionary<GlobalTableKey, CacheState> global
            = new ConcurrentDictionary<GlobalTableKey, CacheState>(DefaultConcurrencyLevel, DefaultCapacity);

        /*
         * 会话。
         * key是 LogicServer.Id，现在的实现就是Zeze.Config.AutoKeyLocalId。在连接建立后收到的Login Or Relogin 中设置。
         * 每个会话记住分配给自己的GlobalTableKey，用来在正常退出的时候释放。
         * 每个会话还需要记录该会话的Socket.SessionId。在连接重新建立时更新。
         * 总是GetOrAdd，不删除。按现在的cache-sync设计，AutoKeyLocalId是及其有限的。不会一直增长。简化。
         */
        private ConcurrentDictionary<int, CacheHolder> sessions
            = new ConcurrentDictionary<int, CacheHolder>(DefaultConcurrencyLevel, 4096);

        private GlobalCacheManager()
        { 
        }

        public void Start(IPAddress ipaddress, int port, Config config = null)
        {
            lock (this)
            {
                if (Server != null)
                    return;
                if (null == config)
                    config = Config.Load();
                Server = new ServerService(config);
                Server.AddFactoryHandle(new Acquire().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new Acquire(),
                    Handle = ProcessAcquireRequest,
                });
                Server.AddFactoryHandle(new Reduce().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new Reduce(),
                });
                Server.AddFactoryHandle(new Login().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new Login(),
                    Handle = ProcessLogin,
                });
                Server.AddFactoryHandle(new ReLogin().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new ReLogin(),
                    Handle = ProcessReLogin,
                });
                Server.AddFactoryHandle(new NormalClose().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new NormalClose(),
                    Handle = ProcessNormalClose,
                });
                // 临时注册到这里，安全起见应该起一个新的Service，并且仅绑定到 localhost。
                Server.AddFactoryHandle(new Cleanup().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new Cleanup(),
                    Handle = ProcessCleanup,
                });

                ServerSocket = Server.NewServerSocket(ipaddress, port);
            }
        }

        public void Stop()
        {
            lock (this)
            {
                if (null == Server)
                    return;
                ServerSocket.Dispose();
                ServerSocket = null;
                Server.Close();
                Server = null;
            }
        }

        /// <summary>
        /// 报告错误的时候把相关信息（包括GlobalCacheManager和LogicServer等等）编码，手动Cleanup时，
        /// 解码并连接正确的服务器执行。降低手动风险。
        /// </summary>
        /// <param name="p"></param>
        /// <returns></returns>
        private int ProcessCleanup(Zeze.Net.Protocol p)
        {
            var rpc = p as Cleanup;

            // 安全性以后加强。
            if (false == rpc.Argument.SecureKey.Equals("Ok! verify secure."))
            {
                rpc.SendResultCode(-1);
                return 0;
            }

            var session = sessions.GetOrAdd(rpc.Argument.AutoKeyLocalId, (key) => new CacheHolder());
            if (session.GlobalCacheManagerHashIndex != rpc.Argument.GlobalCacheManagerHashIndex)
            {
                // 多点验证
                rpc.SendResultCode(-2);
                return 0;
            }

            if (this.Server.GetSocket(session.SessionId) != null)
            {
                // 连接存在，禁止cleanup。
                rpc.SendResultCode(-3);
                return 0;
            }

            // 还有更多的防止出错的手段吗？

            // XXX verify danger
            Zeze.Util.Scheduler.Instance.Schedule((ThisTask) =>
            {
                foreach (var gkey in session.Acquired.Keys)
                {
                    // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                    Release(session, gkey);
                }
                rpc.SendResultCode(0);
            }, 5 * 60 * 1000); // delay 5 mins

            return 0;
        }

        private int ProcessLogin(Zeze.Net.Protocol p)
        {
            var rpc = p as Login;
            var session = sessions.GetOrAdd(rpc.Argument.AutoKeyLocalId, (_) => new CacheHolder());
            if (false == session.TryBindSocket(p.Sender, rpc.Argument.GlobalCacheManagerHashIndex))
            {
                rpc.SendResultCode(-1);
                return 0;
            }
            // new login, 比如逻辑服务器重启。release old acquired.
            foreach (var gkey in session.Acquired.Keys)
            {
                // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                Release(session, gkey);
            }
            rpc.SendResultCode(0);
            return 0;
        }

        private int ProcessReLogin(Zeze.Net.Protocol p)
        {
            var rpc = p as ReLogin;
            var session = sessions.GetOrAdd(rpc.Argument.AutoKeyLocalId, (_) => new CacheHolder());
            if (false == session.TryBindSocket(p.Sender, rpc.Argument.GlobalCacheManagerHashIndex))
            {
                rpc.SendResultCode(-1);
                return 0;
            }
            rpc.SendResultCode(0);
            return 0;
        }
        
        private int ProcessNormalClose(Zeze.Net.Protocol p)
        {
            var rpc = p as NormalClose;
            var session = rpc.Sender.UserState as CacheHolder;
            if (null == session)
            {
                rpc.SendResultCode(AcquireNotLogin);
                return 0; // not login
            }
            if (false == session.TryUnBindSocket(p.Sender))
            {
                rpc.SendResultCode(-2);
                return 0;
            }
            foreach (var gkey in session.Acquired.Keys)
            {
                // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                Release(session, gkey);
            }
            rpc.SendResultCode(0);
            logger.Debug("After NormalClose global.Count={0}", global.Count);
            return 0;
        }

        private int ProcessAcquireRequest(Zeze.Net.Protocol p)
        {
            Acquire rpc = (Acquire)p;
            if (rpc.Sender.UserState == null)
            {
                rpc.SendResultCode(AcquireNotLogin);
                return 0;
            }
            switch (rpc.Argument.State)
            {
                case StateInvalid: // realease
                    Release(rpc.Sender.UserState as CacheHolder, rpc.Argument.GlobalTableKey);
                    rpc.Result = rpc.Argument;
                    rpc.SendResult();
                    return 0;

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

        private void Release(CacheHolder holder, GlobalTableKey gkey)
        {
            CacheState cs = global.GetOrAdd(gkey, (tabkeKeyNotUsed) => new CacheState());
            lock (cs)
            {
                if (cs.Modify == holder)
                    cs.Modify = null;
                cs.Share.Remove(holder); // always try remove

                if (cs.Modify == null && cs.Share.Count == 0 && cs.AcquireStatePending == StateInvalid)
                {
                    // 安全的从global中删除，没有并发问题。
                    cs.AcquireStatePending = StateRemoved;
                    global.TryRemove(gkey, out var _);
                }
                holder.Acquired.TryRemove(gkey, out var _);
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
                    if (cs.AcquireStatePending == StateRemoved)
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
                            // 已经是Modify又申请，可能是sender异常关闭，又重启连上。更新一下。应该是不需要的。
                            sender.Acquired[rpc.Argument.GlobalTableKey] = StateModify;
                            rpc.SendResultCode(AcquireShareAlreadyIsModify);
                            return 0;
                        }

                        int stateReduceResult = StateReduceException;
                        Zeze.Util.Task.Run(() =>
                        {
                            stateReduceResult = cs.Modify.Reduce(rpc.Argument.GlobalTableKey, StateShare);

                            lock (cs)
                            {
                                Monitor.PulseAll(cs);
                            }
                        }, "GlobalCacheManager.AcquireShare.Reduce");
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);

                        switch (stateReduceResult)
                        {
                            case StateShare:
                                cs.Modify.Acquired[rpc.Argument.GlobalTableKey] = StateShare;
                                cs.Share.Add(cs.Modify); // 降级成功，有可能降到 Invalid，此时就不需要加入 Share 了。
                                break;

                            default:
                                // 包含协议返回错误的值的情况。
                                // case StateReduceRpcTimeout:
                                // case StateReduceException:
                                // case StateReduceNetError:
                                cs.AcquireStatePending = StateInvalid;
                                Monitor.Pulse(cs);

                                logger.Error("XXX 8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = StateInvalid;
                                rpc.SendResultCode(AcquireShareFaild);
                                return 0;
                        }

                        cs.Modify = null;
                        sender.Acquired[rpc.Argument.GlobalTableKey] = StateShare;
                        cs.Share.Add(sender);
                        cs.AcquireStatePending = StateInvalid;
                        Monitor.Pulse(cs);
                        logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.SendResult();
                        return 0;
                    }

                    sender.Acquired[rpc.Argument.GlobalTableKey] = StateShare;
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
                    if (cs.AcquireStatePending == StateRemoved)
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
                            // 已经是Modify又申请，可能是sender异常关闭，又重启连上。更新一下。应该是不需要的。
                            sender.Acquired[rpc.Argument.GlobalTableKey] = StateModify;
                            rpc.SendResultCode(AcquireModifyAlreadyIsModify);
                            cs.AcquireStatePending = StateInvalid;
                            return 0;
                        }

                        int stateReduceResult = StateReduceException;
                        Zeze.Util.Task.Run(() =>
                        {
                            stateReduceResult = cs.Modify.Reduce(rpc.Argument.GlobalTableKey, StateInvalid);
                            lock (cs)
                            {
                                Monitor.PulseAll(cs);
                            }
                        }, "GlobalCacheManager.AcquireModify.Reduce");
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);

                        switch (stateReduceResult)
                        {
                            case StateInvalid:
                                cs.Modify.Acquired.TryRemove(rpc.Argument.GlobalTableKey, out var _);
                                break; // reduce success

                            default:
                                // case StateReduceRpcTimeout:
                                // case StateReduceException:
                                // case StateReduceNetError:
                                cs.AcquireStatePending = StateInvalid;
                                Monitor.Pulse(cs);

                                logger.Error("XXX 9 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = StateInvalid;
                                rpc.SendResultCode(AcquireModifyFaild);
                                return 0;
                        }

                        cs.Modify = sender;
                        sender.Acquired[rpc.Argument.GlobalTableKey] = StateModify;
                        cs.AcquireStatePending = StateInvalid;
                        Monitor.Pulse(cs);

                        logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.SendResult();
                        return 0;
                    }

                    List<Util.KV<CacheHolder, Reduce >> reducePending = new List<Util.KV<CacheHolder, Reduce>>();
                    HashSet<CacheHolder> reduceSuccessed = new HashSet<CacheHolder>();
                    bool senderIsShare = false;
                    // 先把降级请求全部发送给出去。
                    foreach (CacheHolder c in cs.Share)
                    {
                        if (c == sender)
                        {
                            senderIsShare = true;
                            reduceSuccessed.Add(sender);
                            continue;
                        }
                        Reduce reduce = c.ReduceWaitLater(rpc.Argument.GlobalTableKey, StateInvalid);
                        if (null != reduce)
                        {
                            reducePending.Add(Util.KV.Create(c, reduce));
                        }
                        else
                        {
                            // 网络错误不再认为成功。整个降级失败，要中断降级。
                            // 已经发出去的降级请求要等待并处理结果。后面处理。
                            break;
                        }
                    }

                    Zeze.Util.Task.Run(() =>
                    {
                        // 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，应该也会等待所有任务结束（包括错误）。
                        foreach (var reduce in reducePending)
                        {
                            try
                            {
                                reduce.Value.Future.Task.Wait();
                                if (reduce.Value.Result.State == StateInvalid)
                                {
                                    // 后面还有个成功的处理循环，但是那里可能包含sender，在这里更新吧。
                                    reduce.Key.Acquired.TryRemove(rpc.Argument.GlobalTableKey, out var _);
                                    reduceSuccessed.Add(reduce.Key);
                                }
                                else
                                {
                                    reduce.Key.SetError();
                                }
                            }
                            catch (Exception ex)
                            {
                                reduce.Key.SetError();
                                // 等待失败不再看作成功。这个以前已经处理了，但这个注释没有更新。
                                logger.Error(ex, "Reduce {0} {1} {2} {3}", sender, rpc.Argument.State, cs, reduce.Value.Argument);
                            }
                        }
                        lock (cs)
                        {
                            Monitor.PulseAll(cs); // 需要唤醒等待任务结束的，但没法指定，只能全部唤醒。
                        }
                    }, "GlobalCacheManager.AcquireModify.WaitReduce");
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    Monitor.Wait(cs);

                    // 移除成功的。
                    foreach (CacheHolder successed in reduceSuccessed)
                    {
                        cs.Share.Remove(successed);
                    }

                    // 如果前面降级发生中断(break)，这里就不会为0。
                    if (cs.Share.Count == 0)
                    {
                        cs.Modify = sender;
                        sender.Acquired[rpc.Argument.GlobalTableKey] = StateModify;
                        cs.AcquireStatePending = StateInvalid;
                        Monitor.Pulse(cs); // Pending 结束，唤醒一个进来就可以了。

                        logger.Debug("8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.SendResult();
                    }
                    else
                    {
                        // senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
                        if (senderIsShare)
                            cs.Share.Add(sender); // 失败了，要把原来是share的sender恢复。先这样吧。

                        cs.AcquireStatePending = StateInvalid;
                        Monitor.Pulse(cs); // Pending 结束，唤醒一个进来就可以了。

                        logger.Error("XXX 10 {0} {1} {2}", sender, rpc.Argument.State, cs);

                        rpc.Result.State = StateInvalid;
                        rpc.SendResultCode(AcquireModifyFaild);
                    }
                    // 很好，网络失败不再看成成功，发现除了加break，其他处理已经能包容这个改动，都不用动。
                    return 0;
                }
            }
        }

        public sealed class CacheState
        {
            internal CacheHolder Modify { get; set; }
            internal int AcquireStatePending { get; set; } = GlobalCacheManager.StateInvalid;
            internal HashSet<CacheHolder> Share { get; } = new HashSet<CacheHolder>();
            public override string ToString()
            {
                StringBuilder sb = new StringBuilder();
                ByteBuffer.BuildString(sb, Share);
                return $"P{AcquireStatePending} M{Modify} S{sb}";
            }
        }

        public sealed class CacheHolder
        {
            private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

            public long SessionId { get; private set; }
            public int GlobalCacheManagerHashIndex { get; private set; } // UnBind 的时候不会重置，会一直保留到下一次Bind。

            // 记住分配给该会话的GlobalTableKey。这里本来用Set即可，为了线程安全和并发，用ConcurrentDictionary，顺便记住分配的State。
            public ConcurrentDictionary<GlobalTableKey, int> Acquired { get; }
                = new ConcurrentDictionary<GlobalTableKey, int>(GlobalCacheManager.DefaultConcurrencyLevel, 1000000);

            public bool TryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex)
            {
                lock (this)
                {
                    if (newSocket.UserState != null)
                        return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。

                    var socket = GlobalCacheManager.Instance.Server.GetSocket(SessionId);
                    if (null == socket)
                    {
                        // old socket not exist or has lost.
                        SessionId = newSocket.SessionId;
                        newSocket.UserState = this;
                        GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
                        return true;
                    }
                    // 每个AutoKeyLocalId只允许一个实例，已经存在了以后，旧的实例上有状态，阻止新的实例登录成功。
                    return false;
                }
            }

            public bool TryUnBindSocket(AsyncSocket oldSocket)
            {
                lock (this)
                {
                    // 这里检查比较严格，但是这些检查应该都不会出现。

                    if (oldSocket.UserState != this)
                        return false; // not bind to this

                    var socket = GlobalCacheManager.Instance.Server.GetSocket(SessionId);
                    if (socket != oldSocket)
                        return false; // not same socket

                    SessionId = 0;
                    return true;
                }
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
                        // 如果rpc返回错误的值，外面能处理。
                        return reduce.Result.State;
                    }
                    return GlobalCacheManager.StateReduceNetError;
                }
                catch (RpcTimeoutException timeoutex)
                {
                    // 等待超时，应该报告错误。
                    logger.Error(timeoutex, "Reduce RpcTimeoutException {0} target={1}", state, SessionId);
                    return GlobalCacheManager.StateReduceRpcTimeout;
                }
                catch (Exception ex)
                {
                    logger.Error(ex, "Reduce Exception {0} target={1}", state, SessionId);
                    return GlobalCacheManager.StateReduceException;
                }
            }

            public const long ForbitPeriod = 10 * 1000; // 10 seconds
            private long LastErrorTime = 0;

            public void SetError()
            {
                lock (this)
                {
                    long now = global::Zeze.Util.Time.NowUnixMillis;
                    if (now - LastErrorTime > ForbitPeriod)
                        LastErrorTime = now;
                }
            }
            /// <summary>
            /// 返回null表示发生了网络错误，或者应用服务器已经关闭。
            /// </summary>
            /// <param name="gkey"></param>
            /// <param name="state"></param>
            /// <returns></returns>
            public Reduce ReduceWaitLater(GlobalTableKey gkey, int state)
            {
                try
                {
                    lock (this)
                    {
                        if (global::Zeze.Util.Time.NowUnixMillis - LastErrorTime < ForbitPeriod)
                            return null;
                    }
                    AsyncSocket peer = GlobalCacheManager.Instance.Server.GetSocket(SessionId);
                    if (null != peer)
                    {
                        Reduce reduce = new Reduce(gkey, state);
                        reduce.SendForWait(peer, 10000);
                        return reduce;
                    }
                }
                catch (Exception ex)
                {
                    // 这里的异常只应该是网络发送异常。
                    logger.Error(ex, "ReduceWaitLater Exception {0}", gkey);
                }
                SetError();
                return null;
            }
        }

        public sealed class Param : Zeze.Transaction.Bean
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

            protected override void InitChildrenRootInfo(Record.RootInfo root)
            {
                throw new NotImplementedException();
            }

            public override string ToString()
            {
                return GlobalTableKey.ToString() + ":" + State;
            }
        }
        public sealed class Acquire : Zeze.Net.Rpc<Param, Param>
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

        public sealed class Reduce : Zeze.Net.Rpc<Param, Param>
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

        public sealed class LoginParam : Zeze.Transaction.Bean
        {
            public int AutoKeyLocalId { get; set; }

            // GlobalCacheManager 本身没有编号。
            // 启用多个进程，使用 GlobalTableKey.GetHashCode() 分配负载后，报告错误需要这个来识别哪个进程。
            // 当然识别还可以根据 ServerService 绑定的ip和port。
            // 给每个实例加配置不容易维护。
            public int GlobalCacheManagerHashIndex { get; set; }

            public override void Decode(ByteBuffer bb)
            {
                AutoKeyLocalId = bb.ReadInt();
                GlobalCacheManagerHashIndex = bb.ReadInt();
            }

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteInt(AutoKeyLocalId);
                bb.WriteInt(GlobalCacheManagerHashIndex);
            }

            protected override void InitChildrenRootInfo(Record.RootInfo root)
            {
                throw new NotImplementedException();
            }
        }

        public sealed class Login : Zeze.Net.Rpc<LoginParam, Zeze.Transaction.EmptyBean>
        {
            public override int ModuleId => 0;
            public override int ProtocolId => 3;

            public Login()
            {
            }

            public Login(int id)
            {
                Argument.AutoKeyLocalId = id;
            }
        }

        public sealed class ReLogin : Zeze.Net.Rpc<LoginParam, Zeze.Transaction.EmptyBean>
        {
            public override int ModuleId => 0;
            public override int ProtocolId => 4;

            public ReLogin()
            {
            }

            public ReLogin(int id)
            {
                Argument.AutoKeyLocalId = id;
            }
        }

        public sealed class NormalClose : Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Zeze.Transaction.EmptyBean>
        {
            public override int ModuleId => 0;
            public override int ProtocolId => 5;
        }

        public sealed class AchillesHeel : Zeze.Transaction.Bean
        {
            public int AutoKeyLocalId { get; set; } // 必须的。

            public string SecureKey { get; set; } // 安全验证
            public int GlobalCacheManagerHashIndex { get; set; } // 安全验证

            public override void Decode(ByteBuffer bb)
            {
                AutoKeyLocalId = bb.ReadInt();
                SecureKey = bb.ReadString();
                GlobalCacheManagerHashIndex = bb.ReadInt();
            }

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteInt(AutoKeyLocalId);
                bb.WriteString(SecureKey);
                bb.WriteInt(GlobalCacheManagerHashIndex);
            }

            protected override void InitChildrenRootInfo(Record.RootInfo root)
            {
                throw new NotImplementedException();
            }
        }

        public sealed class Cleanup : Zeze.Net.Rpc<AchillesHeel, Zeze.Transaction.EmptyBean>
        {
            public override int ModuleId => 0;
            public override int ProtocolId => 6;
        }

        public sealed class ServerService : Zeze.Net.Service
        {
            public ServerService(Config config) : base("GlobalCacheManager", config)
            { 
            }

            public override void OnSocketAccept(AsyncSocket so)
            {
                // so.UserState = new CacheHolder(so.SessionId); // Login ReLogin 的时候初始化。
                base.OnSocketAccept(so);
            }
        }

        public sealed class GlobalTableKey : IComparable<GlobalTableKey>, Serializable
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

            public int CompareTo(GlobalTableKey other)
            {
                int c = this.TableName.CompareTo(other.TableName);
                if (c != 0)
                    return c;

                return ByteBuffer.Compare(Key, other.Key);
            }

            public override bool Equals(object obj)
            {
                if (this == obj)
                    return true;

                if (obj is GlobalTableKey another)
                    return TableName.Equals(another.TableName) && ByteBuffer.Equals(Key, another.Key);

                return false;
            }

            public override int GetHashCode()
            {
                const int prime = 31;
                int result = 17;
                result = prime * result + ByteBuffer.calc_hashnr(TableName);
                result = prime * result + ByteBuffer.calc_hashnr(Key, 0, Key.Length);
                return result;
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
}
