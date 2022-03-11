
using Zeze.Beans.GlobalCacheManagerWithRaft;

namespace Zeze.Services
{
    public class GlobalCacheManagerWithRaftAgent : AbstractGlobalCacheManagerWithRaftAgent
    {
        static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public Application Zeze { get; }

        public GlobalCacheManagerWithRaftAgent(Application zeze, Zeze.Raft.RaftConfig raftconf = null)
        {
            Zeze = zeze;
        }

        public class ReduceBridge : GlobalCacheManager.Reduce
        {
            public Reduce Real { get; }

            public ReduceBridge(Reduce real)
            {
                Real = real;
                Argument.GlobalTableKey = new GlobalCacheManager.GlobalTableKey(
                    real.Argument.GlobalTableKey.TableName,
                    real.Argument.GlobalTableKey.Key.ToBytes());
                Argument.State = real.Argument.State;
                Argument.GlobalSerialId = real.Argument.GlobalSerialId;
            }

            public override void SendResult(Zeze.Net.Binary result = null)
            {
                Real.Result.GlobalTableKey = Real.Argument.GlobalTableKey; // no change
                Real.Result.GlobalSerialId = Result.GlobalSerialId;
                Real.Result.State = Result.State;

                Real.SendResult(result);
            }

            public override void SendResultCode(long code, Zeze.Net.Binary result = null)
            {
                Real.Result.GlobalTableKey = Real.Argument.GlobalTableKey; // no change
                Real.Result.GlobalSerialId = Result.GlobalSerialId;
                Real.Result.State = Result.State;

                Real.SendResultCode(code, result);
            }
        }

        protected override long ProcessReduceRequest(Zeze.Net.Protocol _p)
        {
            var rpc = _p as Zeze.Beans.GlobalCacheManagerWithRaft.Reduce;
            switch (rpc.Argument.State)
            {
                case GlobalCacheManagerServer.StateInvalid:
                    {
                        var table = Zeze.GetTable(rpc.Argument.GlobalTableKey.TableName);
                        if (table == null)
                        {
                            logger.Warn($"ReduceInvalid Table Not Found={rpc.Argument.GlobalTableKey.TableName},ServerId={Zeze.Config.ServerId}");
                            // 本地没有找到表格看作成功。
                            rpc.Result.GlobalTableKey = rpc.Argument.GlobalTableKey;
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            rpc.SendResultCode(0);
                            return 0;
                        }
                        return table.ReduceInvalid(new ReduceBridge(rpc));
                    }

                case GlobalCacheManagerServer.StateShare:
                    {
                        var table = Zeze.GetTable(rpc.Argument.GlobalTableKey.TableName);
                        if (table == null)
                        {
                            logger.Warn($"ReduceShare Table Not Found={rpc.Argument.GlobalTableKey.TableName},ServerId={Zeze.Config.ServerId}");
                            // 本地没有找到表格看作成功。
                            rpc.Result.GlobalTableKey = rpc.Argument.GlobalTableKey;
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            rpc.SendResultCode(0);
                            return 0;
                        }
                        return table.ReduceShare(new ReduceBridge(rpc));
                    }

                default:
                    rpc.Result = rpc.Argument;
                    rpc.SendResultCode(GlobalCacheManagerServer.ReduceErrorState);
                    return 0;
            }
        }

        internal RaftAgent[] Agents;

        internal int GetGlobalCacheManagerHashIndex(GlobalTableKey gkey)
        {
            return gkey.GetHashCode() % Agents.Length;
        }

        internal Acquire Acquire(GlobalTableKey gkey, int state)
        {
            if (null != Agents)
            {
                var agent = Agents[GetGlobalCacheManagerHashIndex(gkey)]; // hash
                var rpc = new Acquire();
                rpc.Argument.GlobalTableKey = gkey;
                rpc.Argument.State = state;
                agent.RaftClient.SendForWait(rpc).Task.Wait();
                switch (rpc.ResultCode)
                {
                    case GlobalCacheManagerServer.AcquireModifyFailed:
                    case GlobalCacheManagerServer.AcquireShareFailed:
                        Transaction.Transaction.Current.ThrowAbort("GlobalAgent.Acquire Failed");
                        break;
                }
                return rpc;
            }
            logger.Debug("Acquire local ++++++");
            var result = new Acquire();
            result.Result.State = state;
            return result;
        }

