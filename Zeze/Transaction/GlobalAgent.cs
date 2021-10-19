using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Net;
using Zeze.Services;
using System.Threading.Tasks;
using NLog;

namespace Zeze.Transaction
{
    public sealed class GlobalAgent
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        public GlobalClient Client { get; private set; } // 未启用cache-sync时为空。

        public class Agent
        {
            public AsyncSocket Socket { get; private set; }
            public TaskCompletionSource<AsyncSocket> Logined { get; private set; }
            public string Host { get; }
            public int Port { get; }
            public Zeze.Util.AtomicLong LoginedTimes { get; } = new Util.AtomicLong();
            public int GlobalCacheManagerHashIndex { get; }

            private long LastErrorTime = 0;
            public const long ForbitPeriod = 10 * 1000; // 10 seconds

            public Agent(string host, int port, int _GlobalCacheManagerHashIndex)
            {
                this.Host = host;
                this.Port = port;
                GlobalCacheManagerHashIndex = _GlobalCacheManagerHashIndex;
            }

            public AsyncSocket Connect(GlobalClient client)
            {
                lock (this)
                {
                    // 这个能放到(lock(this)外吗？严格点，放这里更安全。
                    // TODO IsCompletedSuccessfully net471之类的没有这个方法，先不管。net471之类的unity要用。
                    if (null != Logined && Logined.Task.IsCompletedSuccessfully)
                        return Logined.Task.Result;

                    if (global::Zeze.Util.Time.NowUnixMillis - LastErrorTime < ForbitPeriod)
                        throw new AbortException("GloalAgent.Connect: In Forbit Login Period");

                    if (null == Socket)
                    {
                        Socket = client.NewClientSocket(Host, Port, this);
                        // 每次新建连接创建future，没并发问题吧，还没仔细考虑。
                        Logined = new TaskCompletionSource<AsyncSocket>();
                    }
                }
                // 重新设置一个总超时。整个登录流程有ConnectTimeout,LoginTimeout。
                // 要注意，这个超时发生时，登录流程可能还在进行中。
                // 这里先不清理，下一次进来再次等待（需要确认这样可行）。
                if (false == Logined.Task.Wait(5000))
                {
                    lock (this)
                    {
                        // 并发的等待，简单用个规则：在间隔期内不再设置。
                        long now = global::Zeze.Util.Time.NowUnixMillis;
                        if (now - LastErrorTime > ForbitPeriod)
                            LastErrorTime = now;
                    }
                    throw new AbortException("GloalAgent.Connect: Login Timeout");
                }
                return Socket;
            }

            public void Close()
            {
                var tmp = Socket;
                try
                {
                    lock (this)
                    {
                        // 简单保护一下，Close 正常程序退出的时候才调用这个，应该不用保护。
                        if (null == Socket)
                            return;

                        Socket = null; // 正常关闭，先设置这个，以后 OnSocketClose 的时候判断做不同的处理。
                    }

                    var normalClose = new GlobalCacheManager.NormalClose();
                    var future = new TaskCompletionSource<int>();
                    normalClose.Send(tmp,
                        (_) =>
                        {
                            if (normalClose.IsTimeout)
                            {
                                future.SetResult(-100); // 关闭错误就不抛异常了。
                            }
                            else
                            {
                                future.SetResult(normalClose.ResultCode);
                                if (normalClose.ResultCode != 0)
                                    logger.Error("GlobalAgent:NormalClose ResultCode={0}", normalClose.ResultCode);
                            }
                            return 0;
                        });
                    future.Task.Wait();
                }
                finally
                {
                    tmp?.Dispose();
                    // 关闭时，让等待Login的线程全部失败。
                    Logined.TrySetException(new Exception("GlobalAgent.Close"));
                }
            }

