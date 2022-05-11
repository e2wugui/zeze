using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Net;
using Zeze.Services;
using System.Threading.Tasks;
using NLog;
using Zeze.Services.GlobalCacheManager;
using Zeze.Serialize;

namespace Zeze.Transaction
{
    internal interface IGlobalAgent : IDisposable
    {
        // (ResultCode, State, GlobalSerialId)
        public Task<(long, int, long)> Acquire(Binary gkey, int state);
        public int GetGlobalCacheManagerHashIndex(Binary gkey);
    }

    public sealed class GlobalAgent : IGlobalAgent
    {
        internal static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public GlobalClient Client { get; private set; } // 未启用cache-sync时为空。

        public void Dispose()
        {
            Stop();
        }

        public class Agent
        {
            public bool ActiveClose { get; private set; } = false;
            public Connector Connector { get; private set; }
            public Zeze.Util.AtomicLong LoginTimes { get; } = new();
            public int GlobalCacheManagerHashIndex { get; }

            private readonly Zeze.Util.AtomicLong LastErrorTime = new();
            public const long FastErrorPeriod = 2 * 1000;

            public Agent(GlobalClient client, string host, int port, int globalCacheManagerHashIndex)
            {
                GlobalCacheManagerHashIndex = globalCacheManagerHashIndex;
                this.Connector = new Connector(host, port, true)
                {
                    UserState = this
                };
                client.Config.AddConnector(this.Connector);
            }

            private static void ThrowException(string msg, Exception cause = null)
            {
                var txn = Transaction.Current;
                if (txn != null)
                    txn.ThrowAbort(msg, cause);
                throw new Exception(msg, cause);
            }

            public async Task<AsyncSocket> ConnectAsync()
            {
                var so = Connector.TryGetReadySocket();
                if (null != so)
                    return so;

                if (global::Zeze.Util.Time.NowUnixMillis - LastErrorTime.Get() < FastErrorPeriod)
                    ThrowException("GlobalAgent.Connect: In Forbid Login Period");

                try
                {
                    return await Connector.GetReadySocketAsync();
                }
                catch (Exception e)
                {
                    // 并发的等待，简单用个规则：在间隔期内不再设置。
                    long now = global::Zeze.Util.Time.NowUnixMillis;
                    lock (this)
                    {
                        if (now - LastErrorTime.Get() > FastErrorPeriod)
                            LastErrorTime.GetAndSet(now);
                    }
                    ThrowException("GlobalAgent.Connect: Login Timeout", e);
                }
                return null; // never got here
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

                try
                {
                    var ready = Connector.TryGetReadySocket();
                    if (null != ready)
                        new NormalClose().SendAsync(ready).Wait();
                }
                finally
                {
                    Connector.Stop();
                }
            }

            public void OnSocketClose(GlobalClient client, Exception ex)
            {
                lock (this)
                {
                    if (ActiveClose)
                    {
                        // active close
                        return;
                    }
                }

                GlobalAgent.logger.Warn(ex, $"GlobalAgent({Connector}) Passive Close.");

                if (Connector.IsHandshakeDone)
                {
                    Task.Run(async () =>
                    {
                        foreach (var database in client.Zeze.Databases.Values)
                        {
                            foreach (var table in database.Tables)
                            {
                                await table.ReduceInvalidAllLocalOnly(GlobalCacheManagerHashIndex);
                            }
                        }
                        await client.Zeze.CheckpointNow();
                    });
                }
            }
        }

        internal Agent[] Agents;

        public int GetGlobalCacheManagerHashIndex(Binary gkey)
        {
            return gkey.GetHashCode() % Agents.Length;
        }

        public async Task<(long, int, long)> Acquire(Binary gkey, int state)
        {
            if (null != Client)
            {
                var agent = Agents[GetGlobalCacheManagerHashIndex(gkey)]; // hash
                var socket = await agent.ConnectAsync();

                // 请求处理错误抛出异常（比如网络或者GlobalCacheManager已经不存在了），打断外面的事务。
                // 一个请求异常不关闭连接，尝试继续工作。
                var rpc = new Acquire(gkey, state);
                await rpc.SendAsync(socket, 12000);
                /*
                if (rpc.ResultCode != 0) // 这个用来跟踪调试，正常流程使用Result.State检查结果。
                {
                    logger.Warn("Acquire ResultCode={0} {1}", rpc.ResultCode, rpc.Result);
                }
                */
                if (rpc.ResultCode < 0)
                {
                    Transaction.Current.ThrowAbort("GlobalAgent.Acquire Failed");
                    // never got here
                }
                switch (rpc.ResultCode)
                {
                    case GlobalCacheManagerServer.AcquireModifyFailed:
                    case GlobalCacheManagerServer.AcquireShareFailed:
                        Transaction.Current.ThrowAbort("GlobalAgent.Acquire Failed");
                        // never got here
                        break;
                }
                return (rpc.ResultCode, rpc.Result.State, rpc.Result.GlobalSerialId);
            }
            logger.Debug("Acquire local ++++++");
            return (0, state, 0);
        }

