
using System;
using Zeze.Raft.RocksRaft;
using Zeze.Beans.GlobalCacheManagerWithRaft;
using System.Collections.Generic;
using System.Collections.Concurrent;
using Zeze.Net;

namespace Zeze.Services
{
    public class GlobalCacheManagerWithRaft : AbstractGlobalCacheManagerWithRaft
    {
        protected override long ProcessAcquireRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as Acquire;
            rpc.Result.GlobalTableKey = rpc.Argument.GlobalTableKey;
            rpc.Result.State = rpc.Argument.State; // default success
            rpc.ResultCode = 0;

            if (rpc.Sender.UserState == null)
            {
                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                rpc.SendResultCode(GlobalCacheManagerServer.AcquireNotLogin);
                return 0;
            }

            var proc = Rocks.NewProcedure(() =>
            {
                switch (rpc.Argument.State)
                {
                    case GlobalCacheManagerServer.StateInvalid: // release
                        rpc.Result.State = Release(rpc.Sender.UserState as CacheHolder, rpc.Argument.GlobalTableKey, true);
                        return 0;

                    case GlobalCacheManagerServer.StateShare:
                        return AcquireShare(rpc);

                    case GlobalCacheManagerServer.StateModify:
                        return AcquireModify(rpc);

                    default:
                        rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                        return GlobalCacheManagerServer.AcquireErrorState;
                }
            });

            proc.Rpc = rpc; // 设置这个让 RocksRaft.Procedure 成功结束的时候自动发送结果。
            var result = proc.Call();
            if (0 != result)
                rpc.SendResultCode(result); // 失败结果从这里发送。

            return 0; // has handle all error.
        }

