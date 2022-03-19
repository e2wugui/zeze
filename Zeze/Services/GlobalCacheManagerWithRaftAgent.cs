
using Zeze.Beans.GlobalCacheManagerWithRaft;
using System.Threading.Tasks;

namespace Zeze.Services
{
    public class GlobalCacheManagerWithRaftAgent : AbstractGlobalCacheManagerWithRaftAgent, Zeze.Transaction.IGlobalAgent
    {
        static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public Application Zeze { get; }

        public void Dispose()
        {
            Stop();
        }

        public GlobalCacheManagerWithRaftAgent(Application zeze)
        {
            Zeze = zeze;
        }

        public void Start(string[] hosts)
        {
            lock (this)
            {
                if (null != Agents)
                    return;

                Agents = new RaftAgent[hosts.Length];
                for (int i = 0; i < hosts.Length; ++i)
                {
                    var raftconf = Raft.RaftConfig.Load(hosts[i]);
                    Agents[i] = new RaftAgent(this, Zeze, i, raftconf);
                }

                foreach (var agent in Agents)
                {
                    agent.RaftClient.Client.Start();
                }
            }
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
                Agents = null;
            }
        }

        public class ReduceBridge : GlobalCacheManager.Reduce
        {
            public Reduce Real { get; }

            public ReduceBridge(Reduce real)
            {
                Real = real;
                Argument.GlobalTableKey = real.Argument.GlobalTableKey;
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

        public int GetGlobalCacheManagerHashIndex(GlobalTableKey gkey)
        {
            return gkey.GetHashCode() % Agents.Length;
        }

        public (long, int, long) Acquire(GlobalTableKey gkey, int state)
        {
            if (null != Agents)
            {
                var agent = Agents[GetGlobalCacheManagerHashIndex(gkey)]; // hash

                agent.WaitLoginSuccess();

                var rpc = new Acquire();
                rpc.Argument.GlobalTableKey = gkey;
                rpc.Argument.State = state;
                agent.RaftClient.SendForWait(rpc).Task.Wait();

                if (rpc.ResultCode < 0)
                {
                    Transaction.Transaction.Current.ThrowAbort("GlobalAgent.Acquire Failed");
                    // never got here
                }
                switch (rpc.ResultCode)
                {
                    case GlobalCacheManagerServer.AcquireModifyFailed:
                    case GlobalCacheManagerServer.AcquireShareFailed:
                        Transaction.Transaction.Current.ThrowAbort("GlobalAgent.Acquire Failed");
                        // never got here
                        break;
                }
                return (rpc.ResultCode, rpc.Result.State, rpc.Result.GlobalSerialId);
            }
            logger.Debug("Acquire local ++++++");
            return (0, state, 0);
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
                RaftClient.DispatchProtocolToInternalThreadPool = true;
                GlobalCacheManagerWithRaftAgent.RegisterProtocols(RaftClient.Client);
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
                if (LoginTimes.Get() > 0)
                    RaftClient.SendForWait(new NormalClose()).Task.Wait(10 * 1000); // 10s
                RaftClient.Client.Stop();
            }

            private volatile TaskCompletionSource<bool> LoginFuture = new TaskCompletionSource<bool>();

            public void WaitLoginSuccess()
            {
                while (true)
                {
                    try
                    {
                        var volatiletmp = LoginFuture;
                        if (volatiletmp.Task.IsCompletedSuccessfully && volatiletmp.Task.Result)
                            return;
                        volatiletmp.Task.Wait();
                    }
                    catch (System.Exception)
                    {
                    }
                }
            }

            private TaskCompletionSource<bool> StartNewLogin()
            {
                lock (this)
                {
                    LoginFuture.TrySetCanceled(); // 如果旧的Future上面有人在等，让他们失败。
                    LoginFuture = new TaskCompletionSource<bool>();
                    return LoginFuture;
                }
            }

            private void RaftOnSetLeader(Zeze.Raft.Agent agent)
            {
                var future = StartNewLogin();

                if (LoginTimes.Get() == 0)
                {
                    var login = new Login();
                    login.Argument.ServerId = agent.Client.Zeze.Config.ServerId;
                    login.Argument.GlobalCacheManagerHashIndex = GlobalCacheManagerHashIndex;

                    agent.Send(login,
                        (p) =>
                        {
                            var rpc = p as Login;
                            if (rpc.IsTimeout || rpc.ResultCode != 0)
                            {
                                logger.Error($"Login Timeout Or ResultCode != 0. Code={rpc.ResultCode}");
                                // 这里不记录future失败，等待raft通知新的Leader启动新的Login。让外面等待的线程一直等待。
                            }
                            else
                            {
                                LoginTimes.IncrementAndGet();
                                future.TrySetResult(true);
                            }
                            return 0;
                        }, true);
                }
                else
                {
                    var relogin = new ReLogin();
                    relogin.Argument.ServerId = agent.Client.Zeze.Config.ServerId;
                    relogin.Argument.GlobalCacheManagerHashIndex = GlobalCacheManagerHashIndex;
                    agent.Send(relogin,
                        (p) =>
                        {
                            var rpc = p as ReLogin;
                            if (rpc.IsTimeout || rpc.ResultCode != 0)
                            {
                                logger.Error($"Login Timeout Or ResultCode != 0. Code={rpc.ResultCode}");
                                // 这里不记录future失败，等待raft通知新的Leader启动新的Login。让外面等待的线程一直等待。
                            }
                            else
                            {
                                LoginTimes.IncrementAndGet();
                                future.TrySetResult(true);
                            }
                            return 0;
                        }, true);
                }
            }
        }
    }
}