        public async Task<long> ProcessReduceRequest(Zeze.Net.Protocol p)
        {
            var rpc = (Reduce)p;
            switch (rpc.Argument.State)
            {
                case GlobalCacheManagerServer.StateInvalid:
                    {
                        var bb = ByteBuffer.Wrap(rpc.Argument.GlobalKey);
                        var tableId = bb.ReadInt4();
                        var table = Zeze.GetTable(tableId);
                        if (table == null)
                        {
                            logger.Warn($"ReduceInvalid Table Not Found={tableId},ServerId={Zeze.Config.ServerId}");
                            // 本地没有找到表格看作成功。
                            rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            rpc.SendResultCode(0);
                            return 0;
                        }
                        return await table.ReduceInvalid(rpc, bb);
                    }

                case GlobalCacheManagerServer.StateShare:
                    {
                        var bb = ByteBuffer.Wrap(rpc.Argument.GlobalKey);
                        var tableId = bb.ReadInt4();
                        var table = Zeze.GetTable(tableId);
                        if (table == null)
                        {
                            logger.Warn($"ReduceShare Table Not Found={tableId},ServerId={Zeze.Config.ServerId}");
                            // 本地没有找到表格看作成功。
                            rpc.Result.GlobalKey = rpc.Argument.GlobalKey;
                            rpc.Result.State = GlobalCacheManagerServer.StateInvalid;
                            rpc.SendResultCode(0);
                            return 0;
                        }
                        return await table.ReduceShare(rpc, bb);
                    }

                default:
                    rpc.Result = rpc.Argument;
                    rpc.SendResultCode(GlobalCacheManagerServer.ReduceErrorState);
                    return 0;
            }
        }

        public Zeze.Application Zeze { get;  }

        public GlobalAgent(Zeze.Application app)
        {
            Zeze = app;
        }

        public void Start(string[] hostNameOrAddress, int port)
        {
            lock (this)
            {
                if (null != Client)
                    return;

                Client = new GlobalClient(this, Zeze);
                // Raft Need. Zeze-App 自动启用持久化的全局唯一的Rpc.SessionId生成器。
                //Client.SessionIdGenerator = Zeze.ServiceManagerAgent.GetAutoKey(Client.Name).Next;

                Client.AddFactoryHandle(new Reduce().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new Reduce(),
                    Handle = ProcessReduceRequest,
                    TransactionLevel = TransactionLevel.None
                });
                Client.AddFactoryHandle(new Acquire().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new Acquire(),
                    TransactionLevel = TransactionLevel.None
                    // 同步方式调用，不需要设置Handle: Response Timeout 
                });
                Client.AddFactoryHandle(new Login().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = ()=>new Login(),
                    TransactionLevel = TransactionLevel.None
                });
                Client.AddFactoryHandle(new ReLogin().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new ReLogin(),
                    TransactionLevel = TransactionLevel.None
                });
                Client.AddFactoryHandle(new NormalClose().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new NormalClose(),
                    TransactionLevel = TransactionLevel.None
                });

                Agents = new Agent[hostNameOrAddress.Length];
                for (int i = 0; i < hostNameOrAddress.Length; ++i)
                {
                    var hp = hostNameOrAddress[i].Split(':');
                    if (hp.Length > 1)
                        Agents[i] = new Agent(Client, hp[0], int.Parse(hp[1]), i);
                    else
                        Agents[i] = new Agent(Client, hp[0], port, i);
                }
                Client.Start();
            }
            foreach (var agent in Agents)
            {
                agent.ConnectAsync().Wait(3 * 1000);
            }
        }

        public void Stop()
        {
            lock (this)
            {
                if (null == Client)
                    return;

                foreach (var agent in Agents)
                {
                    agent.Close();
                }
                Client.Stop();
            }
        }
    }

    public sealed class GlobalClient : Zeze.Net.Service
    {
        public GlobalClient(GlobalAgent agent, Application zeze)
            : base($"{agent.Zeze.SolutionName}.GlobalClient", zeze)
        {
        }

        public override void OnHandshakeDone(AsyncSocket so)
        {
            var agent = so.UserState as GlobalAgent.Agent;
            if (agent.LoginTimes.Get() > 0)
            {
                var relogin = new ReLogin();
                relogin.Argument.ServerId = Zeze.Config.ServerId;
                relogin.Argument.GlobalCacheManagerHashIndex = agent.GlobalCacheManagerHashIndex;
#pragma warning disable CS1998 // Async method lacks 'await' operators and will run synchronously
                relogin.Send(so,
                    async (_) =>
                    {
                        if (relogin.IsTimeout)
                        {
                            so.Close(new Exception("GlobalAgent.ReLogin Timeout"));
                        }
                        else if (relogin.ResultCode != 0)
                        {
                            so.Close(new Exception("GlobalAgent.ReLogin Fail code=" + relogin.ResultCode));
                        }
                        else
                        {
                            agent.LoginTimes.IncrementAndGet();
                            base.OnHandshakeDone(so);
                        }
                        return 0;
                    });
#pragma warning restore CS1998 // Async method lacks 'await' operators and will run synchronously
            }
            else
            {
                var login = new Login();
                login.Argument.ServerId = Zeze.Config.ServerId;
                login.Argument.GlobalCacheManagerHashIndex = agent.GlobalCacheManagerHashIndex;
#pragma warning disable CS1998 // Async method lacks 'await' operators and will run synchronously
                login.Send(so,
                    async (_) =>
                    {
                        if (login.IsTimeout)
                        {
                            so.Close(new Exception("GlobalAgent.Login Timeout"));
                        }
                        else if (login.ResultCode != 0)
                        {
                            so.Close(new Exception($"GlobalAgent.Logoin Error {login.ResultCode}"));
                        }
                        else
                        {
                            agent.LoginTimes.IncrementAndGet();
                            base.OnHandshakeDone(so);
                        }
                        return 0;
                    });
#pragma warning restore CS1998 // Async method lacks 'await' operators and will run synchronously
            }
        }

        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            _ = Util.Mission.CallAsync(factoryHandle.Handle, p);
        }

        public override void OnSocketClose(AsyncSocket so, Exception e)
        {
            base.OnSocketClose(so, e);
            var agent = so.UserState as GlobalAgent.Agent;
            if (null == e)
                e = new Exception("Peer Normal Close.");
            agent.OnSocketClose(this, e);
        }
    }
}
