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
using Zeze.Raft.StateMachines;

namespace Zeze.Services
{
    public sealed class GlobalCacheManagerWithRaft
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public static GlobalCacheManagerWithRaft Instance { get; } = new GlobalCacheManagerWithRaft();
        public Zeze.Raft.Raft Raft { get; private set; }
        public GlobalCacheManagerStateMachine StateMachine { get; private set; }

        private GlobalCacheManagerWithRaft()
        { 
        }

        public void Start(Zeze.Raft.RaftConfig raftconfig, Config config = null)
        {
            lock (this)
            {
                if (Raft != null)
                    return;

                if (null == config)
                    config = Config.Load();

                StateMachine = new GlobalCacheManagerStateMachine();
                Raft = new Zeze.Raft.Raft(StateMachine, raftconfig, config);

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

                Raft.Server.Start();
            }
        }

        public void Stop()
        {
            lock (this)
            {
                if (null == Raft)
                    return;
                Raft.Server.Close();
                Raft = null;
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
            var rpc = p as GlobalCacheManager.Cleanup;

            // 安全性以后加强。
            if (false == rpc.Argument.SecureKey.Equals("Ok! verify secure."))
            {
                rpc.SendResultCode(-1);
                return 0;
            }

            var session = StateMachine.Sessions.GetOrAdd(rpc.Argument.AutoKeyLocalId);
            if (session.GlobalCacheManagerHashIndex != rpc.Argument.GlobalCacheManagerHashIndex)
            {
                // 多点验证
                rpc.SendResultCode(-2);
                return 0;
            }

            if (this.Raft.Server.GetSocket(session.SessionId) != null)
            {
                // 连接存在，禁止cleanup。
                rpc.SendResultCode(-3);
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
                        Release(session, gkey);
                    }
                    rpc.SendResultCode(0);
                },
                5 * 60 * 1000); // delay 5 mins

