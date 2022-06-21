
using System;
using Zeze.Raft.RocksRaft;
using Zeze.Builtin.GlobalCacheManagerWithRaft;
using System.Collections.Generic;
using System.Collections.Concurrent;
using Zeze.Net;
using System.Threading.Tasks;
using Zeze.Util;
using System.Threading;

namespace Zeze.Services
{
    public class GlobalCacheManagerWithRaft : AbstractGlobalCacheManagerWithRaft, IDisposable
    {
        public const int GlobalSerialIdAtomicLongIndex = 0;

        protected override async Task<long> ProcessAcquireRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as Acquire;
            rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
            rpc.Result.State = rpc.Argument.State; // default success

            if (rpc.Sender.UserState == null)
            {
                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                // 没有登录重做。登录是Agent自动流程的一部分，应该稍后重试。
                rpc.SendResultCode(Zeze.Transaction.Procedure.RaftRetry);
                return 0;
            }

            var session = (CacheHolder)rpc.Sender.UserState;
            session.SetActiveTime(Util.Time.NowUnixMillis);
            await new Procedure(Rocks,
                async () =>
                {
                    switch (rpc.Argument.State)
                    {
                        case GlobalCacheManagerServer.StateInvalid: // realease
                            rpc.Result.State = await ReleasePrivate(session, rpc.Argument.GlobalKey, true);
                            rpc.ResultCode = 0;
                            return 0;

                        case GlobalCacheManagerServer.StateShare:
                            return await AcquireShare(rpc);

                        case GlobalCacheManagerServer.StateModify:
                            return await AcquireModify(rpc);

                        default:
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            rpc.ResultCode = 0;
                            return GlobalCacheManagerServer.AcquireErrorState;
                    }
                })
            {
                // 启用自动发送rpc结果，但不做唯一检查。
                AutoResponse = rpc,
            }
            .CallAsync();

            return 0; // has handle all error.
        }

        private bool GlobalLruTryRemoveCallback(Binary key, Record<Binary, CacheState> r)
        {
            using (var lockey = Locks.Get(key)) // 下面的TryEnter才可能加锁，如果没有加锁，这里的using也是安全的。
            {
                if (false == lockey.TryEnter())
                    return false;
                // 这里不需要设置成StateRemoved。
                // StateRemoved状态表示记录被删除了，而不是被从Cache中清除。
                // AcquireStatePending是瞬时数据（不会被持久化）。
                // 记录从Cache中清除后，可以再次从RocksDb中装载。
                var cs = (CacheState)r.Value;
                if (cs == null || cs.AcquireStatePending == GlobalCacheManagerServer.StateInvalid)
                    return GlobalStates.LruCache.TryRemove(key, out _);
                return false;
            }
        }