        private long AcquireShare(Acquire rpc)
        {
            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;

            while (true)
            {
                var lockey = Locks.Get(rpc.Argument.GlobalTableKey);
                lockey.Enter();
                try
                {
                    CacheState cs = GlobalStates.GetOrAdd(rpc.Argument.GlobalTableKey);
                    if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                        continue;

                    if (cs.Modify != -1 && cs.Share.Count > 0)
                        throw new Exception("CacheState state error");

                    while (cs.AcquireStatePending != GlobalCacheManagerServer.StateInvalid
                        && cs.AcquireStatePending != GlobalCacheManagerServer.StateRemoved)
                    {
                        switch (cs.AcquireStatePending)
                        {
                            case GlobalCacheManagerServer.StateShare:
                                if (cs.Modify == -1)
                                    throw new Exception("CacheState state error");

                                if (cs.Modify == sender.ServerId)
                                {
                                    logger.Debug("1 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                    return GlobalCacheManagerServer.AcquireShareDeadLockFound;
                                }
                                break;

                            case GlobalCacheManagerServer.StateModify:
                                if (cs.Modify == sender.ServerId || cs.Share.Contains(sender.ServerId))
                                {
                                    logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                    return GlobalCacheManagerServer.AcquireShareDeadLockFound;
                                }
                                break;

                            case GlobalCacheManagerServer.StateRemoving:
                                break;
                        }
                        logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        lockey.Wait();
                        if (cs.Modify != -1 && cs.Share.Count > 0)
                            throw new Exception("CacheState state error");
                    }

                    if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                        continue; // concurrent release.

                    cs.AcquireStatePending = GlobalCacheManagerServer.StateShare;
                    cs.GlobalSerialId = SerialIdGenerator.IncrementAndGet();
                    var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(sender.ServerId);
                    if (cs.Modify != -1)
                    {
                        if (cs.Modify == sender.ServerId)
                        {
                            cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                            logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = GlobalCacheManagerServer.StateModify;
                            // 已经是Modify又申请，可能是sender异常关闭，
                            // 又重启连上。更新一下。应该是不需要的。
                            SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                            rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                            return GlobalCacheManagerServer.AcquireShareAlreadyIsModify;
                        }

                        Reduce reduceRpc = null;
                        Zeze.Util.Task.Run(
                            () =>
                            {
                                reduceRpc = null;// cs.Modify.Reduce(rpc.Argument.GlobalTableKey, GlobalCacheManagerServer.StateShare, cs.GlobalSerialId);

                                lockey.Enter();
                                try
                                {
                                    lockey.PulseAll();
                                }
                                finally
                                {
                                    lockey.Exit();
                                }
                            },
                            "GlobalCacheManager.AcquireShare.Reduce");
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        lockey.Wait();

                        var ModifyAcquired = ServerAcquiredTemplate.OpenTableWithType(cs.Modify);
                        switch (reduceRpc.Result.State)
                        {
                            case GlobalCacheManagerServer.StateShare:
                                ModifyAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                                cs.Share.Add(cs.Modify); // 降级成功。
                                break;

                            case GlobalCacheManagerServer.StateInvalid:
                                // 降到了 Invalid，此时就不需要加入 Share 了。
                                ModifyAcquired.Remove(rpc.Argument.GlobalTableKey);
                                break;

                            default:
                                // 包含协议返回错误的值的情况。
                                // case StateReduceRpcTimeout:
                                // case StateReduceException:
                                // case StateReduceNetError:
                                cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                                lockey.Pulse();

                                logger.Error("XXX 8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                return GlobalCacheManagerServer.AcquireShareFailed;
                        }

                        cs.Modify = -1;
                        SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                        cs.Share.Add(sender.ServerId);
                        cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                        lockey.Pulse();
                        logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                        return 0; // 成功也会自动发送结果.
                    }

                    SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                    cs.Share.Add(sender.ServerId);
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                    lockey.Pulse();
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                    return 0; // 成功也会自动发送结果.
                }
                finally
                {
                    lockey.Exit();
                }
            }
        }

        private long AcquireModify(Acquire rpc)
        {
            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;

            while (true)
            {
                var lockey = Locks.Get(rpc.Argument.GlobalTableKey);
                lockey.Enter();
                try
                {
                    CacheState cs = GlobalStates.GetOrAdd(rpc.Argument.GlobalTableKey);
                    if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                        continue;

                    if (cs.Modify != -1 && cs.Share.Count > 0)
                        throw new Exception("CacheState state error");

                    while (cs.AcquireStatePending != GlobalCacheManagerServer.StateInvalid
                        && cs.AcquireStatePending != GlobalCacheManagerServer.StateRemoved)
                    {
                        switch (cs.AcquireStatePending)
                        {
                            case GlobalCacheManagerServer.StateShare:
                                if (cs.Modify == -1)
                                {
                                    logger.Error("cs state must be modify");
                                    throw new Exception("CacheState state error");
                                }
                                if (cs.Modify == sender.ServerId)
                                {
                                    logger.Debug("1 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                    return GlobalCacheManagerServer.AcquireModifyDeadLockFound;
                                }
                                break;
                            case GlobalCacheManagerServer.StateModify:
                                if (cs.Modify == sender.ServerId || cs.Share.Contains(sender.ServerId))
                                {
                                    logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                    return GlobalCacheManagerServer.AcquireModifyDeadLockFound;
                                }
                                break;
                            case GlobalCacheManagerServer.StateRemoving:
                                break;
                        }
                        logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        lockey.Wait();

                        if (cs.Modify != -1 && cs.Share.Count > 0)
                            throw new Exception("CacheState state error");
                    }
                    if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                        continue; // concurrent release

                    cs.AcquireStatePending = GlobalCacheManagerServer.StateModify;
                    cs.GlobalSerialId = SerialIdGenerator.IncrementAndGet();
                    var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(sender.ServerId);
                    if (cs.Modify != -1)
                    {
                        if (cs.Modify == sender.ServerId)
                        {
                            logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            // 已经是Modify又申请，可能是sender异常关闭，又重启连上。
                            // 更新一下。应该是不需要的。
                            SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                            rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                            cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                            lockey.Pulse();
                            return GlobalCacheManagerServer.AcquireModifyAlreadyIsModify;
                        }

                        Reduce reduceRpc = null;
                        Zeze.Util.Task.Run(
                            () =>
                            {
                                reduceRpc = CacheHolder.Reduce(Sessions, cs.Modify, rpc.Argument.GlobalTableKey, GlobalCacheManagerServer.StateInvalid, cs.GlobalSerialId);
                                lockey.Enter();
                                try
                                {
                                    lockey.PulseAll();
                                }
                                finally
                                {
                                    lockey.Exit();
                                }
                            },
                            "GlobalCacheManager.AcquireModify.Reduce");
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        lockey.Wait();

                        var ModifyAcquired = ServerAcquiredTemplate.OpenTableWithType(cs.Modify);
                        switch (reduceRpc.Result.State)
                        {
                            case GlobalCacheManagerServer.StateInvalid:
                                ModifyAcquired.Remove(rpc.Argument.GlobalTableKey);
                                break; // reduce success

                            default:
                                // case StateReduceRpcTimeout:
                                // case StateReduceException:
                                // case StateReduceNetError:
                                cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                                lockey.Pulse();

                                logger.Error("XXX 9 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                return GlobalCacheManagerServer.AcquireModifyFailed;
                        }

                        cs.Modify = sender.ServerId;
                        cs.Share.Remove(sender.ServerId);
                        SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                        cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                        lockey.Pulse();

                        logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                        return 0;
                    }

                    List<Util.KV<CacheHolder, Reduce>> reducePending = new();
                    HashSet<CacheHolder> reduceSucceed = new();
                    bool senderIsShare = false;
                    // 先把降级请求全部发送给出去。
                    foreach (var c in cs.Share)
                    {
                        if (c == sender.ServerId)
                        {
                            senderIsShare = true;
                            reduceSucceed.Add(sender);
                            continue;
                        }
                        Reduce reduce = CacheHolder.ReduceWaitLater(Sessions, c, out var session,
                            rpc.Argument.GlobalTableKey, GlobalCacheManagerServer.StateInvalid, cs.GlobalSerialId);
                        if (null != reduce)
                        {
                            reducePending.Add(Util.KV.Create(session, reduce));
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
                    if (!(cs.Share.Count == 0) && (!senderIsShare || reducePending.Count > 0))
                    {
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
                                        // 后面还有个成功的处理循环，但是那里可能包含sender，
                                        // 在这里更新吧。
                                        var KeyAcquired = ServerAcquiredTemplate.OpenTableWithType(reduce.Key.ServerId);
                                        KeyAcquired.Remove(rpc.Argument.GlobalTableKey);
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
                                    // 等待失败不再看作成功。
                                    logger.Error(ex, "Reduce {0} {1} {2} {3}", sender, rpc.Argument.State, cs, reduce.Value.Argument);
                                }
                            }
                            lockey.Enter();
                            try
                            {
                                // 需要唤醒等待任务结束的，但没法指定，只能全部唤醒。
                                lockey.PulseAll();
                            }
                            finally
                            {
                                lockey.Exit();
                            }
                        },
                        "GlobalCacheManager.AcquireModify.WaitReduce");
                        logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        lockey.Wait();
                    }

                    // 移除成功的。
                    foreach (CacheHolder succeed in reduceSucceed)
                    {
                        cs.Share.Remove(succeed.ServerId);
                    }

                    // 如果前面降级发生中断(break)，这里就不会为0。
                    if (cs.Share.Count == 0)
                    {
                        cs.Modify = sender.ServerId;
                        SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                        cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                        lockey.Pulse(); // Pending 结束，唤醒一个进来就可以了。

                        logger.Debug("8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                        return 0;
                    }

                    // senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
                    // 失败了，要把原来是share的sender恢复。先这样吧。
                    if (senderIsShare)
                        cs.Share.Add(sender.ServerId);

                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                    lockey.Pulse(); // Pending 结束，唤醒一个进来就可以了。

                    logger.Error("XXX 10 {0} {1} {2}", sender, rpc.Argument.State, cs);

                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                    return GlobalCacheManagerServer.AcquireModifyFailed;
                }
                finally
                {
                    lockey.Exit();
                }
            }
        }

        private int Release(CacheHolder sender, GlobalTableKey gkey, bool noWait)
        {
            int result = 0;
            Rocks.NewProcedure(() =>
            {
                result = _Release(sender, gkey, noWait);
                return 0;
            }).Call();
            return result;
        }

        private int _Release(CacheHolder sender, GlobalTableKey gkey, bool noWait)
        {
            while (true)
            {
                var lockey = Locks.Get(gkey);
                lockey.Enter();
                try
                {
                    CacheState cs = GlobalStates.GetOrAdd(gkey);
                    if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                        continue; // 这个是不可能的，因为有Release请求进来意味着肯定有拥有者(share or modify)，此时不可能进入StateRemoved。

                    while (cs.AcquireStatePending != GlobalCacheManagerServer.StateInvalid
                        && cs.AcquireStatePending != GlobalCacheManagerServer.StateRemoved)
                    {
                        switch (cs.AcquireStatePending)
                        {
                            case GlobalCacheManagerServer.StateShare:
                            case GlobalCacheManagerServer.StateModify:
                                logger.Debug("Release 0 {} {} {}", sender, gkey, cs);
                                if (noWait)
                                    return GetSenderCacheState(cs, sender);
                                break;
                            case GlobalCacheManagerServer.StateRemoving:
                                // release 不会导致死锁，等待即可。
                                break;
                        }
                        lockey.Wait();
                    }
                    if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                    {
                        continue;
                    }
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateRemoving;

                    if (cs.Modify == sender.ServerId)
                        cs.Modify = -1;
                    cs.Share.Remove(sender.ServerId); // always try remove

                    if (cs.Modify == -1
                        && cs.Share.Count == 0
                        && cs.AcquireStatePending == GlobalCacheManagerServer.StateInvalid)
                    {
                        // 安全的从global中删除，没有并发问题。
                        cs.AcquireStatePending = GlobalCacheManagerServer.StateRemoved;
                        GlobalStates.Remove(gkey);
                    }
                    else
                    {
                        cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                    }
                    var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(sender.ServerId);
                    SenderAcquired.Remove(gkey);
                    lockey.Pulse();
                    return GetSenderCacheState(cs, sender);
                }
                finally
                {
                    lockey.Exit();
                }
            }
        }

        private int GetSenderCacheState(CacheState cs, CacheHolder sender)
        {
            if (cs.Modify == sender.ServerId)
                return GlobalCacheManagerServer.StateModify;
            if (cs.Share.Contains(sender.ServerId))
                return GlobalCacheManagerServer.StateShare;
            return GlobalCacheManagerServer.StateInvalid;
        }

        protected override long ProcessLoginRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as Login;
            var session = Sessions.GetOrAdd(rpc.Argument.ServerId,
                (_) => new CacheHolder() { GlobalInstance = this, ServerId = rpc.Argument.ServerId });

            lock (session) // 同一个节点互斥。不同节点Bind不需要互斥，Release由Raft-Leader唯一性提供保护。
            {
                if (false == session.TryBindSocket(rpc.Sender, rpc.Argument.GlobalCacheManagerHashIndex))
                {
                    rpc.SendResultCode(GlobalCacheManagerServer.LoginBindSocketFail);
                    return 0;
                }
                // new login, 比如逻辑服务器重启。release old acquired.
                var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(session.ServerId);
                SenderAcquired.Walk((key, value) =>
                {
                    Release(session, key, false);
                    return true; // continue walk
                });
                rpc.SendResultCode(0);
                return 0;
            }
        }

        protected override long ProcessReLoginRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as ReLogin;
            var session = Sessions.GetOrAdd(rpc.Argument.ServerId,
                (key) => new CacheHolder() { GlobalInstance = this, ServerId = rpc.Argument.ServerId });

            lock (session) // 同一个节点互斥。
            {
                if (false == session.TryBindSocket(rpc.Sender, rpc.Argument.GlobalCacheManagerHashIndex))
                {
                    rpc.SendResultCode(GlobalCacheManagerServer.ReLoginBindSocketFail);
                    return 0;
                }
                rpc.SendResultCode(0);
                return 0;
            }
        }

        protected override long ProcessNormalCloseRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as NormalClose;
            if (rpc.Sender.UserState is not CacheHolder session)
            {
                rpc.SendResultCode(GlobalCacheManagerServer.AcquireNotLogin);
                return 0; // not login
            }

            lock (session) // 同一个节点互斥。不同节点Bind不需要互斥，Release由Raft-Leader唯一性提供保护。
            {
                if (false == session.TryUnBindSocket(rpc.Sender))
                {
                    rpc.SendResultCode(GlobalCacheManagerServer.NormalCloseUnbindFail);
                    return 0;
                }
                // TODO 确认Walk中删除记录是否有问题。
                var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(session.ServerId);
                SenderAcquired.Walk((key, value) =>
                {
                    Release(session, key, false);
                    return true; // continue walk
                });
                rpc.SendResultCode(0);
                logger.Debug("After NormalClose global.");
                return 0;
            }
        }