            public void OnSocketClose(GlobalClient client, Exception ex)
            {
                lock (this)
                {
                    if (null == Socket)
                    {
                        // active close
                        return;
                    }
                    Socket = null;
                }
                if (Logined.Task.IsCompletedSuccessfully)
                {
                    foreach (var database in client.Zeze.Databases.Values)
                    {
                        foreach (var table in database.Tables)
                        {
                            table.ReduceInvalidAllLocalOnly(GlobalCacheManagerHashIndex);
                        }
                    }
                    client.Zeze.CheckpointRun();
                }
                Logined.TrySetException(ex); // 连接关闭，这个继续保持。仅在Connect里面需要时创建。
            }
        }

        internal Agent[] Agents;

        internal int GetGlobalCacheManagerHashIndex(GlobalCacheManager.GlobalTableKey gkey)
        {
            return gkey.GetHashCode() % Agents.Length;
        }

        internal int Acquire(GlobalCacheManager.GlobalTableKey gkey, int state)
        {
            if (null != Client)
            {
                var agent = Agents[GetGlobalCacheManagerHashIndex(gkey)]; // hash
                var socket = agent.Connect(Client);

                // 请求处理错误抛出异常（比如网络或者GlobalCacheManager已经不存在了），打断外面的事务。
                // 一个请求异常不关闭连接，尝试继续工作。
                GlobalCacheManager.Acquire rpc = new GlobalCacheManager.Acquire(gkey, state);
                rpc.SendForWait(socket, 12000).Task.Wait();
                /*
                if (rpc.ResultCode != 0) // 这个用来跟踪调试，正常流程使用Result.State检查结果。
                {
                    logger.Warn("Acquire ResultCode={0} {1}", rpc.ResultCode, rpc.Result);
                }
                */
                switch (rpc.ResultCode)
                {
                    case GlobalCacheManager.AcquireModifyFaild:
                    case GlobalCacheManager.AcquireShareFaild:
                        throw new AbortException("GlobalAgent.Acquire Faild");
                }
                return rpc.Result.State;
            }
            logger.Debug("Acquire local ++++++");
            return state;
        }

        public int ProcessReduceRequest(Zeze.Net.Protocol p)
        {
            GlobalCacheManager.Reduce rpc = (GlobalCacheManager.Reduce)p;
            switch (rpc.Argument.State)
            {
                case GlobalCacheManager.StateInvalid:
                    return Zeze.GetTable(rpc.Argument.GlobalTableKey.TableName).ReduceInvalid(rpc);

                case GlobalCacheManager.StateShare:
                    return Zeze.GetTable(rpc.Argument.GlobalTableKey.TableName).ReduceShare(rpc);

                default:
                    rpc.Result = rpc.Argument;
                    rpc.SendResultCode(GlobalCacheManager.ReduceErrorState);
                    return 0;
            }
        }

        public Zeze.Application Zeze { get;  }

        public GlobalAgent(Zeze.Application app)
        {
            Zeze = app;
        }

