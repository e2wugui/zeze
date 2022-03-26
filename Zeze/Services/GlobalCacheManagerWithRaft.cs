
using System;
using Zeze.Raft.RocksRaft;
using Zeze.Beans.GlobalCacheManagerWithRaft;
using System.Collections.Generic;
using System.Collections.Concurrent;
using Zeze.Net;
using System.Threading.Tasks;

namespace Zeze.Services
{
    public class GlobalCacheManagerWithRaft : AbstractGlobalCacheManagerWithRaft, IDisposable
    {
        public const int GlobalSerialIdAtomicLongIndex = 0;

        protected override async Task<long> ProcessAcquireRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as Acquire;
            rpc.Result.GlobalTableKey = rpc.Argument.GlobalTableKey;
            rpc.Result.State = rpc.Argument.State; // default success
            rpc.ResultCode = 0;

            if (rpc.Sender.UserState == null)
            {
                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                // 没有登录重做。登录是Agent自动流程的一部分，应该稍后重试。
                rpc.SendResultCode(Zeze.Transaction.Procedure.RaftRetry);
                return 0;
            }

            new Procedure(Rocks,
                () =>
                {
                    switch (rpc.Argument.State)
                    {
                        case GlobalCacheManagerServer.StateInvalid: // realease
                            rpc.Result.State = ReleasePrivate(rpc.Sender.UserState as CacheHolder, rpc.Argument.GlobalTableKey, true);
                            return 0;

                        case GlobalCacheManagerServer.StateShare:
                            return AcquireShare(rpc);

                        case GlobalCacheManagerServer.StateModify:
                            return AcquireModify(rpc);

                        default:
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            return GlobalCacheManagerServer.AcquireErrorState;
                    }
                })
            {
                // 启用自动发送rpc结果，但不做唯一检查。
                AutoResponse = rpc,
            }
            .Call();

            return 0; // has handle all error.
        }

