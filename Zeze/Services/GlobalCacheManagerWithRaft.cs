
using System;
using Zeze.Raft.RocksRaft;
using Zeze.Component.GlobalCacheManagerWithRaft;

namespace Zeze.Services
{
    public class GlobalCacheManagerWithRaft : AbstractGlobalCacheManagerWithRaft
    {
        protected override long ProcessAcquireRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as Zeze.Component.GlobalCacheManagerWithRaft.Acquire;
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
                        rpc.Result.State = Release(rpc.Sender.UserState as CacheHolder, rpc.Argument.GlobalTableKey, true);
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

        private long AcquireShare(Zeze.Component.GlobalCacheManagerWithRaft.Acquire rpc)
        {
            CacheHolder sender = (CacheHolder)rpc.Sender.UserState;

            while (true)
            {
                CacheState cs = Global.GetOrAdd(rpc.Argument.GlobalTableKey);
                var lockey = Locks.Get(rpc.Argument.GlobalTableKey);
                lockey.Enter();
                try
                {
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
                                    rpc.SendResultCode(GlobalCacheManagerServer.AcquireShareDeadLockFound);
                                    return 0;
                                }
                                break;

                            case GlobalCacheManagerServer.StateModify:
                                if (cs.Modify == sender.ServerId || cs.Share.Contains(sender.ServerId))
                                {
                                    logger.Debug("2 {0} {1} {2}", sender, rpc.Argument.State, cs);
                                    rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                                    rpc.SendResultCode(GlobalCacheManagerServer.AcquireShareDeadLockFound);
                                    return 0;
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
                    var Acquired = Rocks.OpenTable<GlobalTableKey, AcquiredState>("Acquired", sender.ServerId, 10000);
                    if (cs.Modify != -1)
                    {
                        if (cs.Modify == sender.ServerId)
                        {
                            cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                            logger.Debug("4 {0} {1} {2}", sender, rpc.Argument.State, cs);
                            rpc.Result.State = GlobalCacheManagerServer.StateModify;
                            // 已经是Modify又申请，可能是sender异常关闭，
                            // 又重启连上。更新一下。应该是不需要的。
                            Acquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateModify });
                            rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                            rpc.SendResultCode(GlobalCacheManagerServer.AcquireShareAlreadyIsModify);
                            return 0;
                        }

                        Reduce reduceRpc = null;
                        Zeze.Util.Task.Run(
                            () =>
                            {
                                reduceRpc = cs.Modify.Reduce(rpc.Argument.GlobalTableKey, GlobalCacheManagerServer.StateShare, cs.GlobalSerialId);

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

                        switch (reduceRpc.Result.State)
                        {
                            case GlobalCacheManagerServer.StateShare:
                                Acquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                                cs.Share.Add(cs.Modify); // 降级成功。
                                break;

                            case GlobalCacheManagerServer.StateInvalid:
                                // 降到了 Invalid，此时就不需要加入 Share 了。
                                Acquired.Remove(rpc.Argument.GlobalTableKey);
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
                                rpc.SendResultCode(GlobalCacheManagerServer.AcquireShareFailed);
                                return 0;
                        }

                        cs.Modify = -1;
                        Acquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                        cs.Share.Add(sender.ServerId);
                        cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                        lockey.Pulse();
                        logger.Debug("6 {0} {1} {2}", sender, rpc.Argument.State, cs);
                        rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                        rpc.SendResult();
                        return 0;
                    }

                    Acquired.Put(rpc.Argument.GlobalTableKey, new AcquiredState() { State = GlobalCacheManagerServer.StateShare });
                    cs.Share.Add(sender.ServerId);
                    cs.AcquireStatePending = GlobalCacheManagerServer.StateInvalid;
                    lockey.Pulse();
                    logger.Debug("7 {0} {1} {2}", sender, rpc.Argument.State, cs);
                    rpc.Result.GlobalSerialId = cs.GlobalSerialId;
                    rpc.SendResult();

                    return 0;
                }
                finally
                {
                    lockey.Exit();
                }
            }
        }

        private long AcquireModify(Zeze.Component.GlobalCacheManagerWithRaft.Acquire rpc)
        {
            return 0;
        }

        protected override long ProcessCleanupRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Component.GlobalCacheManagerWithRaft.Cleanup;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override long ProcessKeepAliveRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Component.GlobalCacheManagerWithRaft.KeepAlive;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override long ProcessLoginRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Component.GlobalCacheManagerWithRaft.Login;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override long ProcessNormalCloseRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Component.GlobalCacheManagerWithRaft.NormalClose;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override long ProcessReduceRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Component.GlobalCacheManagerWithRaft.Reduce;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        protected override long ProcessReLoginRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Zeze.Component.GlobalCacheManagerWithRaft.ReLogin;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        private Rocks Rocks { get; }
        private readonly Util.AtomicLong SerialIdGenerator = new Util.AtomicLong();
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        private readonly Locks Locks = new Locks();
        private readonly Table<GlobalTableKey, CacheState> Global;

        public GlobalCacheManagerWithRaft(string raftName)
        { 
            Rocks = new Rocks(raftName);

            RegisterRocksTables(Rocks);
            RegisterProtocols(Rocks.Raft.Server);

            Global = Rocks.OpenTable<GlobalTableKey, CacheState>("Global"); // TODO GEN?
            Rocks.Raft.Server.Start();
        }

        public sealed class CacheHolder
        {
            public long SessionId { get; private set; }
            public int GlobalCacheManagerHashIndex { get; private set; }
            public int ServerId { get; private set; }
        }
    }
}