            return 0;
        }

        private int ProcessLogin(Zeze.Net.Protocol p)
        {
            var rpc = p as GlobalCacheManager.Login;
            var session = StateMachine.Sessions.GetOrAdd(rpc.Argument.AutoKeyLocalId);
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
            var rpc = p as GlobalCacheManager.ReLogin;
            var session = StateMachine.Sessions.GetOrAdd(rpc.Argument.AutoKeyLocalId);
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
            var rpc = p as GlobalCacheManager.NormalClose;
            var session = rpc.Sender.UserState as GlobalCacheManagerStateMachine.CacheHolder;
            if (null == session)
            {
                rpc.SendResultCode(GlobalCacheManager.AcquireNotLogin);
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
            logger.Debug("After NormalClose global.Count={0}", StateMachine.Global.Count);
            return 0;
        }

        private int ProcessAcquireRequest(Zeze.Net.Protocol p)
        {
            var rpc = p as GlobalCacheManager.Acquire;
            if (rpc.Sender.UserState == null)
            {
                rpc.SendResultCode(GlobalCacheManager.AcquireNotLogin);
                return 0;
            }
            switch (rpc.Argument.State)
            {
                case GlobalCacheManager.StateInvalid: // realease
                    var session = rpc.Sender.UserState as GlobalCacheManagerStateMachine.CacheHolder;
                    Release(session, rpc.Argument.GlobalTableKey);
                    rpc.Result = rpc.Argument;
                    rpc.SendResult();
                    return 0;

                case GlobalCacheManager.StateShare:
                    return AcquireShare(rpc);

                case GlobalCacheManager.StateModify:
                    return AcquireModify(rpc);

                default:
                    rpc.Result = rpc.Argument;
                    rpc.SendResultCode(GlobalCacheManager.AcquireErrorState);
                    return 0;
            }
        }

        private void Release(
            GlobalCacheManagerStateMachine.CacheHolder holder,
            GlobalCacheManager.GlobalTableKey gkey)
        {
            var cs = StateMachine.Global.GetOrAdd(gkey);
            lock (cs)
            {
                var log = new GlobalCacheManagerStateMachine.OperatesLog();
                if (cs.Modify == holder.Id)
                {
                    log.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateModify()
                    {
                        Key = gkey,
                        Modify = -1
                    });
                }
                // always try remove
                log.Operates.Add(new GlobalCacheManagerStateMachine.RemoveCacheStateShare()
                {
                    Key = gkey,
                    Share = holder.Id
                });
                Raft.AppendLog(log);

                var log2 = new GlobalCacheManagerStateMachine.OperatesLog();
                if (cs.Modify == -1
                    && cs.Share.Count == 0
                    && cs.AcquireStatePending == GlobalCacheManager.StateInvalid)
                {
                    // 安全的从global中删除，没有并发问题。
                    log2.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateAcquireStatePending()
                    {
                        Key = gkey,
                        AcquireStatePending = GlobalCacheManager.StateRemoved
                    });
                    log2.Operates.Add(new GlobalCacheManagerStateMachine.RemoveCacheState()
                    {
                        Key = gkey
                    });
                }
                log2.Operates.Add(new GlobalCacheManagerStateMachine.RemoveCacheHolderAcquired() { Id = holder.Id, Key = gkey });
                Raft.AppendLog(log2);
            }
        }

        private int AcquireShare(GlobalCacheManager.Acquire rpc)
        {
            var sender = rpc.Sender.UserState as GlobalCacheManagerStateMachine.CacheHolder;
            rpc.Result = rpc.Argument;
            while (true)
            {
                var cs = StateMachine.Global.GetOrAdd(rpc.Argument.GlobalTableKey);
                lock (cs)
                {
                    if (cs.AcquireStatePending == GlobalCacheManager.StateRemoved)
                        continue;

                    while (cs.AcquireStatePending != GlobalCacheManager.StateInvalid)
                    {
                        switch (cs.AcquireStatePending)
                        {
                            case GlobalCacheManager.StateShare:
                                if (cs.Modify == sender.Id)
                                {
                                    logger.Debug("1 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = GlobalCacheManager.StateInvalid;
                                    rpc.SendResultCode(GlobalCacheManager.AcquireShareDeadLockFound);
                                    return 0;
                                }
                                break;
                            case GlobalCacheManager.StateModify:
                                if (cs.Modify == sender.Id || cs.Share.Contains(sender.Id))
                                {
                                    logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = GlobalCacheManager.StateInvalid;
                                    rpc.SendResultCode(GlobalCacheManager.AcquireShareDeadLockFound);
                                    return 0;
                                }
                                break;
                        }
                        logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);
                    }
                    var log0 = new GlobalCacheManagerStateMachine.OperatesLog();
                    log0.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateAcquireStatePending()
                    {
                        Key = rpc.Argument.GlobalTableKey,
                        AcquireStatePending = GlobalCacheManager.StateShare,
                    });
                    Raft.AppendLog(log0);
                    // cs.AcquireStatePending = GlobalCacheManager.StateShare;

                    if (cs.Modify != -1)
                    {
                        if (cs.Modify == sender.Id)
                        {
                            logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = GlobalCacheManager.StateModify;
                            // 已经是Modify又申请，可能是sender异常关闭，又重启连上。更新一下。应该是不需要的。
                            var log1 = new GlobalCacheManagerStateMachine.OperatesLog();
                            log1.Operates.Add(new GlobalCacheManagerStateMachine.PutCacheHolderAcquired()
                            {
                                Id = sender.Id,
                                Key = rpc.Argument.GlobalTableKey,
                                AcquireState = GlobalCacheManager.StateModify,
                            });
                            log1.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateAcquireStatePending()
                            {
                                Key = rpc.Argument.GlobalTableKey,
                                AcquireStatePending = GlobalCacheManager.StateInvalid,
                            });
                            // cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                            Raft.AppendLog(log1);
                            rpc.SendResultCode(GlobalCacheManager.AcquireShareAlreadyIsModify);
                            return 0;
                        }

                        int stateReduceResult = GlobalCacheManager.StateReduceException;
                        var modifyHolder = StateMachine.Sessions.GetOrAdd(cs.Modify);
                        Zeze.Util.Task.Run(
                            () =>
                            {
                                stateReduceResult = modifyHolder.Reduce(
                                    rpc.Argument.GlobalTableKey,
                                    GlobalCacheManager.StateShare);

                                lock (cs)
                                {
                                    Monitor.PulseAll(cs);
                                }
                            },
                            "GlobalCacheManager.AcquireShare.Reduce");
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);

                        var log2 = new GlobalCacheManagerStateMachine.OperatesLog();
                        switch (stateReduceResult)
                        {
                            case GlobalCacheManager.StateShare:
                                log2.Operates.Add(new GlobalCacheManagerStateMachine.PutCacheHolderAcquired()
                                {
                                    Id = cs.Modify,
                                    Key = rpc.Argument.GlobalTableKey,
                                    AcquireState = GlobalCacheManager.StateShare
                                });
                                //cs.Modify.Acquired[rpc.Argument.GlobalTableKey] = GlobalCacheManager.StateShare;
                                // 降级成功，有可能降到 Invalid，此时就不需要加入 Share 了。
                                log2.Operates.Add(new GlobalCacheManagerStateMachine.AddCacheStateShare()
                                {
                                    Key = rpc.Argument.GlobalTableKey,
                                    Share = cs.Modify
                                });
                                //cs.Share.Add(cs.Modify); 
                                break;

                            default:
                                // 包含协议返回错误的值的情况。
                                // case StateReduceRpcTimeout:
                                // case StateReduceException:
                                // case StateReduceNetError:
                                log2.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateAcquireStatePending()
                                {
                                    Key = rpc.Argument.GlobalTableKey,
                                    AcquireStatePending = GlobalCacheManager.StateInvalid,
                                });
                                //cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                                Raft.AppendLog(log2);

                                Monitor.Pulse(cs);
                                logger.Error("XXX 8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = GlobalCacheManager.StateInvalid;
                                rpc.SendResultCode(GlobalCacheManager.AcquireShareFaild);
                                return 0;
                        }

                        log2.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateModify()
                        {
                            Key = rpc.Argument.GlobalTableKey,
                            Modify = -1,
                        });
                        //cs.Modify = null;
                        log2.Operates.Add(new GlobalCacheManagerStateMachine.PutCacheHolderAcquired()
                        {
                            Id = sender.Id,
                            Key = rpc.Argument.GlobalTableKey,
                            AcquireState = GlobalCacheManager.StateShare,
                        });
                        //sender.Acquired[rpc.Argument.GlobalTableKey] = GlobalCacheManager.StateShare;
                        log2.Operates.Add(new GlobalCacheManagerStateMachine.AddCacheStateShare()
                        {
                            Key = rpc.Argument.GlobalTableKey,
                            Share = sender.Id,
                        });
                        //cs.Share.Add(sender);
                        log2.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateAcquireStatePending()
                        {
                            Key = rpc.Argument.GlobalTableKey,
                            AcquireStatePending = GlobalCacheManager.StateInvalid,
                        });
                        //cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                        Raft.AppendLog(log2);

                        Monitor.Pulse(cs);
                        logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.SendResult();
                        return 0;
                    }

                    var log3 = new GlobalCacheManagerStateMachine.OperatesLog();
                    log3.Operates.Add(new GlobalCacheManagerStateMachine.PutCacheHolderAcquired()
                    {
                        Id = sender.Id,
                        Key = rpc.Argument.GlobalTableKey,
                        AcquireState = GlobalCacheManager.StateShare,
                    });
                    //sender.Acquired[rpc.Argument.GlobalTableKey] = GlobalCacheManager.StateShare;
                    log3.Operates.Add(new GlobalCacheManagerStateMachine.AddCacheStateShare()
                    {
                        Key = rpc.Argument.GlobalTableKey,
                        Share = sender.Id,
                    });
                    //cs.Share.Add(sender);
                    log3.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateAcquireStatePending()
                    {
                        Key = rpc.Argument.GlobalTableKey,
                        AcquireStatePending = GlobalCacheManager.StateInvalid,
                    });
                    //cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    Raft.AppendLog(log3);
                    rpc.SendResult();
                    return 0;
                }
            }
        }

        private int AcquireModify(GlobalCacheManager.Acquire rpc)
        {
            var sender = rpc.Sender.UserState as GlobalCacheManagerStateMachine.CacheHolder;
            rpc.Result = rpc.Argument;

            while (true)
            {
                var cs = StateMachine.Global.GetOrAdd(rpc.Argument.GlobalTableKey);
                lock (cs)
                {
                    if (cs.AcquireStatePending == GlobalCacheManager.StateRemoved)
                        continue;

                    while (cs.AcquireStatePending != GlobalCacheManager.StateInvalid)
                    {
                        switch (cs.AcquireStatePending)
                        {
                            case GlobalCacheManager.StateShare:
                                if (cs.Modify == sender.Id)
                                {
                                    logger.Debug("1 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = GlobalCacheManager.StateInvalid;
                                    rpc.SendResultCode(GlobalCacheManager.AcquireModifyDeadLockFound);
                                    return 0;
                                }
                                break;
                            case GlobalCacheManager.StateModify:
                                if (cs.Modify == sender.Id || cs.Share.Contains(sender.Id))
                                {
                                    logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = GlobalCacheManager.StateInvalid;
                                    rpc.SendResultCode(GlobalCacheManager.AcquireModifyDeadLockFound);
                                    return 0;
                                }
                                break;
                        }
                        logger.Debug("3 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);
                    }
                    var log0 = new GlobalCacheManagerStateMachine.OperatesLog();
                    log0.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateAcquireStatePending()
                    {
                        Key = rpc.Argument.GlobalTableKey,
                        AcquireStatePending = GlobalCacheManager.StateModify,
                    });
                    //cs.AcquireStatePending = GlobalCacheManager.StateModify;
                    Raft.AppendLog(log0);

                    if (cs.Modify != -1)
                    {
                        if (cs.Modify == sender.Id)
                        {
                            logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            // 已经是Modify又申请，可能是sender异常关闭，又重启连上。更新一下。应该是不需要的。
                            var log1 = new GlobalCacheManagerStateMachine.OperatesLog();
                            log1.Operates.Add(new GlobalCacheManagerStateMachine.PutCacheHolderAcquired()
                            {
                                Id = sender.Id,
                                Key = rpc.Argument.GlobalTableKey,
                                AcquireState = GlobalCacheManager.StateModify,
                            });
                            //sender.Acquired[rpc.Argument.GlobalTableKey] = GlobalCacheManager.StateModify;
                            log1.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateAcquireStatePending()
                            {
                                Key = rpc.Argument.GlobalTableKey,
                                AcquireStatePending = GlobalCacheManager.StateInvalid,
                            });
                            //cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                            Raft.AppendLog(log1);
                            rpc.SendResultCode(GlobalCacheManager.AcquireModifyAlreadyIsModify);
                            return 0;
                        }

                        int stateReduceResult = GlobalCacheManager.StateReduceException;
                        var modifyHolder = StateMachine.Sessions.GetOrAdd(cs.Modify);
                        Zeze.Util.Task.Run(
                            () =>
                            {
                                stateReduceResult = modifyHolder.Reduce(
                                    rpc.Argument.GlobalTableKey,
                                    GlobalCacheManager.StateInvalid);
                                lock (cs)
                                {
                                    Monitor.PulseAll(cs);
                                }
                            },
                            "GlobalCacheManager.AcquireModify.Reduce");
                        logger.Debug("5 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        Monitor.Wait(cs);

                        var log2 = new GlobalCacheManagerStateMachine.OperatesLog();

                        switch (stateReduceResult)
                        {
                            case GlobalCacheManager.StateInvalid:
                                log2.Operates.Add(new GlobalCacheManagerStateMachine.RemoveCacheHolderAcquired()
                                {
                                    Id = cs.Modify,
                                    Key = rpc.Argument.GlobalTableKey,
                                });
                                //cs.Modify.Acquired.TryRemove(rpc.Argument.GlobalTableKey, out var _);
                                break; // reduce success

                            default:
                                // case StateReduceRpcTimeout:
                                // case StateReduceException:
                                // case StateReduceNetError:
                                log2.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateAcquireStatePending()
                                {
                                    Key = rpc.Argument.GlobalTableKey,
                                    AcquireStatePending = GlobalCacheManager.StateInvalid,
                                });
                                // cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                                Raft.AppendLog(log2);

                                Monitor.Pulse(cs);

                                logger.Error("XXX 9 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                rpc.Result.State = GlobalCacheManager.StateInvalid;
                                rpc.SendResultCode(GlobalCacheManager.AcquireModifyFaild);
                                return 0;
                        }

                        cs.Modify = sender.Id;

                        log2.Operates.Add(new GlobalCacheManagerStateMachine.PutCacheHolderAcquired()
                        {
                            Id = sender.Id,
                            Key = rpc.Argument.GlobalTableKey,
                            AcquireState = GlobalCacheManager.StateModify,
                        });
                        //sender.Acquired[rpc.Argument.GlobalTableKey] = GlobalCacheManager.StateModify;
                        log2.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateAcquireStatePending()
                        {
                            Key = rpc.Argument.GlobalTableKey,
                            AcquireStatePending = GlobalCacheManager.StateInvalid,
                        });
                        // cs.AcquireStatePending = GlobalCacheManager.StateInvalid;
                        Raft.AppendLog(log2);

                        Monitor.Pulse(cs);

                        logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.SendResult();
                        return 0;
                    }

                    List<Util.KV<GlobalCacheManagerStateMachine.CacheHolder, GlobalCacheManager.Reduce >> reducePending
                        = new List<Util.KV<GlobalCacheManagerStateMachine.CacheHolder, GlobalCacheManager.Reduce>>();
                    HashSet<GlobalCacheManagerStateMachine.CacheHolder> reduceSuccessed = new HashSet<GlobalCacheManagerStateMachine.CacheHolder>();
                    bool senderIsShare = false;
                    // 先把降级请求全部发送给出去。
                    foreach (var c in cs.Share)
                    {
                        if (c == sender.Id)
                        {
                            senderIsShare = true;
                            reduceSuccessed.Add(sender);
                            continue;
                        }
                        var shareHolder = StateMachine.Sessions.GetOrAdd(c);
                        var reduce = shareHolder.ReduceWaitLater(
                            rpc.Argument.GlobalTableKey,
                            GlobalCacheManager.StateInvalid);
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

                    var log3 = new GlobalCacheManagerStateMachine.OperatesLog();
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
                                    if (reduce.Value.Result.State == GlobalCacheManager.StateInvalid)
                                    {
                                        // 后面还有个成功的处理循环，但是那里可能包含sender，在这里更新吧。
                                        log3.Operates.Add(new GlobalCacheManagerStateMachine.RemoveCacheHolderAcquired()
                                        {
                                            Id = reduce.Key.Id,
                                            Key = rpc.Argument.GlobalTableKey,
                                        });
                                        //reduce.Key.Acquired.TryRemove(rpc.Argument.GlobalTableKey, out var _);
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
                        },
                        "GlobalCacheManager.AcquireModify.WaitReduce");
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    Monitor.Wait(cs);

                    // 移除成功的。
                    foreach (GlobalCacheManagerStateMachine.CacheHolder successed in reduceSuccessed)
                    {
                        log3.Operates.Add(new GlobalCacheManagerStateMachine.RemoveCacheStateShare()
                        {
                            Key = rpc.Argument.GlobalTableKey,
                            Share = successed.Id,
                        });
                        //cs.Share.Remove(successed.Id);
                    }

                    Raft.AppendLog(log3);

                    // 如果前面降级发生中断(break)，这里不会为0。
                    if (cs.Share.Count == 0)
                    {
                        var log4 = new GlobalCacheManagerStateMachine.OperatesLog();
                        log4.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateModify()
                        {
                            Key = rpc.Argument.GlobalTableKey,
                            Modify = sender.Id,
                        });
                        //cs.Modify = sender.Id;
                        log4.Operates.Add(new GlobalCacheManagerStateMachine.PutCacheHolderAcquired()
                        {
                            Id = sender.Id,
                            Key = rpc.Argument.GlobalTableKey,
                            AcquireState = GlobalCacheManager.StateModify,
                        });
                        //sender.Acquired[rpc.Argument.GlobalTableKey] = GlobalCacheManager.StateModify;
                        log4.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateAcquireStatePending()
                        {
                            Key = rpc.Argument.GlobalTableKey,
                            AcquireStatePending = GlobalCacheManager.StateInvalid,
                        });
                        //cs.AcquireStatePending = GlobalCacheManager.StateInvalid;

                        Raft.AppendLog(log4);

                        Monitor.Pulse(cs); // Pending 结束，唤醒一个进来就可以了。

                        logger.Debug("8 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.SendResult();
                    }
                    else
                    {
                        var log5 = new GlobalCacheManagerStateMachine.OperatesLog();
                        // senderIsShare 在失败的时候，Acquired 没有变化，不需要更新。
                        if (senderIsShare)
                        {
                            log5.Operates.Add(new GlobalCacheManagerStateMachine.AddCacheStateShare()
                            {
                                Key = rpc.Argument.GlobalTableKey,
                                Share = sender.Id,
                            });
                            //cs.Share.Add(sender);
                            // 失败了，要把原来是share的sender恢复。先这样吧。
                        }

                        log5.Operates.Add(new GlobalCacheManagerStateMachine.SetCacheStateAcquireStatePending()
                        {
                            Key = rpc.Argument.GlobalTableKey,
                            AcquireStatePending = GlobalCacheManager.StateInvalid,
                        });
                        //cs.AcquireStatePending = GlobalCacheManager.StateInvalid;

                        Raft.AppendLog(log5);

                        Monitor.Pulse(cs); // Pending 结束，唤醒一个进来就可以了。

                        logger.Error("XXX 10 {0} {1} {2}", sender, rpc.Argument.State, cs);

                        rpc.Result.State = GlobalCacheManager.StateInvalid;
                        rpc.SendResultCode(GlobalCacheManager.AcquireModifyFaild);
                    }
                    // 很好，网络失败不再看成成功，发现除了加break，
                    // 其他处理已经能包容这个改动，都不用动。
                    return 0;
                }
            }
        }

        /*
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
            public ConcurrentDictionary<GlobalCacheManager.GlobalTableKey, int>
                Acquired { get; }
                = new ConcurrentDictionary<GlobalCacheManager.GlobalTableKey, int>
                (GlobalCacheManager.DefaultConcurrencyLevel, 1000000);

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

            public int Reduce(GlobalCacheManager.GlobalTableKey gkey, int state)
            {
                try
                {
                    var reduce = ReduceWaitLater(gkey, state);
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
            public GlobalCacheManager.Reduce ReduceWaitLater(
                GlobalCacheManager.GlobalTableKey gkey, int state)
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
                        var reduce = new GlobalCacheManager.Reduce(gkey, state);
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
        */
    }
}