        private long AcquireShare(Acquire rpc)
        {
            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;

            while (true)
            {
                var lockey = Zeze.Raft.RocksRaft.Transaction.Current.AddPessimismLock(Locks.Get(rpc.Argument.GlobalTableKey));

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
                cs.GlobalSerialId = Rocks.AtomicLongIncrementAndGet(GlobalSerialIdAtomicLongIndex);
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

                    int reduceResultState = GlobalCacheManagerServer.StateReduceNetError; // 默认网络错误。
                    if (CacheHolder.Reduce(Sessions, cs.Modify,
                        rpc.Argument.GlobalTableKey, GlobalCacheManagerServer.StateInvalid, cs.GlobalSerialId,
                        async (p) =>
                        {
                            var r = p as Reduce;
                            reduceResultState = r.IsTimeout ? GlobalCacheManagerServer.StateReduceRpcTimeout : r.Result.State;
                            lockey.Enter();
                            try
                            {
                                lockey.PulseAll();
                            }
                            finally
                            {
                                lockey.Exit();
                            }
                            return 0;
                        }))
                    {
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        lockey.Wait();
                    }

                    var ModifyAcquired = ServerAcquiredTemplate.OpenTableWithType(cs.Modify);
                    switch (reduceResultState)
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

                            logger.Error("XXX 8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                            lockey.PulseAll();
                            return GlobalCacheManagerServer.AcquireShareFailed;
                    }

                    cs.Modify = -1;
                    SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                    cs.Share.Add(sender.ServerId);
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                    lockey.PulseAll();
                    logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                    return 0; // 成功也会自动发送结果.
                }

                SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                cs.Share.Add(sender.ServerId);
                cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                lockey.PulseAll();
                logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                return 0; // 成功也会自动发送结果.
            }
        }

        private long AcquireModify(Acquire rpc)
        {
            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;

            while (true)
            {
                var lockey = Zeze.Raft.RocksRaft.Transaction.Current.AddPessimismLock(Locks.Get(rpc.Argument.GlobalTableKey));

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
                cs.GlobalSerialId = Rocks.AtomicLongIncrementAndGet(GlobalSerialIdAtomicLongIndex);
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
                        lockey.PulseAll();
                        return GlobalCacheManagerServer.AcquireModifyAlreadyIsModify;
                    }

                    int reduceResultState = GlobalCacheManagerServer.StateReduceNetError; // 默认网络错误。
                    if (CacheHolder.Reduce(Sessions, cs.Modify,
                        rpc.Argument.GlobalTableKey, GlobalCacheManagerServer.StateInvalid, cs.GlobalSerialId,
                        async (p) =>
                        {
                            var r = p as Reduce;
                            reduceResultState = r.IsTimeout ? GlobalCacheManagerServer.StateReduceRpcTimeout : r.Result.State;
                            lockey.Enter();
                            try
                            {
                                lockey.PulseAll();
                            }
                            finally
                            {
                                lockey.Exit();
                            }
                            return 0;
                        }))
                    {
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        lockey.Wait();
                    }

                    var ModifyAcquired = ServerAcquiredTemplate.OpenTableWithType(cs.Modify);
                    switch (reduceResultState)
                    {
                        case GlobalCacheManagerServer.StateInvalid:
                            ModifyAcquired.Remove(rpc.Argument.GlobalTableKey);
                            break; // reduce success

                        default:
                            // case StateReduceRpcTimeout:
                            // case StateReduceException:
                            // case StateReduceNetError:
                            cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                            lockey.PulseAll();

                            logger.Error("XXX 9 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                            return GlobalCacheManagerServer.AcquireModifyFailed;
                    }

                    cs.Modify = sender.ServerId;
                    cs.Share.Remove(sender.ServerId);
                    SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                    lockey.PulseAll();

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
                    Task.Run(
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
                            lockey.PulseAll();
                        }
                        finally
                        {
                            lockey.Exit();
                        }
                    });
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    lockey.Wait();
                }

                // 移除成功的。
                foreach (CacheHolder succeed in reduceSucceed)
                {
                    if (succeed.ServerId != sender.ServerId)
                    {
                        // sender 不移除：
                        // 1. 如果申请成功，后面会更新到Modify状态。
                        // 2. 如果申请不成功，恢复 cs.Share，保持 Acquired 不变。
                        var KeyAcquired = ServerAcquiredTemplate.OpenTableWithType(succeed.ServerId);
                        KeyAcquired.Remove(rpc.Argument.GlobalTableKey);
                    }
                    cs.Share.Remove(succeed.ServerId);
                }

                // 如果前面降级发生中断(break)，这里就不会为0。
                if (cs.Share.Count == 0)
                {
                    cs.Modify = sender.ServerId;
                    SenderAcquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                    lockey.PulseAll();

                    logger.Debug("8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                    return 0;
                }

                // senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
                // 失败了，要把原来是share的sender恢复。先这样吧。
                if (senderIsShare)
                    cs.Share.Add(sender.ServerId);

                cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                lockey.PulseAll();

                logger.Error("XXX 10 {0} {1} {2}", sender, rpc.Argument.State, cs);

                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                return GlobalCacheManagerServer.AcquireModifyFailed;
            }
        }

        private int Release(CacheHolder sender, GlobalTableKey gkey, bool noWait)
        {
            int result = 0;
            Rocks.NewProcedure(() =>
            {
                result = ReleasePrivate(sender, gkey, noWait);
                return 0;
            }).Call();
            return result;
        }

        private int ReleasePrivate(CacheHolder sender, GlobalTableKey gkey, bool noWait)
        {
            while (true)
            {
                var lockey = Zeze.Raft.RocksRaft.Transaction.Current.AddPessimismLock(Locks.Get(gkey));

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
                logger.Debug($"Release {gkey.TableName}:{gkey.Key} {cs}");
                lockey.PulseAll();
                return GetSenderCacheState(cs, sender);
            }
        }

        private static int GetSenderCacheState(CacheState cs, CacheHolder sender)
        {
            if (cs.Modify == sender.ServerId)
                return GlobalCacheManagerServer.StateModify;
            if (cs.Share.Contains(sender.ServerId))
                return GlobalCacheManagerServer.StateShare;
            return GlobalCacheManagerServer.StateInvalid;
        }

        protected override async Task<long> ProcessLoginRequest(Zeze.Net.Protocol _p)
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
                logger.Info($"Login {Rocks.Raft.Name} {rpc.Sender}.");
                return 0;
            }
        }

        protected override async Task<long> ProcessReLoginRequest(Zeze.Net.Protocol _p)
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
                logger.Info($"ReLogin {Rocks.Raft.Name} {rpc.Sender}.");
                return 0;
            }
        }

        protected override async Task<long> ProcessNormalCloseRequest(Zeze.Net.Protocol _p)
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
                logger.Info($"NormalClose {Rocks.Raft.Name} {rpc.Sender}");
                return 0;
            }
        }

        protected override async Task<long> ProcessCleanupRequest(Zeze.Net.Protocol _p)
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
            Util.Scheduler.Schedule(
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

        protected override async Task<long> ProcessKeepAliveRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as KeepAlive;
            rpc.SendResultCode(Zeze.Transaction.Procedure.NotImplement);
            return 0;
        }

        private Rocks Rocks { get; }
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        private readonly Locks Locks = new();

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
        private readonly ConcurrentDictionary<int, CacheHolder> Sessions = new();

        public GlobalCacheManagerWithRaft(
            string raftName,
            Raft.RaftConfig raftconf = null,
            Config config = null,
            bool RocksDbWriteOptionSync = false)
        { 
            Rocks = new Rocks(raftName, RocksMode.Pessimism, raftconf, config, RocksDbWriteOptionSync);

            RegisterRocksTables(Rocks);
            RegisterProtocols(Rocks.Raft.Server);

            GlobalStates = Rocks.GetTableTemplate("Global").OpenTable<GlobalTableKey, CacheState>(0);
            ServerAcquiredTemplate = Rocks.GetTableTemplate("Session") as TableTemplate<GlobalTableKey, AcquiredState>;

            Rocks.Raft.Server.Start();
        }

        public void Dispose()
        {
            GC.SuppressFinalize(this);
            Rocks.Raft.Shutdown().Wait();
        }

        public sealed class CacheHolder
        {
            public long SessionId { get; private set; }
            public int GlobalCacheManagerHashIndex { get; private set; }
            public int ServerId { get; internal set; }
            public GlobalCacheManagerWithRaft GlobalInstance { get; set; }

            public bool TryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex)
            {
                if (newSocket.UserState != null && newSocket.UserState != this)
                    return false; // 允许重复login|relogin，但不允许切换ServerId。

                var socket = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
                if (null == socket || socket == newSocket)
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

            public static bool Reduce(ConcurrentDictionary<int, CacheHolder> sessions, int serverId,
                GlobalTableKey gkey, int state, long globalSerialId, Func<Protocol, Task<long>> response)
            { 
                if (sessions.TryGetValue(serverId, out var session))
                    return session.Reduce(gkey, state, globalSerialId, response);

                return false;
            }

            public static Reduce ReduceWaitLater(ConcurrentDictionary<int, CacheHolder> sessions,
                int serverId, out CacheHolder session,
                GlobalTableKey gkey, int state, long globalSerialId)
            {
                if (sessions.TryGetValue(serverId, out session))
                    return session.ReduceWaitLater(gkey, state, globalSerialId);

                return null;
            }

            public bool Reduce(GlobalTableKey gkey, int state, long globalSerialId, Func<Protocol, Task<long>> response)
            {
                try
                {
                    lock (this)
                    {
                        if (Util.Time.NowUnixMillis - LastErrorTime < ForbidPeriod)
                            return false;
                    }
                    AsyncSocket peer = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
                    if (null != peer)
                    {
                        var reduce = new Reduce();
                        reduce.Argument.GlobalTableKey = gkey;
                        reduce.Argument.State = state;
                        reduce.Argument.GlobalSerialId = globalSerialId;
                        if (reduce.Send(peer, response, 10000))
                            return true;
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
                    AsyncSocket peer = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
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

            public override string ToString()
            {
                return $"{SessionId}@{ServerId}";
            }
        }
    }
}