        public void Stop()
        {
            lock (this)
            {
                if (null == Agents)
                    return;

                foreach (var agent in Agents)
                {
                    agent.Close();
                }
            }
        }

        // 1. 【Login|ReLogin|NormalClose】会被Raft.Agent重发处理，这要求GlobalRaft能处理重复请求。
        // 2. 【Login|NormalClose】有多个事务处理，这跟rpc.UniqueRequestId唯一性有矛盾。【可行方法：去掉唯一判断，让流程正确处理重复请求。】
        // 3. 【ReLogin】没有数据修改，完全允许重复，并且不判断唯一性。
        // 4. Raft 高可用性，所以认为服务器永远不会关闭，就不需要处理服务器关闭时清理本地状态。
        public class RaftAgent
        {
            public GlobalCacheManagerWithRaftAgent GlobalCacheManagerWithRaftAgent { get; }
            public Zeze.Raft.Agent RaftClient { get; }
            public bool ActiveClose { get; private set; } = false;
            public Util.AtomicLong LoginTimes { get; } = new Util.AtomicLong();
            public int GlobalCacheManagerHashIndex { get; }

            public RaftAgent(GlobalCacheManagerWithRaftAgent global,
                Application zeze, int _GlobalCacheManagerHashIndex,
                Zeze.Raft.RaftConfig raftconf = null)
            {
                GlobalCacheManagerWithRaftAgent = global;
                GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
                RaftClient = new Raft.Agent("Zeze.GlobalRaft.Agent", zeze, raftconf) { OnSetLeader = RaftOnSetLeader };
                GlobalCacheManagerWithRaftAgent.RegisterProtocols(RaftClient.Client);
                RaftClient.Client.Start();
            }

            public void Close()
            {
                lock (this)
                {
                    // 简单保护一下，Close 正常程序退出的时候才调用这个，应该不用保护。
                    if (ActiveClose)
                        return;
                    ActiveClose = true;
                }
                RaftClient.SendForWait(new NormalClose()).Task.Wait();
                RaftClient.Client.Stop();
            }

            private void RaftOnSetLeader(Zeze.Raft.Agent agent)
            {
                if (LoginTimes.Get() == 0)
                {
                    var login = new Login();
                    login.Argument.ServerId = agent.Client.Zeze.Config.ServerId;
                    login.Argument.GlobalCacheManagerHashIndex = 0; // agent.GlobalCacheManagerHashIndex;

                    agent.Send(login,
                        (p) =>
                        {
                            var rpc = p as Login;
                            if (rpc.IsTimeout || rpc.ResultCode != 0)
                            {
                                logger.Error($"Login Timeout Or ResultCode != 0. Code={rpc.ResultCode}");
                            }
                            else
                            {
                                LoginTimes.IncrementAndGet();
                            }
                            return 0;
                        }, true);
                }
                else
                {
                    var relogin = new ReLogin();
                    relogin.Argument.ServerId = agent.Client.Zeze.Config.ServerId;
                    relogin.Argument.GlobalCacheManagerHashIndex = 0; // agent.GlobalCacheManagerHashIndex;
                    agent.Send(relogin,
                        (p) =>
                        {
                            var rpc = p as ReLogin;
                            if (rpc.IsTimeout || rpc.ResultCode != 0)
                            {
                                logger.Error($"Login Timeout Or ResultCode != 0. Code={rpc.ResultCode}");
                            }
                            else
                            {
                                LoginTimes.IncrementAndGet();
                            }
                            return 0;
                        }, true);
                }
            }
        }
    }
}