        private async Task<long> AcquireShare(Acquire rpc)
        {
            var fresh = rpc.ResultCode;
            rpc.ResultCode = 0;

            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;

            while (true)
            {
                var lockey = await Zeze.Raft.RocksRaft.Transaction.Current.AddPessimismLockAsync(Locks.Get(rpc.Argument.GlobalKey));

                CacheState cs = await GlobalStates.GetOrAddAsync(rpc.Argument.GlobalKey);
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
                                return GlobalCacheManagerServer.AcquireShareDeadLockFound; // 事务数据没有改变，回滚
                            }
                            break;

                        case GlobalCacheManagerServer.StateModify:
                            if (cs.Modify == sender.ServerId || cs.Share.Contains(sender.ServerId))
                            {
                                logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                return GlobalCacheManagerServer.AcquireShareDeadLockFound; // 事务数据没有改变，回滚
                            }
                            break;

                        case GlobalCacheManagerServer.StateRemoving:
                            break;
                    }
                    logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    await lockey.WaitAsync();
                    if (cs.Modify != -1 && cs.Share.Count > 0)
                        throw new Exception("CacheState state error");
                }

                if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                    continue; // concurrent release.

                cs.AcquireStatePending = GlobalCacheManagerServer.StateShare;
                //Rocks.AtomicLongIncrementAndGet(GlobalSerialIdAtomicLongIndex);
                SerialId.IncrementAndGet();
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
                        await SenderAcquired.PutAsync(rpc.Argument.GlobalKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                        rpc.ResultCode = GlobalCacheManagerServer.AcquireShareAlreadyIsModify;
                        return 0; // 可以忽略的错误，数据有改变，需要提交事务。
                    }

                    int reduceResultState = GlobalCacheManagerServer.StateReduceNetError; // 默认网络错误。
                    if (CacheHolder.Reduce(Sessions, cs.Modify,
                        rpc.Argument.GlobalKey, GlobalCacheManagerServer.StateInvalid, fresh,
                        async (p) =>
                        {
                            var r = p as Reduce;
                            reduceResultState = r.IsTimeout ? GlobalCacheManagerServer.StateReduceRpcTimeout : r.Result.State;
                            using var lockey2 = await Locks.Get(r.Argument.GlobalKey).EnterAsync();
                            lockey2.PulseAll();
                            return 0;
                        }))
                    {
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        await lockey.WaitAsync();
                    }

                    var ModifyAcquired = ServerAcquiredTemplate.OpenTableWithType(cs.Modify);
                    switch (reduceResultState)
                    {
                        case GlobalCacheManagerServer.StateShare:
                            await ModifyAcquired.PutAsync(rpc.Argument.GlobalKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                            cs.Share.Add(cs.Modify); // 降级成功。
                            break;

                        case GlobalCacheManagerServer.StateInvalid:
                            // 降到了 Invalid，此时就不需要加入 Share 了。
                            await ModifyAcquired.RemoveAsync(rpc.Argument.GlobalKey);
                            break;

                        case GlobalCacheManagerServer.StateReduceErrorFreshAcquire:
                            cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;

                            logger.Error("XXX fresh {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            lockey.PulseAll();
                            return GlobalCacheManagerServer.StateReduceErrorFreshAcquire; // 事务数据没有改变，回滚

                        default:
                            // 包含协议返回错误的值的情况。
                            // case StateReduceRpcTimeout:
                            // case StateReduceException:
                            // case StateReduceNetError:
                            cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;

                            logger.Error("XXX 8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            lockey.PulseAll();
                            return GlobalCacheManagerServer.AcquireShareFailed; // 事务数据没有改变，回滚
                    }

                    cs.Modify = -1;
                    await SenderAcquired.PutAsync(rpc.Argument.GlobalKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                    cs.Share.Add(sender.ServerId);
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                    lockey.PulseAll();
                    logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    return 0; // 成功也会自动发送结果.
                }

                await SenderAcquired.PutAsync(rpc.Argument.GlobalKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                cs.Share.Add(sender.ServerId);
                cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                lockey.PulseAll();
                logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                return 0; // 成功也会自动发送结果.
            }
        }

        private async Task<long> AcquireModify(Acquire rpc)
        {
            var fresh = rpc.ResultCode;
            rpc.ResultCode = 0;

            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;

            while (true)
            {
                var lockey = await Zeze.Raft.RocksRaft.Transaction.Current.AddPessimismLockAsync(Locks.Get(rpc.Argument.GlobalKey));

                CacheState cs = await GlobalStates.GetOrAddAsync(rpc.Argument.GlobalKey);
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
                                return GlobalCacheManagerServer.AcquireModifyDeadLockFound; // 事务数据没有改变，回滚
                            }
                            break;
                        case GlobalCacheManagerServer.StateModify:
                            if (cs.Modify == sender.ServerId || cs.Share.Contains(sender.ServerId))
                            {
                                logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                return GlobalCacheManagerServer.AcquireModifyDeadLockFound; // 事务数据没有改变，回滚
                            }
                            break;
                        case GlobalCacheManagerServer.StateRemoving:
                            break;
                    }
                    logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    await lockey.WaitAsync();

                    if (cs.Modify != -1 && cs.Share.Count > 0)
                        throw new Exception("CacheState state error");
                }
                if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                    continue; // concurrent release

                cs.AcquireStatePending = GlobalCacheManagerServer.StateModify;
                //Rocks.AtomicLongIncrementAndGet(GlobalSerialIdAtomicLongIndex);
                SerialId.IncrementAndGet();
                var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(sender.ServerId);
                if (cs.Modify != -1)
                {
                    if (cs.Modify == sender.ServerId)
                    {
                        logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        // 已经是Modify又申请，可能是sender异常关闭，又重启连上。
                        // 更新一下。应该是不需要的。
                        await SenderAcquired.PutAsync(rpc.Argument.GlobalKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                        cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                        lockey.PulseAll();
                        rpc.ResultCode = GlobalCacheManagerServer.AcquireModifyAlreadyIsModify;
                        return 0; // 可以忽略的错误，数据有改变，需要提交事务。
                    }

                    int reduceResultState = GlobalCacheManagerServer.StateReduceNetError; // 默认网络错误。
                    if (CacheHolder.Reduce(Sessions, cs.Modify,
                        rpc.Argument.GlobalKey, GlobalCacheManagerServer.StateInvalid, fresh,
                        async (p) =>
                        {
                            var r = p as Reduce;
                            reduceResultState = r.IsTimeout ? GlobalCacheManagerServer.StateReduceRpcTimeout : r.Result.State;
                            using var lockey2 = await Locks.Get(r.Argument.GlobalKey).EnterAsync();
                            lockey2.PulseAll();
                            return 0;
                        }))
                    {
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        await lockey.WaitAsync();
                    }

                    var ModifyAcquired = ServerAcquiredTemplate.OpenTableWithType(cs.Modify);
                    switch (reduceResultState)
                    {
                        case GlobalCacheManagerServer.StateInvalid:
                            await ModifyAcquired.RemoveAsync(rpc.Argument.GlobalKey);
                            break; // reduce success

                        case GlobalCacheManagerServer.StateReduceErrorFreshAcquire:
                            cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;

                            logger.Error("XXX fresh {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            lockey.PulseAll();
                            return GlobalCacheManagerServer.StateReduceErrorFreshAcquire; // 事务数据没有改变，回滚

                        default:
                            // case StateReduceRpcTimeout:
                            // case StateReduceException:
                            // case StateReduceNetError:
                            cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                            lockey.PulseAll();

                            logger.Error("XXX 9 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            return GlobalCacheManagerServer.AcquireModifyFailed; // 事务数据没有改变，回滚
                    }

                    cs.Modify = sender.ServerId;
                    cs.Share.Remove(sender.ServerId);
                    await SenderAcquired.PutAsync(rpc.Argument.GlobalKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                    lockey.PulseAll();

                    logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
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
                        rpc.Argument.GlobalKey, GlobalCacheManagerServer.StateInvalid, fresh);
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
                var freshReduce = false;
                if (!(cs.Share.Count == 0) && (!senderIsShare || reducePending.Count > 0))
                {
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
                                    case GlobalCacheManagerServer.StateInvalid:
                                        reduceSucceed.Add(reduce.Key);
                                        break;
                                    case GlobalCacheManagerServer.StateReduceErrorFreshAcquire:
                                        freshReduce = true;
                                        break;
                                    default:
                                        reduce.Key.SetError();
                                        break;
                                }
                            }
                            catch (Exception ex)
                            {
                                reduce.Key.SetError();
                                // 等待失败不再看作成功。
                                logger.Error(ex, "Reduce {0} {1} {2} {3}", sender, rpc.Argument.State, cs, reduce.Value.Argument);
                            }
                        }
                        using var lockey2 = await Locks.Get(rpc.Argument.GlobalKey).EnterAsync();
                        lockey2.PulseAll();
                    });
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    await lockey.WaitAsync();
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
                        await KeyAcquired.RemoveAsync(rpc.Argument.GlobalKey);
                    }
                    cs.Share.Remove(succeed.ServerId);
                }

                // 如果前面降级发生中断(break)，这里就不会为0。
                if (cs.Share.Count == 0)
                {
                    cs.Modify = sender.ServerId;
                    await SenderAcquired.PutAsync(rpc.Argument.GlobalKey, new AcquiredState()
                    {
                        State = GlobalCacheManagerServer.StateModify
                    });
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                    lockey.PulseAll();

                    logger.Debug("8 {0} {1} {2}", sender, rpc.Argument.State, cs);
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
                rpc.ResultCode = freshReduce
                    ? GlobalCacheManagerServer.StateReduceErrorFreshAcquire // 这个错误码导致Server-RedoAndReleaseLock
                    : GlobalCacheManagerServer.AcquireModifyFailed; // 这个错误码导致Server事务失败。
                return 0; // 可能存在部分reduce成功，需要提交事务。
            }
        }

        private async Task<int> Release(CacheHolder sender, Binary gkey, bool noWait)
        {
            int result = 0;
            await Rocks.NewProcedure(async () =>
            {
                result = await ReleasePrivate(sender, gkey, noWait);
                return 0;
            }).CallAsync();
            return result;
        }

        private async Task<int> ReleasePrivate(CacheHolder sender, Binary gkey, bool noWait)
        {
            while (true)
            {
                var lockey = await Zeze.Raft.RocksRaft.Transaction.Current.AddPessimismLockAsync(Locks.Get(gkey));

                CacheState cs = await GlobalStates.GetOrAddAsync(gkey);
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
                    await lockey.WaitAsync();
                }
                if (cs.AcquireStatePending == GlobalCacheManagerServer.StateRemoved)
                {
                    continue;
                }
                cs.AcquireStatePending = GlobalCacheManagerServer.StateRemoving;

                if (cs.Modify == sender.ServerId)
                    cs.Modify = -1;
                cs.Share.Remove(sender.ServerId); // always try remove

                if (cs.Modify == -1 && cs.Share.Count == 0)
                {
                    // 安全的从global中删除，没有并发问题。
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateRemoved;
                    await GlobalStates.RemoveAsync(gkey);
                }
                else
                {
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                }
                var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(sender.ServerId);
                await SenderAcquired.RemoveAsync(gkey);
                logger.Debug($"Release {gkey} {cs}");
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

            // 同一个节点互斥。不同节点Bind不需要互斥，Release由Raft-Leader唯一性提供保护。
            if (false == await session.TryBindSocket(rpc.Sender, rpc.Argument.GlobalCacheManagerHashIndex))
            {
                rpc.SendResultCode(GlobalCacheManagerServer.LoginBindSocketFail);
                return 0;
            }
            session.SetActiveTime(Util.Time.NowUnixMillis);
            // new login, 比如逻辑服务器重启。release old acquired.
            var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(session.ServerId);
            await SenderAcquired.WalkKeyAsync(async (key) =>
            {
                await Release(session, key, false);
                return true; // continue walk
            });
            rpc.Result.MaxNetPing = Config.MaxNetPing;
            rpc.Result.ServerProcessTime = Config.ServerProcessTime;
            rpc.Result.ServerReleaseTimeout = Config.ServerReleaseTimeout;
            rpc.SendResultCode(0);
            logger.Info($"Login {Rocks.Raft.Name} {rpc.Sender}.");
            return 0;
        }

        protected override async Task<long> ProcessReLoginRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as ReLogin;
            var session = Sessions.GetOrAdd(rpc.Argument.ServerId, 
                (key) => new CacheHolder() { GlobalInstance = this, ServerId = rpc.Argument.ServerId });

            if (false == await session.TryBindSocket(rpc.Sender, rpc.Argument.GlobalCacheManagerHashIndex))
            {
                rpc.SendResultCode(GlobalCacheManagerServer.ReLoginBindSocketFail);
                return 0;
            }
            session.SetActiveTime(Util.Time.NowUnixMillis);
            rpc.SendResultCode(0);
            logger.Info($"ReLogin {Rocks.Raft.Name} {rpc.Sender}.");
            return 0;
        }

        protected override async Task<long> ProcessNormalCloseRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as NormalClose;
            if (rpc.Sender.UserState is not CacheHolder session)
            {
                rpc.SendResultCode(GlobalCacheManagerServer.AcquireNotLogin);
                return 0; // not login
            }

            // 同一个节点互斥。不同节点Bind不需要互斥，Release由Raft-Leader唯一性提供保护。
            if (false == await session.TryUnBindSocket(rpc.Sender))
            {
                rpc.SendResultCode(GlobalCacheManagerServer.NormalCloseUnbindFail);
                return 0;
            }
            // TODO 确认Walk中删除记录是否有问题。
            var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(session.ServerId);
            await SenderAcquired.WalkKeyAsync(async (key) =>
            {
                await Release(session, key, false);
                return true; // continue walk
            });
            rpc.SendResultCode(0);
            logger.Info($"NormalClose {Rocks.Raft.Name} {rpc.Sender}");
            return 0;
        }

        protected override async Task<long> ProcessCleanupRequest(Zeze.Net.Protocol _p)
        {
            if (AchillesHeelConfig != null) // disable cleanup.
                return 0;

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
            await Task.Delay(5 * 60 * 1000); // delay 5 mins
            var SenderAcquired = ServerAcquiredTemplate.OpenTableWithType(session.ServerId);
            await SenderAcquired.WalkKeyAsync(async (key) =>
            {
                await Release(session, key, false);
                return true; // continue release;
            });
            rpc.SendResultCode(0);
            return 0;
        }

        protected override Task<long> ProcessKeepAliveRequest(Zeze.Net.Protocol p)
        {
            var session = (CacheHolder)p.UserState;
            if (null == session)
            {
                p.SendResultCode(GlobalCacheManagerServer.AcquireNotLogin);
                return Task.FromResult(0L);
            }
            session.SetActiveTime(Util.Time.NowUnixMillis);
            p.SendResultCode(0);
            return Task.FromResult(0L);
        }

        private Rocks Rocks { get; set; }
        private AtomicLong SerialId { get; } = new();
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        private readonly Locks Locks = new();

        /// <summary>
        /// 全局记录分配状态。
        /// </summary>
        private Table<Binary, CacheState> GlobalStates;

        /// <summary>
        /// 每个服务器已分配记录。
        /// 这是个Table模板，使用的时候根据ServerId打开真正的存储表。
        /// </summary>
        private TableTemplate<Binary, AcquiredState> ServerAcquiredTemplate;

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
        // 外面主动提供装载配置，需要在Load之前把这个实例注册进去。
        public GlobalCacheManagerServer.GCMConfig Config { get; } = new();
        private Config ZezeConfig;

        public bool WriteOptions { get; }

        public GlobalCacheManagerWithRaft(bool writeOptions = false)
        {
            WriteOptions = writeOptions;
        }

        public async Task<GlobalCacheManagerWithRaft> OpenAsync(
            string raftName, Raft.RaftConfig raftconf = null, Config config = null)
        {
            lock (this)
            {
                if (Rocks != null)
                    throw new InvalidOperationException();
                Rocks = new Rocks(RocksMode.Pessimism, WriteOptions);
            }

            ZezeConfig = config;
            if (ZezeConfig == null)
                ZezeConfig = new Zeze.Config().AddCustomize(Config).LoadAndParse();

            await Rocks.OpenAsync(raftName, raftconf, config);

            RegisterRocksTables(Rocks);
            RegisterProtocols(Rocks.Raft.Server);

            var globalTemplate = (TableTemplate<Binary, CacheState>)Rocks.GetTableTemplate("Global");
            globalTemplate.LruTryRemove = GlobalLruTryRemoveCallback;
            GlobalStates = globalTemplate.OpenTable<Binary, CacheState>(0);
            ServerAcquiredTemplate = Rocks.GetTableTemplate("Session") as TableTemplate<Binary, AcquiredState>;

            Rocks.Raft.Server.Start();

            // Global的守护不需要独立线程。当出现异常问题不能工作时，没有释放锁是不会造成致命问题的。
            AchillesHeelConfig = new AchillesHeelConfig(Config.MaxNetPing, Config.ServerProcessTime, Config.ServerReleaseTimeout);
            Scheduler.Schedule(AchillesHeelDaemon, 5000, 5000);
            return this;
        }

        private AchillesHeelConfig AchillesHeelConfig;

        private async Task AchillesHeelDaemon(SchedulerTask ThisTask)
        {
            var now = Util.Time.NowUnixMillis;
            if (Rocks.Raft.IsLeader)
            {
                foreach (var session in Sessions.Values)
                {
                    if (now - session.GetActiveTime() > AchillesHeelConfig.GlobalDaemonTimeout)
                    {
                        using (await session.Mutex.LockAsync())
                        {
                            session.Kick();
                            var Acquired = ServerAcquiredTemplate.OpenTableWithType(session.ServerId);
                            await Acquired.WalkKeyAsync(async (key) =>
                            {
                                // ConcurrentDictionary 可以在循环中删除。这样虽然效率低些，但是能处理更多情况。
                                if (Rocks.Raft.IsLeader)
                                {
                                    await Release(session, key, false);
                                    return true;
                                }
                                return false;
                            });
                            // skip allReleaseFuture result
                        }
                    }
                }
            }
        }

        public void Dispose()
        {
            GC.SuppressFinalize(this);
            Rocks.Raft.Shutdown().Wait();
        }

        public sealed class CacheHolder
        {
            private long ActiveTime = Time.NowUnixMillis;
            public long SessionId { get; private set; }
            public int GlobalCacheManagerHashIndex { get; private set; }
            public int ServerId { get; internal set; }
            public GlobalCacheManagerWithRaft GlobalInstance { get; set; }
            public Nito.AsyncEx.AsyncLock Mutex { get; } = new();

            public long GetActiveTime()
            {
                return Interlocked.Read(ref ActiveTime);
            }

            public void SetActiveTime(long value)
            {
                Interlocked.Exchange(ref ActiveTime, value);
            }

            // not under lock
            internal void Kick()
            {
                var peer = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
                if (null != peer)
                {
                    peer.UserState = null; // 来自这个Agent的所有请求都会失败。
                    peer.Close(null); // 关闭连接，强制Agent重新登录。
                }
                SessionId = 0; // 清除网络状态。
            }

            public async Task<bool> TryBindSocket(AsyncSocket newSocket, int _GlobalCacheManagerHashIndex)
            {
                using (await Mutex.LockAsync())
                {
                    if (newSocket.UserState != null && newSocket.UserState != this)
                        return false; // 允许重复login|relogin，但不允许切换ServerId。

                    var socket = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
                    if (null == socket || socket == newSocket)
                    {
                        // old socket not exist or has lost or is same.
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
                using (await Mutex.LockAsync())
                {
                    // 这里检查比较严格，但是这些检查应该都不会出现。

                    if (oldSocket.UserState != this)
                        return false; // not bind to this

                    var socket = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
                    if (socket != null && socket != oldSocket)
                        return false; // not same socket: socket is null 意味着当前绑定的已经关闭，此时也允许解除绑定。

                    SessionId = 0;
                    return true;
                }
            }

            public static bool Reduce(ConcurrentDictionary<int, CacheHolder> sessions, int serverId,
                Binary gkey, int state, long fresh, Func<Protocol, Task<long>> response)
            { 
                if (sessions.TryGetValue(serverId, out var session))
                    return session.Reduce(gkey, state, fresh, response);

                return false;
            }

            public static Reduce ReduceWaitLater(ConcurrentDictionary<int, CacheHolder> sessions,
                int serverId, out CacheHolder session,
                Binary gkey, int state, long fresh)
            {
                if (sessions.TryGetValue(serverId, out session))
                    return session.ReduceWaitLater(gkey, state, fresh);

                return null;
            }

            public bool Reduce(Binary gkey, int state, long fresh, Func<Protocol, Task<long>> response)
            {
                try
                {
                    lock (this)
                    {
                        if (Util.Time.NowUnixMillis - LastErrorTime < GlobalInstance.AchillesHeelConfig.GlobalForbidPeriod)
                            return false;
                    }
                    AsyncSocket peer = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
                    if (null != peer)
                    {
                        var reduce = new Reduce();
                        reduce.ResultCode = fresh;
                        reduce.Argument.GlobalKey = gkey;
                        reduce.Argument.State = state;
                        if (reduce.Send(peer, response, GlobalInstance.AchillesHeelConfig.ReduceTimeout))
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

            private long LastErrorTime = 0;

            public void SetError()
            {
                lock (this)
                {
                    long now = Util.Time.NowUnixMillis;
                    if (now - LastErrorTime > GlobalInstance.AchillesHeelConfig.GlobalForbidPeriod)
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
                        if (Util.Time.NowUnixMillis - LastErrorTime < GlobalInstance.AchillesHeelConfig.GlobalForbidPeriod)
                            return null;
                    }
                    AsyncSocket peer = GlobalInstance.Rocks.Raft.Server.GetSocket(SessionId);
                    if (null != peer)
                    {
                        var reduce = new Reduce();
                        reduce.ResultCode = fresh;
                        reduce.Argument.GlobalKey = gkey;
                        reduce.Argument.State = state;
                        _ = reduce.SendAsync(peer, GlobalInstance.AchillesHeelConfig.ReduceTimeout);
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
