using DotNext.Threading;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Net;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Xml;
using Zeze.Net;
using Zeze.Serialize;
using Zeze.Services.GlobalCacheManager;
using Zeze.Transaction;
using Zeze.Util;

namespace Zeze.Services
{
    public sealed class GlobalCacheManagerServer
    {
        public const int StateInvalid = 0;
        public const int StateShare = 1;
        public const int StateModify = 2;
        public const int StateRemoving = 3;

        public const int StateRemoved = 10; // 从容器(Cache或Global)中删除后设置的状态，最后一个状态。
        public const int StateReduceRpcTimeout = 11; // 用来表示 reduce 超时失败。不是状态。
        public const int StateReduceException = 12; // 用来表示 reduce 异常失败。不是状态。
        public const int StateReduceNetError = 13;  // 用来表示 reduce 网络失败。不是状态。
        public const int StateReduceDuplicate = 14; // 用来表示重复的 reduce。错误报告，不是状态。
        public const int StateReduceSessionNotFound = 15;
        public const int StateReduceErrorFreshAcquire = 16; // 错误码，too many try 处理机制

        public const int AcquireShareDeadLockFound = 21;
        public const int AcquireShareAlreadyIsModify = 22;
        public const int AcquireModifyDeadLockFound = 23;
        public const int AcquireErrorState = 24;
        public const int AcquireModifyAlreadyIsModify = 25;
        public const int AcquireShareFailed = 26;
        public const int AcquireModifyFailed = 27;
        public const int AcquireException = 28;
        public const int AcquireInvalidFailed = 29;
        public const int AcquireNotLogin = 30;
        public const int AcquireFreshSource = 31;

        public const int ReduceErrorState = 41;
        public const int ReduceShareAlreadyIsInvalid = 42;
        public const int ReduceShareAlreadyIsShare = 43;
        public const int ReduceInvalidAlreadyIsInvalid = 44;

        public const int CleanupErrorSecureKey = 60;
        public const int CleanupErrorGlobalCacheManagerHashIndex = 61;
        public const int CleanupErrorHasConnection = 62;

        public const int ReLoginBindSocketFail = 80;

        public const int NormalCloseUnbindFail = 100;

        public const int LoginBindSocketFail = 120;

        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public static GlobalCacheManagerServer Instance { get; } = new GlobalCacheManagerServer();
        public ServerService Server { get; private set; }
        public AsyncSocket ServerSocket { get; private set; }
        private ConcurrentDictionary<Binary, CacheState> global;
        private readonly Util.AtomicLong SerialIdGenerator = new();

        /*
         * 会话。
         * key是 LogicServer.Id，现在的实现就是Zeze.Config.ServerId。
         * 在连接建立后收到的Login Or ReLogin 中设置。
         * 每个会话记住分配给自己的GlobalTableKey，用来在正常退出的时候释放。
         * 每个会话还需要记录该会话的Socket.SessionId。在连接重新建立时更新。
         * 总是GetOrAdd，不删除。按现在的cache-sync设计，
         * ServerId 是及其有限的。不会一直增长。
         * 简化实现。
         */
        private ConcurrentDictionary<int, CacheHolder> Sessions;

        private GlobalCacheManagerServer()
        { 
        }

        public class GCMConfig : Config.ICustomize
        {
            public string Name => "GlobalCacheManager";

            public int ConcurrencyLevel { get; set; } = 1024;
            // 设置了这么大，开始使用后，大概会占用700M的内存，作为全局服务器，先这么大吧。
            // 尽量不重新调整ConcurrentDictionary。
            public int InitialCapacity { get; set; } = 10000000;

            public int MaxNetPing = 1500;
            public int ServerProcessTime = 1000;
            public int ServerReleaseTimeout = 10 * 1000;

            public void Parse(XmlElement self)
            {
                string attr;

                attr = self.GetAttribute("ConcurrencyLevel");
                if (attr.Length > 0)
                    ConcurrencyLevel = int.Parse(attr);
                if (ConcurrencyLevel < Environment.ProcessorCount)
                    ConcurrencyLevel = Environment.ProcessorCount;

                attr = self.GetAttribute("InitialCapacity");
                if (attr.Length > 0)
                    InitialCapacity = int.Parse(attr);
                if (InitialCapacity < 31)
                    InitialCapacity = 31;

                attr = self.GetAttribute("MaxNetPing");
                if (attr.Length > 0)
                    MaxNetPing = int.Parse(attr);
                attr = self.GetAttribute("ServerProcessTime");
                if (attr.Length > 0)
                    ServerProcessTime = int.Parse(attr);
                attr = self.GetAttribute("ServerReleaseTimeout");
                if (attr.Length > 0)
                    ServerReleaseTimeout = int.Parse(attr);
            }
        }