        protected override long ProcessCleanupRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as Cleanup;

            // 安全性以后加强。
            if (false == rpc.Argument.SecureKey.Equals("Ok! verify secure."))
            {
                rpc.SendResultCode(GlobalCacheManagerServer.CleanupErrorSecureKey);
                return 0;
            }

            var session = Sessions.GetOrAdd(rpc.Argument.ServerId, (key) => new CacheHolder() { GlobalInstance = this });
            if (session.GlobalCacheManagerHashIndex != rpc.Argument.GlobalCacheManagerHashIndex)
            {
                // 多点验证
                rpc.SendResultCode(GlobalCacheManagerServer.CleanupErrorGlobalCacheManagerHashIndex);
                return 0;
            }

            if (Rocks.Raft.Server.GetSocket(session.SessionId) != null)
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
                    var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(session.ServerId);
                    SenderAcquired.Walk((key, value) =>
                    {
                        Release(session, key, false);
                        return true; // continue release;
                    });
                    rpc.SendResultCode(0);
                },
                5 * 60 * 1000); // delay 5 mins

            return 0;
        }

        protected override long ProcessKeepAliveRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as KeepAlive;
            rpc.SendResultCode(Zeze.Transaction.Procedure.NotImplement);
            return 0;
        }

        private Rocks Rocks { get; }
        private readonly Util.AtomicLong SerialIdGenerator = new Util.AtomicLong();
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        private readonly Locks Locks = new Locks();

        /// <summary>
        /// 全局记录分配状态。
        /// </summary>
        private readonly Table<GlobalTableKey, CacheState> GlobalStates;

        /// <summary>
        /// 每个服务器已分配记录。
        /// 这是个Table模板，使用的时候根据ServerId打开真正的存储表。
        /// </summary>
        private readonly TableTemplate<GlobalTableKey, AcquiredState> ServerAcquiredTemplate;

        /*
         * 会话。
         * key是 LogicServer.Id，现在的实现就是Zeze.Config.ServerId。
         * 在连接建立后收到的Login Or ReLogin 中设置。
         * 每个会话还需要记录该会话的Socket.SessionId。在连接重新建立时更新。
         * 总是GetOrAdd，不删除。按现在的cache-sync设计，
         * ServerId 是及其有限的。不会一直增长。
         * 简化实现。
         */
        private readonly ConcurrentDictionary<int, CacheHolder> Sessions = new ConcurrentDictionary<int, CacheHolder>();

        public GlobalCacheManagerWithRaft(string raftName)
        {
            Rocks = new Rocks(raftName);

            RegisterRocksTables(Rocks);
            RegisterProtocols(Rocks.Raft.Server);

            GlobalStates = Rocks.GetTableTemplate("Global").OpenTable<GlobalTableKey, CacheState>(0);
            ServerAcquiredTemplate = Rocks.GetTableTemplate("Acquired") as TableTemplate<GlobalTableKey, AcquiredState>;

            Rocks.Raft.Server.Start();
        }

        public sealed class CacheHolder
        {
            public long SessionId { get; private set; }
            public int GlobalCacheManagerHashIndex { get; private set; }
            public int ServerId { get; internal set; }
            public GlobalCacheManagerWithRaft GlobalInstance { get; set; }

            public bool TryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex)
            {
                if (newSocket.UserState != null)
                    return false; // 不允许再次绑定。Login Or ReLogin 只能发一次。

                var socket = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
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

            public bool TryUnBindSocket(AsyncSocket oldSocket)
            {
                // 这里检查比较严格，但是这些检查应该都不会出现。

                if (oldSocket.UserState != this)
                    return false; // not bind to this

                var socket = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
                if (socket != oldSocket)
                    return false; // not same socket

                SessionId = 0;
                return true;
            }

            public static Reduce Reduce(ConcurrentDictionary<int, CacheHolder> sessions, int serverId,
                GlobalTableKey gkey, int state, long globalSerialId)
            {
                if (sessions.TryGetValue(serverId, out var session))
                    return session.Reduce(gkey, state, globalSerialId);

                var reduce = new Reduce();
                reduce.Argument.GlobalTableKey = gkey;
                reduce.Argument.State = state;
                reduce.Argument.GlobalSerialId = globalSerialId;
                reduce.Result.State = GlobalCacheManagerServer.StateReduceSessionNotFound;

                return reduce;
            }

            public static Reduce ReduceWaitLater(ConcurrentDictionary<int, CacheHolder> sessions,
                int serverId, out CacheHolder session,
                GlobalTableKey gkey, int state, long globalSerialId)
            {
                if (sessions.TryGetValue(serverId, out session))
                    return session.ReduceWaitLater(gkey, state, globalSerialId);

                return null;
            }

            public Reduce Reduce(GlobalTableKey gkey, int state, long globalSerialId)
            {
                Reduce reduce = ReduceWaitLater(gkey, state, globalSerialId);
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
                catch (RpcTimeoutException timeoutex)
                {
                    // 等待超时，应该报告错误。
                    logger.Error(timeoutex, "Reduce RpcTimeoutException {0} target={1} '{2}'", state, SessionId, gkey);
                    reduce.Result.State = GlobalCacheManagerServer.StateReduceRpcTimeout;
                    return reduce;
                }
                catch (Exception ex)
                {
                    logger.Error(ex, "Reduce Exception {0} target={1} '{2}'", state, SessionId, gkey);
                    reduce.Result.State = GlobalCacheManagerServer.StateReduceException;
                    return reduce;
                }
            }

            public const long ForbidPeriod = 10 * 1000; // 10 seconds
            private long LastErrorTime = 0;

            public void SetError()
            {
                lock (this)
                {
                    long now = Util.Time.NowUnixMillis;
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
            public Reduce ReduceWaitLater(GlobalTableKey gkey, int state, long globalSerialId)
            {
                try
                {
                    lock (this)
                    {
                        if (Util.Time.NowUnixMillis - LastErrorTime < ForbidPeriod)
                            return null;
                    }
                    AsyncSocket peer = GlobalCacheManagerServer.Instance.Server.GetSocket(SessionId);
                    if (null != peer)
                    {
                        var reduce = new Reduce();
                        reduce.Argument.GlobalTableKey = gkey;
                        reduce.Argument.State = state;
                        reduce.Argument.GlobalSerialId = globalSerialId;
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
    }
}