        public void Start(string hostNameOrAddress, int port)
        {
            lock (this)
            {
                if (null != Client)
                    return;

                Client = new GlobalClient(this, Zeze);
                // Raft Need. Zeze-App 自动启用持久化的全局唯一的Rpc.SessionId生成器。
                //Client.SessionIdGenerator = Zeze.ServiceManagerAgent.GetAutoKey(Client.Name).Next;

                Client.AddFactoryHandle(new GlobalCacheManager.Reduce().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new GlobalCacheManager.Reduce(),
                    Handle = ProcessReduceRequest,
                    NoProcedure = true
                });
                Client.AddFactoryHandle(new GlobalCacheManager.Acquire().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new GlobalCacheManager.Acquire(),
                    NoProcedure = true
                    // 同步方式调用，不需要设置Handle: Response Timeout 
                });
                Client.AddFactoryHandle(new GlobalCacheManager.Login().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = ()=>new GlobalCacheManager.Login(),
                    NoProcedure = true
                });
                Client.AddFactoryHandle(new GlobalCacheManager.ReLogin().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new GlobalCacheManager.ReLogin(),
                    NoProcedure = true
                });
                Client.AddFactoryHandle(new GlobalCacheManager.NormalClose().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new GlobalCacheManager.NormalClose(),
                    NoProcedure = true
                });

                var globals = hostNameOrAddress.Split(';');
                Agents = new Agent[globals.Length];
                for (int i = 0; i < globals.Length; ++i)
                {
                    var hp = globals[i].Split(':');
                    if (hp.Length > 1)
                        Agents[i] = new Agent(hp[0], int.Parse(hp[1]), i);
                    else
                        Agents[i] = new Agent(hp[0], port, i);
                }
                foreach (var agent in Agents)
                {
                    try
                    {
                        agent.Connect(Client);
                    }
                    catch (Exception ex)
                    {
                        // 允许部分GlobalCacheManager连接错误时，继续启动程序，虽然后续相关事务都会失败。
                        logger.Error(ex, "GlobalAgent.Connect"); 
                    }
                }
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
                Client = null;
            }
        }
    }

    public sealed class GlobalClient : Zeze.Net.Service
    {
        GlobalAgent agent;

        public GlobalClient(GlobalAgent agent, Application zeze)
            : base($"{agent.Zeze.SolutionName}.GlobalClient", zeze)
        {
            this.agent = agent;
        }

        public override void OnHandshakeDone(AsyncSocket so)
        {
            base.OnHandshakeDone(so);
            var agent = so.UserState as GlobalAgent.Agent;
            if (agent.LoginedTimes.Get() > 1)
            {
                var relogin = new GlobalCacheManager.ReLogin();
                relogin.Argument.ServerId = Zeze.Config.ServerId;
                relogin.Argument.GlobalCacheManagerHashIndex = agent.GlobalCacheManagerHashIndex;
                relogin.Send(so,
                    (_) =>
                    {
                        if (relogin.IsTimeout)
                        {
                            agent.Logined.TrySetException(new Exception("GloalAgent.ReLogin Timeout")); ;
                        }
                        else if (relogin.ResultCode != 0)
                        {
                            agent.Logined.TrySetException(new Exception($"GlobalAgent.ReLogoin Error {relogin.ResultCode}"));
                        }
                        else
                        {
                            agent.LoginedTimes.IncrementAndGet();
                            agent.Logined.SetResult(so);
                        }
                        return 0;
                    });
            }
            else
            {
                var login = new GlobalCacheManager.Login();
                login.Argument.ServerId = Zeze.Config.ServerId;
                login.Argument.GlobalCacheManagerHashIndex = agent.GlobalCacheManagerHashIndex;
                login.Send(so,
                    (_) =>
                    {
                        if (login.IsTimeout)
                        {
                            agent.Logined.TrySetException(new Exception("GloalAgent.Login Timeout")); ;
                        }
                        else if (login.ResultCode != 0)
                        {
                            agent.Logined.TrySetException(new Exception($"GlobalAgent.Logoin Error {login.ResultCode}"));
                        }
                        else
                        {
                            agent.LoginedTimes.IncrementAndGet();
                            agent.Logined.SetResult(so);
                        }
                        return 0;
                    });
            }
        }

        public override void OnSocketConnectError(AsyncSocket so, Exception e)
        {
            base.OnSocketConnectError(so, e);
            var agent = so.UserState as GlobalAgent.Agent;
            if (null == e)
                e = new Exception("Normal Connect Error???"); // ConnectError 应该 e != null 吧，懒得确认了。
            agent.Logined.TrySetException(e);
        }

        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            // Reduce 很重要。必须得到执行，不能使用默认线程池(Task.Run),防止饥饿。
            if (null != factoryHandle.Handle)
            {
                agent.Zeze.InternalThreadPool.QueueUserWorkItem(
                    () => Util.Task.Call(() => factoryHandle.Handle(p), p));
            }
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