        public GCMConfig GcmConfig { get; } = new GCMConfig();
        private GlobalCacheManagerPerf Perf;
        //private Util.ObjectPool<Acquire> AcquirePool = new();
        //private Util.ObjectPool<Reduce> ReducePool = new();

        public void Start(IPAddress ipaddress, int port, Config config = null)
        {
            lock (this)
            {
                if (Server != null)
                    return;

                if (null == config)
                    config = Zeze.Config.Load();

                config.ParseCustomize(GcmConfig);

                // TODO 根据配置是否启用性能统计。
                Perf = new(SerialIdGenerator);

                Sessions = new(GcmConfig.ConcurrencyLevel, 4096);
                global = new(GcmConfig.ConcurrencyLevel, GcmConfig.InitialCapacity);

                Server = new ServerService(config);

                Server.AddFactoryHandle(
                    new Acquire().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new Acquire(), //AcquirePool.Create,
                        Handle = ProcessAcquireRequest,
                    });

                Server.AddFactoryHandle(
                    new Reduce().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new Reduce(), // ReducePool.Create,
                    });

                Server.AddFactoryHandle(
                    new Login().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new Login(),
                        Handle = ProcessLogin,
                    });

                Server.AddFactoryHandle(
                    new ReLogin().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new ReLogin(),
                        Handle = ProcessReLogin,
                    });

                Server.AddFactoryHandle(
                    new NormalClose().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new NormalClose(),
                        Handle = ProcessNormalClose,
                    });

                // 临时注册到这里，安全起见应该起一个新的Service，并且仅绑定到 localhost。
                Server.AddFactoryHandle(
                    new Cleanup().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new Cleanup(),
                        Handle = ProcessCleanup,
                    });

                Server.AddFactoryHandle(
                    new KeepAlive().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new KeepAlive(),
                        Handle = ProcessKeepAlive
                    });
                ServerSocket = Server.NewServerSocket(ipaddress, port, null);


                // Global的守护不需要独立线程。当出现异常问题不能工作时，没有释放锁是不会造成致命问题的。
                AchillesHeelConfig = new AchillesHeelConfig(GcmConfig.MaxNetPing, GcmConfig.ServerProcessTime, GcmConfig.ServerReleaseTimeout);
                Zeze.Util.Scheduler.Schedule(AchillesHeelDaemon, 5000, 5000);
            }
        }

        private AchillesHeelConfig AchillesHeelConfig;

        private async Task AchillesHeelDaemon(SchedulerTask ThisTask)
        {
            var now = Util.Time.NowUnixMillis;

            foreach (var session in Sessions.Values)
            { 
                if (now - session.GetActiveTime() > AchillesHeelConfig.GlobalDaemonTimeout && !session.DebugMode)
                {
                    using (await session.Mutex.AcquireAsync(CancellationToken.None))
                    {
                        session.Kick();
                        if (session.Acquired.Count > 0)
                        {
                            var releaseCount = 0L;
                            foreach (var e in session.Acquired)
                            {
                                // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                                await ReleaseAsync(session, e.Key, false);
                                releaseCount++;
                            }
                            session.SetActiveTime(Util.Time.NowUnixMillis);
                            if (releaseCount > 0)
                                logger.Info($"AchillesHeelDaemon.Release session={session} count={releaseCount}");
                        }
                    }
                }
            }
        }

        private Task<long> ProcessKeepAlive(Protocol p)
        {
            var session = (CacheHolder)p.Sender?.UserState;
            if (null == session)
            {
                p.SendResultCode(AcquireNotLogin);
                return Task.FromResult(0L);
            }
            session.SetActiveTime(Util.Time.NowUnixMillis);
            p.SendResultCode(0);
            return Task.FromResult(0L);
        }

        public void Stop()
        {
            lock (this)
            {
                if (null == Server)
                    return;
                ServerSocket.Dispose();
                ServerSocket = null;
                Server.Stop();
                Server = null;
            }
        }

        /// <summary>
        /// 报告错误的时候带上相关信息（包括GlobalCacheManager和LogicServer等等）
        /// 手动Cleanup时，连接正确的服务器执行。
        /// </summary>
        /// <param name="p"></param>
        /// <returns></returns>
        private async Task<long> ProcessCleanup(Protocol p)
        {
            if (AchillesHeelConfig != null)
                return 0;

            var rpc = p as Cleanup;

            // 安全性以后加强。
            if (false == rpc.Argument.SecureKey.Equals("Ok! verify secure."))
            {
                rpc.SendResultCode(CleanupErrorSecureKey);
                return 0;
            }

            var session = Sessions.GetOrAdd(rpc.Argument.ServerId, (key) => new CacheHolder(GcmConfig));
            if (session.GlobalCacheManagerHashIndex != rpc.Argument.GlobalCacheManagerHashIndex)
            {
                // 多点验证
                rpc.SendResultCode(CleanupErrorGlobalCacheManagerHashIndex);
                return 0;
            }

            if (this.Server.GetSocket(session.SessionId) != null)
            {
                // 连接存在，禁止cleanup。
                rpc.SendResultCode(CleanupErrorHasConnection);
                return 0;
            }

            // 还有更多的防止出错的手段吗？
            await Task.Delay(5 * 60 * 1000); // delay 5 mins

            foreach (var e in session.Acquired)
            {
                // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                await ReleaseAsync(session, e.Key, false);
            }
            rpc.SendResultCode(0);
            return 0;
        }

        private async Task<long> ProcessLogin(Protocol p)
        {
            var rpc = p as Login;
            var session = Sessions.GetOrAdd(rpc.Argument.ServerId, (_) => new CacheHolder(GcmConfig));

            if (false == await session.TryBindSocket(p.Sender, rpc.Argument.GlobalCacheManagerHashIndex, true))
            {
                rpc.SendResultCode(LoginBindSocketFail);
                return 0;
            }
            session.SetActiveTime(Util.Time.NowUnixMillis);
            session.DebugMode = rpc.Argument.DebugMode;
            // 只会有一个会话成功绑定并继续到达这里。
            // new login, 比如逻辑服务器重启。release old acquired.
            foreach (var e in session.Acquired)
            {
                // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                await ReleaseAsync(session, e.Key, false);
            }
            rpc.Result.MaxNetPing = GcmConfig.MaxNetPing;
            rpc.Result.ServerProcessTime = GcmConfig.ServerProcessTime;
            rpc.Result.ServerReleaseTimeout = GcmConfig.ServerReleaseTimeout;
            rpc.SendResultCode(0);
            return 0;
        }

        private async Task<long> ProcessReLogin(Protocol p)
        {
            var rpc = p as ReLogin;
            var session = Sessions.GetOrAdd(rpc.Argument.ServerId, (_) => new CacheHolder(GcmConfig));
            if (false == await session.TryBindSocket(p.Sender, rpc.Argument.GlobalCacheManagerHashIndex, false))
            {
                rpc.SendResultCode(ReLoginBindSocketFail);
                return 0;
            }
            session.SetActiveTime(Util.Time.NowUnixMillis);
            session.DebugMode = rpc.Argument.DebugMode;
            rpc.SendResultCode(0);
            return 0;
        }
        
        private async Task<long> ProcessNormalClose(Protocol p)
        {
            var rpc = p as NormalClose;
            if (rpc.Sender.UserState is not CacheHolder session)
            {
                rpc.SendResultCode(AcquireNotLogin);
                return 0; // not login
            }

            if (false == await session.TryUnBindSocket(p.Sender))
            {
                rpc.SendResultCode(NormalCloseUnbindFail);
                return 0;
            }
            foreach (var e in session.Acquired)
            {
                // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                await ReleaseAsync(session, e.Key, false);
            }
            rpc.SendResultCode(0);
            logger.Debug("After NormalClose global.Count={0}", global.Count);
            return 0;
        }

        private async Task<long> ProcessAcquireRequest(Protocol p)
        {
            Acquire rpc = (Acquire)p;
            Perf?.OnAcquireBegin(rpc);

            rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
            rpc.Result.State = rpc.Argument.State; // default success

            if (rpc.Sender.UserState == null)
            {
                rpc.Result.State = StateInvalid;
                rpc.SendResultCode(AcquireNotLogin);
                Perf?.OnAcquireEnd(rpc);
                //AcquirePool.Reclaim(rpc);
                return 0;
            }
            try
            {
                var session = rpc.Sender.UserState as CacheHolder;
                session.SetActiveTime(Util.Time.NowUnixMillis);
                switch (rpc.Argument.State)
                {
                    case StateInvalid: // realease
                        rpc.Result.State = await ReleaseAsync(session, rpc.Argument.GlobalKey, true);
                        rpc.SendResultCode(0);
                        return 0;

                    case StateShare:
                        return await AcquireShareAsync(rpc);

                    case StateModify:
                        return await AcquireModifyAsync(rpc);

                    default:
                        rpc.Result.State = StateInvalid;
                        rpc.SendResultCode(AcquireErrorState);
                        return 0;
                }
            }
            catch (Exception e)
            {
                logger.Error(e);
                rpc.Result.State = StateInvalid;
                rpc.SendResultCode(AcquireException);
                return 0;
            }
            finally
            {
                Perf?.OnAcquireEnd(rpc);
                //AcquirePool.Reclaim(rpc);
            }
        }

        private async Task<int> ReleaseAsync(CacheHolder sender, Binary _gkey, bool noWait)
        {
            while (true)
            {
                CacheState cs = global.GetOrAdd(_gkey, (key) => new CacheState() { GlobalKey = key });
                var gkey = cs.GlobalKey;
                using var lockcs = await cs.Monitor.EnterAsync();

                if (cs.AcquireStatePending == StateRemoved)
                    continue; // 这个是不可能的，因为有Release请求进来意味着肯定有拥有者(share or modify)，此时不可能进入StateRemoved。

                while (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved)
                {
                    switch (cs.AcquireStatePending)
                    {
                        case StateShare:
                        case StateModify:
                            logger.Debug("Release 0 {} {} {}", sender, gkey, cs);
                            if (noWait)
                                return cs.GetSenderCacheState(sender);
                            break;
                        case StateRemoving:
                            // release 不会导致死锁，等待即可。
                            break;
                    }
                    await cs.Monitor.WaitAsync();
                }
                if (cs.AcquireStatePending == StateRemoved)
                {
                    continue;
                }
                cs.AcquireStatePending = StateRemoving;

                if (cs.Modify == sender)
                    cs.Modify = null;
                cs.Share.Remove(sender); // always try remove
                sender.Acquired.TryRemove(gkey, out _);

                if (cs.Modify == null && cs.Share.Count == 0)
                {
                    // 安全的从global中删除，没有并发问题。
                    cs.AcquireStatePending = StateRemoved;
                    global.TryRemove(gkey, out var _);
                }
                else
                {
                    cs.AcquireStatePending = StateInvalid;
                }
                cs.Monitor.PulseAll();
                return StateInvalid;
            }
        }

        private async Task<int> AcquireShareAsync(Acquire rpc)
        {
            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;

            while (true)
            {
                CacheState cs = global.GetOrAdd(rpc.Argument.GlobalKey, (key) => new CacheState() { GlobalKey = key });
                var gkey = cs.GlobalKey;
                using var lockcs = await cs.Monitor.EnterAsync();

                if (cs.AcquireStatePending == StateRemoved)
                    continue;

                if (cs.Modify != null && cs.Share.Count > 0)
                    throw new Exception("CacheState state error");

                while (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved)
                {
                    switch (cs.AcquireStatePending)
                    {
                        case StateShare:
                            if (cs.Modify == null)
                                throw new Exception("CacheState state error");

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
                        case StateRemoving:
                            break;
                    }
                    logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    await cs.Monitor.WaitAsync();
                    if (cs.Modify != null && cs.Share.Count > 0)
                        throw new Exception("CacheState state error");
                }
                if (cs.AcquireStatePending == StateRemoved)
                    continue; // concurrent release.

                cs.AcquireStatePending = StateShare;
                SerialIdGenerator.IncrementAndGet();

                if (cs.Modify != null)
                {
                    if (cs.Modify == sender)
                    {
                        cs.AcquireStatePending = StateInvalid;
                        logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.Result.State = StateModify;
                        // 已经是Modify又申请，可能是sender异常关闭，
                        // 又重启连上。更新一下。应该是不需要的。
                        sender.Acquired[gkey] = StateModify;
                        rpc.SendResultCode(AcquireShareAlreadyIsModify);
                        return 0;
                    }

                    int reduceResultState = StateReduceNetError; // 默认网络错误。
                    if (cs.Modify.Reduce(gkey, StateInvalid, rpc.ResultCode,
                        async (p) =>
                        {
                            var r = p as Reduce;
                            Perf?.OnReduceEnd(r);
                            reduceResultState = r.IsTimeout ? StateReduceRpcTimeout : r.Result.State;
                            using var lockcs = await cs.Monitor.EnterAsync();
                            cs.Monitor.PulseAll();
                            //ReducePool.Reclaim(r);
                            return 0;
                        }))
                    {
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        await cs.Monitor.WaitAsync();
                    }

                    switch (reduceResultState)
                    {
                        case StateShare:
                            cs.Modify.Acquired[gkey] = StateShare;
                            cs.Share.Add(cs.Modify); // 降级成功。
                            break;

                        case StateInvalid:
                            // 降到了 Invalid，此时就不需要加入 Share 了。
                            cs.Modify.Acquired.TryRemove(gkey, out _);
                            break;

                        case StateReduceErrorFreshAcquire:
                            cs.AcquireStatePending = StateInvalid;

                            logger.Error("XXX fresh {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = StateInvalid;
                            cs.Monitor.PulseAll();
                            rpc.SendResultCode(StateReduceErrorFreshAcquire);
                            return 0;

                        default:
                            // 包含协议返回错误的值的情况。
                            // case StateReduceRpcTimeout:
                            // case StateReduceException:
                            // case StateReduceNetError:
                            cs.AcquireStatePending = StateInvalid;
                            cs.Monitor.PulseAll();

                            logger.Error("XXX 8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = StateInvalid;
                            rpc.SendResultCode(AcquireShareFailed);
                            return 0;
                    }

                    cs.Modify = null;
                    sender.Acquired[gkey] = StateShare;
                    cs.Share.Add(sender);
                    cs.AcquireStatePending = StateInvalid;
                    cs.Monitor.PulseAll();
                    logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.SendResultCode(0);
                    return 0;
                }

                sender.Acquired[gkey] = StateShare;
                cs.Share.Add(sender);
                cs.AcquireStatePending = StateInvalid;
                cs.Monitor.PulseAll();
                logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                rpc.SendResultCode(0);

                return 0;
            }
        }

        private async Task<int> AcquireModifyAsync(Acquire rpc)
        {
            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;
            while (true)
            {
                CacheState cs = global.GetOrAdd(rpc.Argument.GlobalKey, (key) => new CacheState() { GlobalKey = key });
                var gkey = cs.GlobalKey;
                using var lockcs = await cs.Monitor.EnterAsync();

                if (cs.AcquireStatePending == StateRemoved)
                    continue;

                if (cs.Modify != null && cs.Share.Count > 0)
                    throw new Exception("CacheState state error");

                while (cs.AcquireStatePending != StateInvalid && cs.AcquireStatePending != StateRemoved)
                {
                    switch (cs.AcquireStatePending)
                    {
                        case StateShare:
                            if (cs.Modify == null)
                            {
                                logger.Error("cs state must be modify");
                                throw new Exception("CacheState state error");
                            }
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
                        case StateRemoving:
                            break;
                    }
                    logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    await cs.Monitor.WaitAsync();

                    if (cs.Modify != null && cs.Share.Count > 0)
                        throw new Exception("CacheState state error");
                }
                if (cs.AcquireStatePending == StateRemoved)
                    continue; // concurrent release

                cs.AcquireStatePending = StateModify;
                SerialIdGenerator.IncrementAndGet();

                if (cs.Modify != null)
                {
                    if (cs.Modify == sender)
                    {
                        logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        // 已经是Modify又申请，可能是sender异常关闭，又重启连上。
                        // 更新一下。应该是不需要的。
                        sender.Acquired[gkey] = StateModify;
                        rpc.SendResultCode(AcquireModifyAlreadyIsModify);
                        cs.AcquireStatePending = StateInvalid;
                        cs.Monitor.PulseAll();
                        return 0;
                    }

                    int reduceResultState = StateReduceNetError; // 默认网络错误。
                    if (cs.Modify.Reduce(gkey, StateInvalid, rpc.ResultCode,
                        async (p) =>
                        {
                            var r = p as Reduce;
                            Perf?.OnReduceEnd(r);
                            reduceResultState = r.IsTimeout ? StateReduceRpcTimeout : r.Result.State;
                            using var lockcs = await cs.Monitor.EnterAsync();
                            cs.Monitor.PulseAll();
                            //ReducePool.Reclaim(r);
                            return 0;
                        }))
                    {
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        await cs.Monitor.WaitAsync();
                    }

                    switch (reduceResultState)
                    {
                        case StateInvalid:
                            cs.Modify.Acquired.TryRemove(gkey, out _);
                            break; // reduce success

                        case StateReduceErrorFreshAcquire:
                            cs.AcquireStatePending = StateInvalid;

                            logger.Error("XXX fresh {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = StateInvalid;
                            cs.Monitor.PulseAll();
                            rpc.SendResultCode(StateReduceErrorFreshAcquire);
                            return 0;

                        default:
                            // case StateReduceRpcTimeout:
                            // case StateReduceException:
                            // case StateReduceNetError:
                            cs.AcquireStatePending = StateInvalid;
                            cs.Monitor.PulseAll();

                            logger.Error("XXX 9 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = StateInvalid;
                            rpc.SendResultCode(AcquireModifyFailed);
                            return 0;
                    }

                    cs.Modify = sender;
                    cs.Share.Remove(sender);
                    sender.Acquired[gkey] = StateModify;
                    cs.AcquireStatePending = StateInvalid;
                    cs.Monitor.PulseAll();

                    logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.SendResultCode(0);
                    return 0;
                }

                List<Util.KV<CacheHolder, Reduce>> reducePending = new();
                HashSet<CacheHolder> reduceSucceed = new();
                bool senderIsShare = false;
                // 先把降级请求全部发送给出去。
                foreach (CacheHolder c in cs.Share)
                {
                    if (c == sender)
                    {
                        // 申请者不需要降级，直接加入成功。
                        senderIsShare = true;
                        reduceSucceed.Add(sender);
                        continue;
                    }
                    Reduce reduce = c.ReduceWaitLater(gkey, StateInvalid, rpc.ResultCode);
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
                // 两种情况不需要发reduce
                // 1. share是空的, 可以直接升为Modify
                // 2. sender是share, 而且reducePending的size是0
                var freshReduce = false;
                if (!(cs.Share.Count == 0) && (!senderIsShare || reducePending.Count > 0))
                {
                    // 必须放到另外的线程执行，后面cs.Monitor.WaitAsync();会释放锁。
                    // 这是必须的。
                    _ = Task.Run(async () =>
                    {
                        // 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
                        // 应该也会等待所有任务结束（包括错误）。
                        foreach (var reduce in reducePending)
                        {
                            try
                            {
                                await reduce.Value.Future.Task;
                                switch (reduce.Value.Result.State)
                                {
                                    case StateInvalid:
                                        reduceSucceed.Add(reduce.Key);
                                        break;
                                    case StateReduceErrorFreshAcquire:
                                        freshReduce = true;
                                        break;
                                    default:
                                        reduce.Key.SetError();
                                        logger.Error("Reduce result state={0}", reduce.Value.Result.State);
                                        break;
                                }
                                Perf?.OnReduceEnd(reduce.Value);
                            }
                            catch (Exception ex)
                            {
                                reduce.Key.SetError();
                                // 等待失败不再看作成功。
                                logger.Error(ex, "Reduce {0} {1} {2} {3}", sender, rpc.Argument.State, cs, reduce.Value.Argument);
                            }
                        }
                        using var lockcs = await cs.Monitor.EnterAsync();
                        cs.Monitor.PulseAll();
                    });
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    await cs.Monitor.WaitAsync();
                }

                // 移除成功的。
                foreach (CacheHolder succeed in reduceSucceed)
                {
                    if (succeed != sender)
                    {
                        // sender 不移除：
                        // 1. 如果申请成功，后面会更新到Modify状态。
                        // 2. 如果申请不成功，恢复 cs.Share，保持 Acquired 不变。
                        succeed.Acquired.TryRemove(gkey, out var _);
                    }
                    cs.Share.Remove(succeed);
                }

                // 如果前面降级发生中断(break)，这里就不会为0。
                if (cs.Share.Count == 0)
                {
                    cs.Modify = sender;
                    sender.Acquired[gkey] = StateModify;
                    cs.AcquireStatePending = StateInvalid;
                    cs.Monitor.PulseAll();

                    logger.Debug("8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.SendResultCode(0);
                }
                else
                {
                    // senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
                    // 失败了，要把原来是share的sender恢复。先这样吧。
                    if (senderIsShare)
                        cs.Share.Add(sender);

                    cs.AcquireStatePending = StateInvalid;
                    cs.Monitor.PulseAll();
                    logger.Error("XXX 10 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.Result.State = StateInvalid;
                    rpc.SendResultCode(freshReduce ? StateReduceErrorFreshAcquire : AcquireModifyFailed);
                }
                return 0;
            }
        }

        public sealed class CacheState
        {
            internal CacheHolder Modify { get; set; }
            internal Binary GlobalKey { get; set; }
            internal HashSet<CacheHolder> Share { get; } = new HashSet<CacheHolder>();
            internal Nito.AsyncEx.AsyncMonitor Monitor { get; } = new();
            internal int AcquireStatePending { get; set; } = StateInvalid;

            public override string ToString()
            {
                StringBuilder sb = new();
                ByteBuffer.BuildString(sb, Share);
                return $"P{AcquireStatePending} M{Modify} S{sb}";
            }

            public int GetSenderCacheState(CacheHolder sender)
            {
                if (Modify == sender)
                    return StateModify;
                if (Share.Contains(sender))
                    return StateShare;
                return StateInvalid;
            }
        }

        public sealed class CacheHolder
        {
            private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
            private long ActiveTime = Time.NowUnixMillis;
            private bool Logined = false;
            public bool DebugMode { get; set; }

            public long SessionId { get; private set; }

            public ConcurrentDictionary<Binary, int> Acquired { get; }
            public AsyncLock Mutex { get; } = new();
            public int GlobalCacheManagerHashIndex { get; private set; } // UnBind 的时候不会重置，会一直保留到下一次Bind。

            public long GetActiveTime()
            {
                return Interlocked.Read(ref ActiveTime);
            }

            public void SetActiveTime(long value)
            {
                Interlocked.Exchange(ref ActiveTime, value);
            }

            public CacheHolder(GCMConfig config)
            {
                Acquired = new(config.ConcurrencyLevel, config.InitialCapacity);
            }

            // not under lock
            internal void Kick()
            {
                var peer = Instance.Server.GetSocket(SessionId);
                if (null != peer)
                {
                    peer.UserState = null; // 来自这个Agent的所有请求都会失败。
                    peer.Close(null); // 关闭连接，强制Agent重新登录。
                }
                SessionId = 0; // 清除网络状态。
            }


            public async Task<bool> TryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex, bool login)
            {
                using (await Mutex.AcquireAsync(CancellationToken.None))
                {
                    if (login)
                    {
                        // login 相当于重置，允许再次Login。
                        Logined = true;
                    }
                    else
                    {
                        // relogin 必须login之后才允许ReLogin。这个用来检测Global宕机并重启。
                        if (false == Logined)
                            return false;
                    }

                    if (newSocket.UserState != null)
                        return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。

                    var socket = GlobalCacheManagerServer.Instance.Server.GetSocket(SessionId);
                    if (null == socket)
                    {
                        // old socket not exist or has lost.
                        SessionId = newSocket.SessionId;
                        newSocket.UserState = this;
                        GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
                        return true;
                    }
                    // 每个ServerId只允许一个实例，已经存在了以后，旧的实例上有状态，阻止新的实例登录成功。
                    return false;
                }
            }

            public async Task<bool> TryUnBindSocket(AsyncSocket oldSocket)
            {
                // 这里检查比较严格，但是这些检查应该都不会出现。
                using (await Mutex.AcquireAsync(CancellationToken.None))
                {
                    if (oldSocket.UserState != this)
                        return false; // not bind to this

                    var socket = GlobalCacheManagerServer.Instance.Server.GetSocket(SessionId);
                    if (socket != null && socket != oldSocket)
                        return false; // not same socket: socket is null 意味着当前绑定的已经关闭，此时也允许解除绑定。

                    SessionId = 0;
                    return true;
                }
            }
            public override string ToString()
            {
                return "" + SessionId;
            }

            public bool Reduce(Binary gkey, int state, long fresh, Func<Protocol, Task<long>> response)
            {
                try
                {
                    lock (this)
                    {
                        if (global::Zeze.Util.Time.NowUnixMillis - LastErrorTime < Instance.AchillesHeelConfig.GlobalForbidPeriod)
                            return false;
                    }
                    AsyncSocket peer = GlobalCacheManagerServer.Instance.Server.GetSocket(SessionId);
                    if (null != peer)
                    {
                        var reduce = new Reduce(gkey, state);
                        reduce.ResultCode = fresh;
                        Instance.Perf?.OnReduceBegin(reduce);
                        if (reduce.Send(peer, response, Instance.AchillesHeelConfig.ReduceTimeout))
                            return true;
                        Instance.Perf?.OnReduceCancel(reduce);
                    }
                }
                catch (Exception ex)
                {
                    // 这里的异常只应该是网络发送异常。
                    logger.Error(ex, "ReduceWaitLater Exception {0}", gkey);
                }
                SetError();
                return false;
            }

            private long LastErrorTime = 0;

            public void SetError()
            {
                lock (this)
                {
                    long now = global::Zeze.Util.Time.NowUnixMillis;
                    if (now - LastErrorTime > Instance.AchillesHeelConfig.GlobalForbidPeriod)
                        LastErrorTime = now;
                }
            }

            /// <summary>
            /// 返回null表示发生了网络错误，或者应用服务器已经关闭。
            /// </summary>
            /// <param name="gkey"></param>
            /// <param name="state"></param>
            /// <returns></returns>
            public Reduce ReduceWaitLater(Binary gkey, int state, long fresh)
            {
                try
                {
                    lock (this)
                    {
                        if (global::Zeze.Util.Time.NowUnixMillis - LastErrorTime < Instance.AchillesHeelConfig.GlobalForbidPeriod)
                            return null;
                    }
                    AsyncSocket peer = GlobalCacheManagerServer.Instance.Server.GetSocket(SessionId);
                    if (null != peer)
                    {
                        Reduce reduce = new(gkey, state);
                        reduce.ResultCode = fresh;

                        Instance.Perf?.OnReduceBegin(reduce);
                        _ = reduce.SendAsync(peer, Instance.AchillesHeelConfig.ReduceTimeout);
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
    }
}

namespace Zeze.Services.GlobalCacheManager
{
    public sealed class GlobalKeyState : Bean
    {
        public Binary GlobalKey { get; set; }
        public int State { get; set; }

        public override void Decode(ByteBuffer bb)
        {
            GlobalKey = bb.ReadBinary();
            State = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteBinary(GlobalKey);
            bb.WriteInt(State);
        }

        public override string ToString()
        {
            return GlobalKey + ":" + State;
        }

        public override void ClearParameters()
        {
            GlobalKey = null;
            State = 0;
        }
    }

    public sealed class Acquire : Rpc<GlobalKeyState, GlobalKeyState>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(Acquire).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public Acquire()
        {
        }

        public Acquire(Binary gkey, int state)
        {
            Argument.GlobalKey = gkey;
            Argument.State = state;
        }
    }

    public class Reduce : Rpc<GlobalKeyState, GlobalKeyState>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(Reduce).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public Reduce()
        {
        }

        public Reduce(Binary gkey, int state)
        {
            Argument.GlobalKey = gkey;
            Argument.State = state;
        }
    }

    public sealed class LoginParam : Bean
    {
        public int ServerId { get; set; }

        // GlobalCacheManager 本身没有编号。
        // 启用多个进程，使用 GlobalTableKey.GetHashCode() 分配负载后，报告错误需要这个来识别哪个进程。
        // 当然识别还可以根据 ServerService 绑定的ip和port。
        // 给每个实例加配置不容易维护。
        public int GlobalCacheManagerHashIndex { get; set; }
        public bool DebugMode; // 调试模式下不检查Release Timeout,方便单步调试

        public override void Decode(ByteBuffer bb)
        {
            ServerId = bb.ReadInt();
            GlobalCacheManagerHashIndex = bb.ReadInt();
            DebugMode = bb.ReadBool();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteInt(ServerId);
            bb.WriteInt(GlobalCacheManagerHashIndex);
            bb.WriteBool(DebugMode);
        }

        public override void ClearParameters()
        {
            ServerId = 0;
            GlobalCacheManagerHashIndex = 0;
            DebugMode = false;
        }
    }

    public sealed class AchillesHeelConfigFromGlobal : Bean
    {
        public int MaxNetPing;
        public int ServerProcessTime;
        public int ServerReleaseTimeout;

        public override void Decode(ByteBuffer bb)
        {
            MaxNetPing = bb.ReadInt();
            ServerProcessTime = bb.ReadInt();
            ServerReleaseTimeout = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteInt(MaxNetPing);
            bb.WriteInt(ServerProcessTime);
            bb.WriteInt(ServerReleaseTimeout);
        }

        public override void ClearParameters()
        {
            MaxNetPing = 0;
            ServerProcessTime = 0;
            ServerReleaseTimeout = 0;
        }
    }

    public sealed class Login : Rpc<LoginParam, AchillesHeelConfigFromGlobal>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(Login).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public Login()
        {
        }

        public Login(int id)
        {
            Argument.ServerId = id;
        }
    }

    public sealed class ReLogin : Rpc<LoginParam, EmptyBean>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(ReLogin).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;

        public ReLogin()
        {
        }

        public ReLogin(int id)
        {
            Argument.ServerId = id;
        }
    }

    public sealed class NormalClose : Rpc<EmptyBean, EmptyBean>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(NormalClose).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class AchillesHeel : Bean
    {
        public int ServerId { get; set; } // 必须的。

        public string SecureKey { get; set; } // 安全验证
        public int GlobalCacheManagerHashIndex { get; set; } // 安全验证

        public override void Decode(ByteBuffer bb)
        {
            ServerId = bb.ReadInt();
            SecureKey = bb.ReadString();
            GlobalCacheManagerHashIndex = bb.ReadInt();
        }

        public override void Encode(ByteBuffer bb)
        {
            bb.WriteInt(ServerId);
            bb.WriteString(SecureKey);
            bb.WriteInt(GlobalCacheManagerHashIndex);
        }

        public override void ClearParameters()
        {
            ServerId = 0;
            SecureKey = null;
            GlobalCacheManagerHashIndex = 0;
        }
    }

    public sealed class Cleanup : Rpc<AchillesHeel, EmptyBean>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(Cleanup).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }

    public sealed class ServerService : Service
    {
        public ServerService(Config config) : base("GlobalCacheManager", config)
        {
        }

        public override async void OnSocketClose(AsyncSocket so, Exception e)
        {
            var session = (GlobalCacheManagerServer.CacheHolder)so.UserState;
            // unbind when login
            await session?.TryUnBindSocket(so);
            base.OnSocketClose(so, e);
        }

        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            // Global 处理是纯异步的，直接在io-thread中异步执行。不需要进行线程切换。
            _ = Mission.CallAsync(factoryHandle.Handle, p, null);
        }
    }

    /*
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
    */

    /// <summary>
    /// AchillesHeel!
    /// GlobalAgent 定时发送KeepAlive，只要发现GlobalCacheManager没有相应，
    ///     就释放本地从该GlobalCacheManager申请的资源。
    /// GlobalCacheManager 一定时间（大于客户端发送间隔的两倍）没有收到某个GlobalAgent的KeepAlive，
    ///     就释放该GlobalAgent拥有的资源。【关键】这样定义是否足够，有没有数据安全问题？
    /// 【问题】
    ///     a) 如果GlobalAgent发送KeepAlive的代码死了（不能正确清理本地资源的状态），
    ///     但是其他执行事务的模块还活着，此时就需要把执行事务的模块通过检查一个标志，禁止活动，
    ///     检查这个这个标志在多个GlobalCacheManager时不容易高效实现。
    ///     b) 实行事务时检查标志的代码可能也会某些原因失效，那就更复杂了。
    ///     c) 另外本地要在KeepAlive失败时自动清理，需要记录锁修改状态，并且能正确Checkpoint。
    ///     这在某些异常原因导致本地服务器死掉时很可能无法正常进行。而此时GlobalCacheManager
    ///     超时就清理还是有风险。
    ///     *) 总之，可能的情况太多，KeepAlive还是不够安全。
    ///     所以先不实现了。
    /// </summary>
    public sealed class KeepAlive : Rpc<EmptyBean, EmptyBean>
    {
        public readonly static int ProtocolId_ = Util.FixedHash.Hash32(typeof(KeepAlive).FullName);

        public override int ModuleId => 0;
        public override int ProtocolId => ProtocolId_;
    }
}
