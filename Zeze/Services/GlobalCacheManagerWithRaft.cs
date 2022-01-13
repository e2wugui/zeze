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
using Zeze.Raft;
using Zeze.Services.GlobalCacheManager;
using System.IO;

/*
namespace Zeze.Services
{
    public sealed class GlobalCacheManagerWithRaft
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public static GlobalCacheManagerWithRaft Instance { get; } = new GlobalCacheManagerWithRaft();
        public Zeze.Raft.Raft Raft { get; private set; }
        public RaftDatas RaftData => (RaftDatas)Raft.StateMachine;

        private GlobalCacheManagerWithRaft()
        { 
        }

        public GlobalCacheManagerServer.GCMConfig Config { get; } = new GlobalCacheManagerServer.GCMConfig();

        public void Start(Zeze.Raft.RaftConfig raftconfig, Config config = null)
        {
            lock (this)
            {
                if (Raft != null)
                    return;

                if (null == config)
                {
                    config = new Config();
                    config.AddCustomize(Config);
                    config.LoadAndParse();
                }

                Raft = new Zeze.Raft.Raft(new RaftDatas(Config), raftconfig.Name, raftconfig, config);

                Raft.Server.AddFactoryHandle(
                    new GlobalCacheManager.Acquire().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new GlobalCacheManager.Acquire(),
                        Handle = ProcessAcquireRequest,
                    });

                Raft.Server.AddFactoryHandle(
                    new GlobalCacheManager.Reduce().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new GlobalCacheManager.Reduce(),
                    });

                Raft.Server.AddFactoryHandle(
                    new GlobalCacheManager.Login().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new GlobalCacheManager.Login(),
                        Handle = ProcessLogin,
                    });

                Raft.Server.AddFactoryHandle(
                    new GlobalCacheManager.ReLogin().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new GlobalCacheManager.ReLogin(),
                        Handle = ProcessReLogin,
                    });

                Raft.Server.AddFactoryHandle(
                    new GlobalCacheManager.NormalClose().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new GlobalCacheManager.NormalClose(),
                        Handle = ProcessNormalClose,
                    });

                // 临时注册到这里，安全起见应该起一个新的Service，并且仅绑定到 localhost。
                Raft.Server.AddFactoryHandle(
                    new GlobalCacheManager.Cleanup().TypeId,
                    new Service.ProtocolFactoryHandle()
                    {
                        Factory = () => new GlobalCacheManager.Cleanup(),
                        Handle = ProcessCleanup,
                    });

                OperateFactory.Add(new RemoveCacheState().TypeId,
                    () => new RemoveCacheState());

                OperateFactory.Add(new SetCacheStateAcquireStatePending().TypeId,
                    () => new SetCacheStateAcquireStatePending());
                OperateFactory.Add(new SetCacheStateModify().TypeId,
                    () => new SetCacheStateModify());

                OperateFactory.Add(new AddCacheStateShare().TypeId,
                    () => new AddCacheStateShare());
                OperateFactory.Add(new RemoveCacheStateShare().TypeId,
                    () => new RemoveCacheStateShare());

                OperateFactory.Add(new PutCacheHolderAcquired().TypeId,
                    () => new PutCacheHolderAcquired());
                OperateFactory.Add(new RemoveCacheHolderAcquired().TypeId,
                    () => new RemoveCacheHolderAcquired());

                Raft.Server.Start();
            }
        }

        public void Stop()
        {
            lock (this)
            {
                if (null == Raft)
                    return;
                Raft.Server.Stop();
                Raft = null;
            }
        }

        private CacheHolder GetSession(int id)
        {
            return RaftData.Sessions.GetOrAdd(id, (k) => new CacheHolder(k));
        }

        /// <summary>
        /// 报告错误的时候把相关信息（包括GlobalCacheManager和LogicServer等等）编码，手动Cleanup时，
        /// 解码并连接正确的服务器执行。降低手动风险。
        /// </summary>
        /// <param name="p"></param>
        /// <returns></returns>
        private long ProcessCleanup(Zeze.Net.Protocol p)
        {
            var rpc = p as GlobalCacheManager.Cleanup;

            // 安全性以后加强。
            if (false == rpc.Argument.SecureKey.Equals("Ok! verify secure."))
            {
                rpc.SendResultCode(GlobalCacheManagerServer.CleanupErrorSecureKey);
                return 0;
            }

            var session = GetSession(rpc.Argument.AutoKeyLocalId);
            if (session.GlobalCacheManagerHashIndex != rpc.Argument.GlobalCacheManagerHashIndex)
            {
                // 多点验证
                rpc.SendResultCode(GlobalCacheManagerServer.CleanupErrorGlobalCacheManagerHashIndex);
                return 0;
            }

            if (this.Raft.Server.GetSocket(session.SessionId) != null)
            {
                // 连接存在，禁止cleanup。
                rpc.SendResultCode(GlobalCacheManagerServer.CleanupErrorHasConnection);
                return 0;
            }

            // 还有更多的防止出错的手段吗？

            // XXX verify danger
            Zeze.Util.Scheduler.Instance.Schedule(
                (ThisTask) =>
                {
                    foreach (var gkey in session.Acquired.Keys)
                    {
                        // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                        Release(rpc.Sender.RemoteAddress, rpc.UniqueRequestId, session, gkey);
                    }
                    rpc.SendResultCode(0);
                },
                5 * 60 * 1000); // delay 5 mins

            return 0;
        }

        private long ProcessLogin(Zeze.Net.Protocol p)
        {
            var rpc = p as GlobalCacheManager.Login;
            var session = GetSession(rpc.Argument.ServerId);
            if (false == session.TryBindSocket(
                p.Sender,
                rpc.Argument.GlobalCacheManagerHashIndex))
            {
                rpc.SendResultCode(GlobalCacheManagerServer.LoginBindSocketFail);
                return 0;
            }
            // new login, 比如逻辑服务器重启。release old acquired.
            foreach (var gkey in session.Acquired.Keys)
            {
                // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                Release(rpc.Sender.RemoteAddress, rpc.UniqueRequestId, session, gkey);
            }
            rpc.SendResultCode(0);
            return 0;
        }

        private long ProcessReLogin(Zeze.Net.Protocol p)
        {
            var rpc = p as GlobalCacheManager.ReLogin;
            var session = GetSession(rpc.Argument.ServerId);
            if (false == session.TryBindSocket(p.Sender, rpc.Argument.GlobalCacheManagerHashIndex))
            {
                rpc.SendResultCode(GlobalCacheManagerServer.ReLoginBindSocketFail);
                return 0;
            }
            rpc.SendResultCode(0);
            return 0;
        }
        
        private long ProcessNormalClose(Zeze.Net.Protocol p)
        {
            var rpc = p as GlobalCacheManager.NormalClose;
            var session = rpc.Sender.UserState as CacheHolder;
            if (null == session)
            {
                rpc.SendResultCode(GlobalCacheManagerServer.AcquireNotLogin);
                return 0; // not login
            }
            if (false == session.TryUnBindSocket(p.Sender))
            {
                rpc.SendResultCode(GlobalCacheManagerServer.NormalCloseUnbindFail);
                return 0;
            }
            foreach (var gkey in session.Acquired.Keys)
            {
                // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                Release(rpc.Sender.RemoteAddress, rpc.UniqueRequestId, session, gkey);
            }
            rpc.SendResultCode(0);
            //logger.Debug("After NormalClose global.Count={0}", RaftData.Global.Count);
            return 0;
        }

        private long ProcessAcquireRequest(Zeze.Net.Protocol p)
        {
            var rpc = (Acquire)p;
            rpc.Result.GlobalTableKey = rpc.Argument.GlobalTableKey;
            rpc.Result.State = rpc.Argument.State; // default success

            if (rpc.Sender.UserState == null)
            {
                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                rpc.SendResultCode(GlobalCacheManagerServer.AcquireNotLogin);
                return 0;
            }
            try
            {
                switch (rpc.Argument.State)
                {
                    case GlobalCacheManagerServer.StateInvalid: // realease
                        var session = rpc.Sender.UserState as CacheHolder;
                        var iraftrpc = rpc as IRaftRpc;
                        Release(rpc.Sender.RemoteAddress, iraftrpc.UniqueRequestId, session, rpc.Argument.GlobalTableKey);
                        rpc.SendResult();
                        return 0;

                    case GlobalCacheManagerServer.StateShare:
                        return AcquireShare(rpc);

                    case GlobalCacheManagerServer.StateModify:
                        return AcquireModify(rpc);

                    default:
                        rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                        rpc.SendResultCode(GlobalCacheManagerServer.AcquireErrorState);
                        return 0;
                }
            }
            catch (Exception e)
            {
                logger.Error(e);
                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                rpc.SendResultCode(GlobalCacheManagerServer.AcquireException);
                return 0;
            }
        }

        private void Release(
            string appInstance,
            long requestId,
            CacheHolder holder,
            GlobalCacheManager.GlobalTableKey gkey)
        {
            var cs = RaftData.Global.GetOrAdd(gkey);
            lock (cs)
            {
                var step0 = new OperatesLog(appInstance, requestId);
                if (cs.Modify == holder.Id)
                {
                    step0.SetCacheStateModify(gkey, -1);
                }
                // always try remove
                step0.RemoveCacheStateShare(gkey, holder.Id);

                Raft.AppendLog(step0);

                var step1 = new OperatesLog(appInstance, requestId);
                if (cs.Modify == -1
                    && cs.Share.Count == 0
                    && cs.AcquireStatePending == GlobalCacheManagerServer.StateInvalid)
                {
                    // 安全的从global中删除，没有并发问题。
                    step1.SetCacheStateAcquireStatePending(
                        gkey, GlobalCacheManagerServer.StateRemoved);
                    step1.RemoveCacheState(gkey);
                }
                step1.RemoveCacheHolderAcquired(holder.Id, gkey);
                Raft.AppendLog(step1);
            }
        }

        private int AcquireShare(Acquire rpc)
        {
            var sender = rpc.Sender.UserState as CacheHolder;
            var gkey = rpc.Argument.GlobalTableKey;
            while (true)
            {
                var cs = RaftData.Global.GetOrAdd(gkey);
                lock (cs)
                {
                    if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                        continue;

                    while (cs.AcquireStatePending != GlobalCacheManagerServer.StateInvalid)
                    {
                        switch (cs.AcquireStatePending)
                        {
                            case GlobalCacheManagerServer.StateShare:
                                if (cs.Modify == sender.Id)
                                {
                                    logger.Debug("1 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                    rpc.SendResultCode(GlobalCacheManagerServer.AcquireShareDeadLockFound);
                                    return 0;
                                }
                                break;
                            case GlobalCacheManagerServer.StateModify:
                                if (cs.Modify == sender.Id || cs.Share.Contains(sender.Id))
                                {
                                    logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                    rpc.SendResultCode(GlobalCacheManagerServer.AcquireShareDeadLockFound);
                                    return 0;
                                }
                                break;
                        }
                        logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);
                    }
                    var iraftrpc = rpc as IRaftRpc;
                    var step0 = new OperatesLog(rpc.Sender.RemoteAddress, rpc.UniqueRequestId);
                    step0.SetCacheStateAcquireStatePending(gkey, GlobalCacheManagerServer.StateShare);
                    Raft.AppendLog(step0);
                    // cs.AcquireStatePending = GlobalCacheManager.StateShare;
                    cs.GlobalSerialId = RaftData.SerialIdGenerator.IncrementAndGet(); // TODO

                    if (cs.Modify != -1)
                    {
                        if (cs.Modify == sender.Id)
                        {
                            logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = GlobalCacheManagerServer.StateModify;
                            // 已经是Modify又申请，可能是sender异常关闭，又重启连上。更新一下。应该是不需要的。
                            var setp1 = new OperatesLog(rpc.Sender.RemoteAddress, rpc.UniqueRequestId);
                            setp1.PutCacheHolderAcquired(sender.Id, gkey, GlobalCacheManagerServer.StateModify);
                            setp1.SetCacheStateAcquireStatePending(gkey, GlobalCacheManagerServer.StateInvalid);
                            // cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                            Raft.AppendLog(setp1);
                            rpc.SendResultCode(GlobalCacheManagerServer.AcquireShareAlreadyIsModify);
                            return 0;
                        }

                        Reduce reduceRpc = null; ;
                        var modifyHolder = GetSession(cs.Modify);
                        Zeze.Util.Task.Run(
                            () =>
                            {
                                reduceRpc = modifyHolder.Reduce(
                                    gkey, GlobalCacheManagerServer.StateShare, cs.GlobalSerialId);

                                lock (cs)
                                {
                                    Monitor.PulseAll(cs);
                                }
                            },
                            "GlobalCacheManager.AcquireShare.Reduce");
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);

                        var step2 = new OperatesLog(rpc.Sender.RemoteAddress, rpc.UniqueRequestId);
                        switch (reduceRpc.Result.State)
                        {
                            case GlobalCacheManagerServer.StateShare:
                                step2.PutCacheHolderAcquired(cs.Modify, gkey,
                                    GlobalCacheManagerServer.StateShare);
                                //cs.Modify.Acquired[rpc.Argument.GlobalTableKey] = GlobalCacheManager.StateShare;
                                // 降级成功，有可能降到 Invalid，此时就不需要加入 Share 了。
                                step2.AddCacheStateShare(rpc.Argument.GlobalTableKey, cs.Modify);
                                //cs.Share.Add(cs.Modify); 
                                break;

                            default:
                                // 包含协议返回错误的值的情况。
                                // case StateReduceRpcTimeout:
                                // case StateReduceException:
                                // case StateReduceNetError:
                                step2.SetCacheStateAcquireStatePending(
                                    rpc.Argument.GlobalTableKey, GlobalCacheManagerServer.StateInvalid);
                                //cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                                Raft.AppendLog(step2);

                                Monitor.Pulse(cs);
                                logger.Error("XXX 8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                rpc.SendResultCode(GlobalCacheManagerServer.AcquireShareFailed);
                                return 0;
                        }

                        step2.SetCacheStateModify(gkey, -1);
                        //cs.Modify = null;
                        step2.PutCacheHolderAcquired(sender.Id, gkey, GlobalCacheManagerServer.StateShare);
                        //sender.Acquired[gkey] = GlobalCacheManager.StateShare;
                        step2.AddCacheStateShare(gkey, sender.Id);
                        //cs.Share.Add(sender);
                        step2.SetCacheStateAcquireStatePending(gkey, GlobalCacheManagerServer.StateInvalid);
                        //cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                        Raft.AppendLog(step2);

                        Monitor.Pulse(cs);
                        logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.SendResult();
                        return 0;
                    }

                    var step3 = new OperatesLog(rpc.Sender.RemoteAddress, rpc.UniqueRequestId);
                    step3.PutCacheHolderAcquired(sender.Id, gkey, GlobalCacheManagerServer.StateShare);
                    //sender.Acquired[gkey] = GlobalCacheManager.StateShare;
                    step3.AddCacheStateShare(gkey, sender.Id);
                    //cs.Share.Add(sender);
                    step3.SetCacheStateAcquireStatePending(gkey, GlobalCacheManagerServer.StateInvalid);
                    //cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    Raft.AppendLog(step3);
                    rpc.SendResult();
                    return 0;
                }
            }
        }

        private int AcquireModify(Acquire rpc)
        {
            var sender = rpc.Sender.UserState as CacheHolder;
            var gkey = rpc.Argument.GlobalTableKey;
            while (true)
            {
                var cs = RaftData.Global.GetOrAdd(gkey);
                lock (cs)
                {
                    if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                        continue;

                    while (cs.AcquireStatePending != GlobalCacheManagerServer.StateInvalid)
                    {
                        switch (cs.AcquireStatePending)
                        {
                            case GlobalCacheManagerServer.StateShare:
                                if (cs.Modify == sender.Id)
                                {
                                    logger.Debug("1 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                    rpc.SendResultCode(GlobalCacheManagerServer.AcquireModifyDeadLockFound);
                                    return 0;
                                }
                                break;
                            case GlobalCacheManagerServer.StateModify:
                                if (cs.Modify == sender.Id || cs.Share.Contains(sender.Id))
                                {
                                    logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                    rpc.SendResultCode(GlobalCacheManagerServer.AcquireModifyDeadLockFound);
                                    return 0;
                                }
                                break;
                        }
                        logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);
                    }
                    var step0 = new OperatesLog(rpc.Sender.RemoteAddress, rpc.UniqueRequestId);
                    step0.SetCacheStateAcquireStatePending(gkey, GlobalCacheManagerServer.StateModify);
                    //cs.AcquireStatePending = GlobalCacheManager.StateModify;
                    Raft.AppendLog(step0);
                    cs.GlobalSerialId = RaftData.SerialIdGenerator.IncrementAndGet(); // TODO

                    if (cs.Modify != -1)
                    {
                        if (cs.Modify == sender.Id)
                        {
                            logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            // 已经是Modify又申请，可能是sender异常关闭，又重启连上。更新一下。应该是不需要的。
                            var step1 = new OperatesLog(rpc.Sender.RemoteAddress, rpc.UniqueRequestId);
                            step1.PutCacheHolderAcquired(sender.Id, gkey, GlobalCacheManagerServer.StateModify);
                            //sender.Acquired[rpc.Argument.GlobalTableKey] = GlobalCacheManager.StateModify;
                            step1.SetCacheStateAcquireStatePending(gkey, GlobalCacheManagerServer.StateInvalid);
                            //cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                            Raft.AppendLog(step1);
                            rpc.SendResultCode(GlobalCacheManagerServer.AcquireModifyAlreadyIsModify);
                            return 0;
                        }

                        Reduce reduceRpc = null;
                        var modifyHolder = GetSession(cs.Modify);
                        Zeze.Util.Task.Run(
                            () =>
                            {
                                reduceRpc = modifyHolder.Reduce(
                                    gkey, GlobalCacheManagerServer.StateInvalid, cs.GlobalSerialId);
                                lock (cs)
                                {
                                    Monitor.PulseAll(cs);
                                }
                            },
                            "GlobalCacheManager.AcquireModify.Reduce");
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);

                        var step2 = new OperatesLog(rpc.Sender.RemoteAddress, rpc.UniqueRequestId);

                        switch (reduceRpc.Result.State)
                        {
                            case GlobalCacheManagerServer.StateInvalid:
                                step2.RemoveCacheHolderAcquired(cs.Modify, gkey);
                                //cs.Modify.Acquired.TryRemove(rpc.Argument.GlobalTableKey, out var _);
                                break; // reduce success

                            default:
                                // case StateReduceRpcTimeout:
                                // case StateReduceException:
                                // case StateReduceNetError:
                                step2.SetCacheStateAcquireStatePending(gkey,
                                    GlobalCacheManagerServer.StateInvalid);
                                // cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                                Raft.AppendLog(step2);

                                Monitor.Pulse(cs);

                                logger.Error("XXX 9 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                rpc.SendResultCode(GlobalCacheManagerServer.AcquireModifyFailed);
                                return 0;
                        }

                        step2.SetCacheStateModify(gkey, sender.Id);
                        //cs.Modify = sender.Id;
                        step2.PutCacheHolderAcquired(sender.Id, gkey, GlobalCacheManagerServer.StateModify);
                        //sender.Acquired[rpc.Argument.GlobalTableKey] = GlobalCacheManager.StateModify;
                        step2.SetCacheStateAcquireStatePending(gkey, GlobalCacheManagerServer.StateInvalid);
                        // cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                        Raft.AppendLog(step2);

                        Monitor.Pulse(cs);

                        logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.SendResult();
                        return 0;
                    }

                    List<Util.KV<CacheHolder, GlobalCacheManager.Reduce >> reducePending
                        = new List<Util.KV<CacheHolder, GlobalCacheManager.Reduce>>();
                    HashSet<CacheHolder> reduceSucceed = new HashSet<CacheHolder>();
                    bool senderIsShare = false;
                    // 先把降级请求全部发送给出去。
                    foreach (var c in cs.Share)
                    {
                        if (c == sender.Id)
                        {
                            senderIsShare = true;
                            reduceSucceed.Add(sender);
                            continue;
                        }
                        var shareHolder = GetSession(c);
                        var reduce = shareHolder.ReduceWaitLater(
                            gkey, GlobalCacheManagerServer.StateInvalid, cs.GlobalSerialId);
                        if (null != reduce)
                        {
                            reducePending.Add(Util.KV.Create(shareHolder, reduce));
                        }
                        else
                        {
                            // 网络错误不再认为成功。整个降级失败，要中断降级。
                            // 已经发出去的降级请求要等待并处理结果。后面处理。
                            break;
                        }
                    }

                    var iraftrpc = rpc as IRaftRpc;
                    var step3 = new OperatesLog(rpc.Sender.RemoteAddress, iraftrpc.UniqueRequestId);
                    Zeze.Util.Task.Run(
                        () =>
                        {
                            // 一个个等待是否成功。WaitAll 碰到错误不知道怎么处理的，
                            // 应该也会等待所有任务结束（包括错误）。
                            foreach (var reduce in reducePending)
                            {
                                try
                                {
                                    reduce.Value.Future.Task.Wait();
                                    if (reduce.Value.Result.State == GlobalCacheManagerServer.StateInvalid)
                                    {
                                        // 后面还有个成功的处理循环，但是那里可能包含sender，在这里更新吧。
                                        step3.RemoveCacheHolderAcquired(reduce.Key.Id, gkey);
                                        //reduce.Key.Acquired.TryRemove(rpc.Argument.GlobalTableKey, out var _);
                                        reduceSucceed.Add(reduce.Key);
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
                        },
                        "GlobalCacheManager.AcquireModify.WaitReduce");
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    Monitor.Wait(cs);

                    // 移除成功的。
                    foreach (CacheHolder succeed in reduceSucceed)
                    {
                        step3.RemoveCacheStateShare(gkey, succeed.Id);
                        //cs.Share.Remove(succeed.Id);
                    }

                    Raft.AppendLog(step3);

                    // 如果前面降级发生中断(break)，这里不会为0。
                    if (cs.Share.Count == 0)
                    {
                        var step4 = new OperatesLog(rpc.Sender.RemoteAddress, rpc.UniqueRequestId);
                        step4.SetCacheStateModify(gkey, sender.Id);
                        //cs.Modify = sender.Id;
                        step4.PutCacheHolderAcquired(sender.Id, gkey, GlobalCacheManagerServer.StateModify);
                        //sender.Acquired[gkey] = GlobalCacheManager.StateModify;
                        step4.SetCacheStateAcquireStatePending(gkey, GlobalCacheManagerServer.StateInvalid);
                        //cs.AcquireStatePending = GlobalCacheManager.StateInvalid;

                        Raft.AppendLog(step4);

                        Monitor.Pulse(cs); // Pending 结束，唤醒一个进来就可以了。

                        logger.Debug("8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.SendResult();
                    }
                    else
                    {
                        var step5 = new OperatesLog(rpc.Sender.RemoteAddress, rpc.UniqueRequestId);
                        // senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
                        if (senderIsShare)
                        {
                            step5.AddCacheStateShare(gkey, sender.Id);
                            //cs.Share.Add(sender);
                            // 失败了，要把原来是share的sender恢复。先这样吧。
                        }

                        step5.SetCacheStateAcquireStatePending(gkey, GlobalCacheManagerServer.StateInvalid);
                        //cs.AcquireStatePending = GlobalCacheManager.StateInvalid;

                        Raft.AppendLog(step5);

                        Monitor.Pulse(cs); // Pending 结束，唤醒一个进来就可以了。

                        logger.Error("XXX 10 {0} {1} {2}", sender, rpc.Argument.State, cs);

                        rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                        rpc.SendResultCode(GlobalCacheManagerServer.AcquireModifyFailed);
                    }
                    // 很好，网络失败不再看成成功，发现除了加break，
                    // 其他处理已经能包容这个改动，都不用动。
                    return 0;
                }
            }
        }

        public class RaftDatas : Zeze.Raft.StateMachine
        {
            public MapsOnRocksDb Storage { get; }

            public MapsOnRocksDb.Map<GlobalTableKey, CacheState> Global { get; }
            public Zeze.Util.AtomicLong SerialIdGenerator = new Util.AtomicLong();
            public MapsOnRocksDb.Map<int, CacheHolder> Sessions { get; }

            public RaftDatas(GlobalCacheManagerServer.GCMConfig config)
            {
                Storage = new MapsOnRocksDb(".");

                Global = Storage.GetOrAdd<GlobalTableKey, CacheState>(
                    "Global", config.InitialCapacity, config.InitialCapacity, config.ConcurrencyLevel);

                Sessions = Storage.GetOrAdd<int, CacheHolder>(
                    "Sessions", 10_0000, 10_0000, config.ConcurrencyLevel);

                AddFactory(new OperatesLog("", 0).TypeId, () => new OperatesLog("", 0));
            }

            public override void LoadFromSnapshot(string path)
            {
                lock (Raft)
                {
                    Storage.Restore(path);
                }
            }

            public override bool Snapshot(
                string path,
                out long LastIncludedIndex,
                out long LastIncludedTerm)
            {
                string checkpointDir = null;
                lock (Raft)
                {
                    var lastAppliedLog = Raft.LogSequence.LastAppliedLog();
                    LastIncludedIndex = lastAppliedLog.Index;
                    LastIncludedTerm = lastAppliedLog.Term;
                    checkpointDir = Storage.Checkpoint();
                }
                Storage.Backup(checkpointDir, path);
                Directory.Delete(checkpointDir, true);

                long oldFirstIndex = 0;
                lock (Raft)
                {
                    // 先关闭文件，结束Snapshot。
                    // 马上调整FirstIndex允许请求在新的状态上工作。
                    // 然后在锁外，慢慢删除旧的日志。
                    oldFirstIndex = Raft.LogSequence.GetAndSetFirstIndex(LastIncludedIndex);
                }
                Raft.LogSequence.RemoveLogBeforeLastApplied(oldFirstIndex);
                return true;
            }
        }
        /// <summary>
        /// 【优化】按顺序记录多个修改数据的操作，减少提交给Raft的日志数量。
        /// </summary>
        public sealed class OperatesLog : Log
        {
            private List<Operate> Operates { get; } = new List<Operate>();

            public OperatesLog(string appInstance, long requestId) : base(appInstance, requestId)
            {

            }

            public void RemoveCacheState(GlobalCacheManager.GlobalTableKey key)
            {
                Operates.Add(new RemoveCacheState()
                {
                    Key = key,
                });
            }

            public void SetCacheStateAcquireStatePending(
                GlobalCacheManager.GlobalTableKey key, int state)
            {
                Operates.Add(new SetCacheStateAcquireStatePending()
                {
                    Key = key,
                    AcquireStatePending = state,
                });
            }

            public void SetCacheStateModify(
                GlobalCacheManager.GlobalTableKey key, int id)
            {
                Operates.Add(new SetCacheStateModify()
                {
                    Key = key,
                    Modify = id,
                });
            }

            public void AddCacheStateShare(GlobalCacheManager.GlobalTableKey key, int id)
            {
                Operates.Add(new AddCacheStateShare()
                {
                    Key = key,
                    Share = id,
                });
            }

            public void RemoveCacheStateShare(GlobalCacheManager.GlobalTableKey key, int id)
            {
                Operates.Add(new RemoveCacheStateShare()
                {
                    Key = key,
                    Share = id,
                });
            }

            public void PutCacheHolderAcquired(
                int id, GlobalCacheManager.GlobalTableKey key, int state)
            {
                Operates.Add(new PutCacheHolderAcquired()
                {
                    Id = id,
                    Key = key, 
                    AcquireState = state,
                });
            }

            public void RemoveCacheHolderAcquired(
                int id, GlobalCacheManager.GlobalTableKey key)
            {
                Operates.Add(new RemoveCacheHolderAcquired()
                {
                    Id = id,
                    Key = key,
                });
            }

            public override void Apply(RaftLog holder, Zeze.Raft.StateMachine stateMachine)
            {
                var sm = stateMachine as RaftDatas;
                foreach (var op in Operates)
                {
                    op.Apply(sm);
                }
            }

            public override void Decode(ByteBuffer bb)
            {
                base.Decode(bb);
                for (int count = bb.ReadInt(); count > 0; --count)
                {
                    var opid = bb.ReadInt();
                    if (Instance.OperateFactory.TryGetValue(opid, out var factory))
                    {
                        Operate op = factory();
                        op.Decode(bb);
                    }
                    else
                    {
                        throw new Exception($"Unknown Operate With Id={opid}");
                    }
                }
            }

            public override void Encode(ByteBuffer bb)
            {
                base.Encode(bb);
                bb.WriteInt(Operates.Count);
                foreach (var op in Operates)
                {
                    bb.WriteInt(op.TypeId);
                    op.Encode(bb);
                }
            }
        }

        private Dictionary<int, Func<Operate>> OperateFactory { get; }
            = new Dictionary<int, Func<Operate>>();

        public abstract class Operate : Serializable
        {
            public virtual int TypeId => (int)Zeze.Transaction.Bean.Hash32(GetType().FullName);
            // 这里不管多线程，由调用者决定并发。
            public abstract void Apply(RaftDatas sm);
            public abstract void Encode(ByteBuffer bb);
            public abstract void Decode(ByteBuffer bb);
        }

        public sealed class PutCacheHolderAcquired : Operate
        {
            public int Id { get; set; }
            public GlobalCacheManager.GlobalTableKey Key { get; set; }
            public int AcquireState { get; set; }

            public override void Apply(RaftDatas sm)
            {
                sm.Sessions.Update(Id, (v) => v.Acquired[Key] = AcquireState);
            }

            public override void Decode(ByteBuffer bb)
            {
                Id = bb.ReadInt();
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
                AcquireState = bb.ReadInt();
            }

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteInt(Id);
                Key.Encode(bb);
                bb.WriteInt(AcquireState);
            }
        }

        public sealed class RemoveCacheHolderAcquired : Operate
        {
            public int Id { get; set; }
            public GlobalCacheManager.GlobalTableKey Key { get; set; }

            public override void Apply(RaftDatas sm)
            {
                sm.Sessions.Update(Id, (v) => v.Acquired.TryRemove(Key, out var _));
            }

            public override void Decode(ByteBuffer bb)
            {
                Id = bb.ReadInt();
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
            }

            public override void Encode(ByteBuffer bb)
            {
                bb.WriteInt(Id);
                Key.Encode(bb);
            }
        }

        public sealed class RemoveCacheState : Operate
        {
            public GlobalCacheManager.GlobalTableKey Key { get; set; }

            public override void Apply(RaftDatas sm)
            {
                sm.Global.Remove(Key);
            }

            public override void Decode(ByteBuffer bb)
            {
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
            }

            public override void Encode(ByteBuffer bb)
            {
                Key.Encode(bb);
            }
        }

        public sealed class SetCacheStateAcquireStatePending : Operate
        {
            public GlobalCacheManager.GlobalTableKey Key { get; set; }
            public int AcquireStatePending { get; set; }

            public override void Apply(RaftDatas sm)
            {
                sm.Global.Update(Key, (v) => v.AcquireStatePending = AcquireStatePending);
            }

            public override void Decode(ByteBuffer bb)
            {
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
                AcquireStatePending = bb.ReadInt();
            }

            public override void Encode(ByteBuffer bb)
            {
                Key.Encode(bb);
                bb.WriteInt(AcquireStatePending);
            }
        }

        public sealed class SetCacheStateModify : Operate
        {
            public GlobalCacheManager.GlobalTableKey Key { get; set; }
            public int Modify { get; set; }

            public override void Apply(RaftDatas sm)
            {
                sm.Global.Update(Key, (v) => v.Modify = Modify);
            }

            public override void Decode(ByteBuffer bb)
            {
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
                Modify = bb.ReadInt();
            }

            public override void Encode(ByteBuffer bb)
            {
                Key.Encode(bb);
                bb.WriteInt(Modify);
            }
        }

        public sealed class AddCacheStateShare : Operate
        {
            public Services.GlobalCacheManager.GlobalTableKey Key { get; set; }
            public int Share { get; set; }

            public override void Apply(RaftDatas sm)
            {
                sm.Global.Update(Key, (v) => v.Share.Add(Share));
            }

            public override void Decode(ByteBuffer bb)
            {
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
                Share = bb.ReadInt();
            }

            public override void Encode(ByteBuffer bb)
            {
                Key.Encode(bb);
                bb.WriteInt(Share);
            }
        }

        public sealed class RemoveCacheStateShare : Operate
        {
            public Services.GlobalCacheManager.GlobalTableKey Key { get; set; }
            public int Share { get; set; }

            public override void Apply(RaftDatas sm)
            {
                sm.Global.Update(Key, (v) => v.Share.Remove(Share));
            }

            public override void Decode(ByteBuffer bb)
            {
                Key = new Services.GlobalCacheManager.GlobalTableKey();
                Key.Decode(bb);
                Share = bb.ReadInt();
            }

            public override void Encode(ByteBuffer bb)
            {
                Key.Encode(bb);
                bb.WriteInt(Share);
            }
        }

        public sealed class CacheState : Serializable
        {
            internal int AcquireStatePending { get; set; } = GlobalCacheManagerServer.StateInvalid;
            internal long GlobalSerialId { get; set; }
            internal int Modify { get; set; } // AutoKeyLocalId
            internal HashSet<int> Share { get; } = new HashSet<int>(); // AutoKeyLocalIds

            public override string ToString()
            {
                StringBuilder sb = new StringBuilder();
                ByteBuffer.BuildString(sb, Share);
                return $"P{AcquireStatePending} M{Modify} S{sb}";
            }

            public CacheState()
            {
            }

            private CacheState(CacheState other)
            {
                AcquireStatePending = other.AcquireStatePending;
                Modify = other.Modify;
                foreach (var e in other.Share)
                {
                    Share.Add(e);
                }
            }

            public void Decode(ByteBuffer bb)
            {
                AcquireStatePending = bb.ReadInt();
                Modify = bb.ReadInt();
                Share.Clear();
                for (int count = bb.ReadInt(); count > 0; --count)
                {
                    Share.Add(bb.ReadInt());
                }
            }

            public void Encode(ByteBuffer bb)
            {
                bb.WriteInt(AcquireStatePending);
                bb.WriteInt(Modify);
                bb.WriteInt(Share.Count);
                foreach (var e in Share)
                {
                    bb.WriteInt(e);
                }
            }
        }

        public sealed class CacheHolder : Serializable
        {
            private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

            // local only. 每一个Raft服务器独立设置。【不会系列化】。
            public long SessionId { get; private set; }
            public int GlobalCacheManagerHashIndex { get; private set; } // UnBind 的时候不会重置，会一直保留到下一次Bind。

            // 已分配给这个cache的记录。【需要系列化】。
            public ConcurrentDictionary<GlobalTableKey, int> Acquired { get; }
                
            public CacheHolder(GlobalCacheManagerServer.GCMConfig config)
            {
                Acquired = new ConcurrentDictionary<GlobalTableKey, int>
                    (config.ConcurrencyLevel, config.InitialCapacity);
            }

            // 【需要系列化】。
            public int Id { get; private set; }

            public CacheHolder(int key)
            {
                Id = key;
            }

            public bool TryBindSocket(Zeze.Net.AsyncSocket newSocket,
                int _GlobalCacheManagerHashIndex)
            {
                lock (this)
                {
                    if (newSocket.UserState != null)
                        return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。

                    var socket = Services.GlobalCacheManagerWithRaft.Instance.Raft.Server.GetSocket(SessionId);
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

            public bool TryUnBindSocket(Zeze.Net.AsyncSocket oldSocket)
            {
                lock (this)
                {
                    // 这里检查比较严格，但是这些检查应该都不会出现。

                    if (oldSocket.UserState != this)
                        return false; // not bind to this

                    var socket = Services.GlobalCacheManagerWithRaft.Instance.Raft.Server.GetSocket(SessionId);
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

            public Reduce Reduce(GlobalTableKey gkey, int state, long globalSerialId)
            {
                var reduce = ReduceWaitLater(gkey, state, globalSerialId);
                try
                {
                    if (null != reduce)
                    {
                        reduce.Future.Task.Wait();
                        // 如果rpc返回错误的值，外面能处理。
                        return reduce;
                    }
                    reduce.Result.State = GlobalCacheManagerServer.StateReduceNetError;
                    return reduce;
                }
                catch (Zeze.Net.RpcTimeoutException timeoutex)
                {
                    // 等待超时，应该报告错误。
                    logger.Error(timeoutex, "Reduce RpcTimeoutException {0} target={1}", state, SessionId);
                    reduce.Result.State = GlobalCacheManagerServer.StateReduceRpcTimeout;
                    return reduce;
                }
                catch (Exception ex)
                {
                    logger.Error(ex, "Reduce Exception {0} target={1}", state, SessionId);
                    reduce.Result.State = GlobalCacheManagerServer.StateReduceException;
                    return reduce;
                }
            }

            public const long ForbidPeriod = 10 * 1000; // 10 seconds
            private long LastErrorTime = 0; // 本地变量，【不会系列化】。

            public void SetError()
            {
                lock (this)
                {
                    long now = global::Zeze.Util.Time.NowUnixMillis;
                    if (now - LastErrorTime > ForbidPeriod)
                        LastErrorTime = now;
                }
            }
            /// <summary>
            /// 返回null表示发生了网络错误，或者应用服务器已经关闭。
            /// </summary>
            /// <param name="gkey"></param>
            /// <param name="state"></param>
            /// <returns></returns>
            public GlobalCacheManager.Reduce ReduceWaitLater(
                GlobalCacheManager.GlobalTableKey gkey, int state, long globalSerialId)
            {
                try
                {
                    lock (this)
                    {
                        if (global::Zeze.Util.Time.NowUnixMillis - LastErrorTime < ForbidPeriod)
                            return null;
                    }
                    Zeze.Net.AsyncSocket peer = Services.GlobalCacheManagerWithRaft.Instance.Raft.Server.GetSocket(SessionId);
                    if (null != peer)
                    {
                        var reduce = new Reduce(gkey, state, globalSerialId);
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

            public CacheHolder()
            {
            }

            private CacheHolder(CacheHolder other)
            {
                SessionId = other.SessionId;
                GlobalCacheManagerHashIndex = other.GlobalCacheManagerHashIndex;
                foreach (var e in other.Acquired)
                {
                    Acquired.TryAdd(e.Key, e.Value);
                }
            }

            public void Decode(ByteBuffer bb)
            {
                Id = bb.ReadInt();
                Acquired.Clear();
                for (int count = bb.ReadInt(); count > 0; --count)
                {
                    var key = new Services.GlobalCacheManager.GlobalTableKey();
                    key.Decode(bb);
                    int value = bb.ReadInt();
                    Acquired.TryAdd(key, value);
                }
            }

            public void Encode(ByteBuffer bb)
            {
                bb.WriteInt(Id);
                bb.WriteInt(Acquired.Count);
                foreach (var e in Acquired)
                {
                    e.Key.Encode(bb);
                    bb.WriteInt(e.Value);
                }
            }
        }
    }
}
*/