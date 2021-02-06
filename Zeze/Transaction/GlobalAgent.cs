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
        private GlobalClient Client { get; set; }
        internal AsyncSocket ClientSocket;
        internal TaskCompletionSource<AsyncSocket> Connected;

        internal int Acquire(GlobalCacheManager.GlobalTableKey gkey, int state)
        {
            if (null != ClientSocket)
            {
                // 请求处理错误抛出异常（比如网络或者GlobalCacheManager已经不存在了）。
                GlobalCacheManager.Acquire rpc = new GlobalCacheManager.Acquire(gkey, state);
                rpc.SendForWait(ClientSocket, 12000).Task.Wait();
                /*
                if (rpc.ResultCode != 0)
                {
                    logger.Warn("Acquire ResultCode={0} {1}", rpc.ResultCode, rpc.Result);
                }
                */
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

        public GlobalAgent(Zeze.Application zeze)
        {
            Zeze = zeze;
        }

        public void Start(string hostNameOrAddress, int port)
        {
            lock (this)
            {
                if (null != Client)
                    return;
                Client = new GlobalClient(this, Zeze);
                Client.AddFactoryHandle(new GlobalCacheManager.Reduce().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new GlobalCacheManager.Reduce(),
                    Handle = ProcessReduceRequest
                });
                Client.AddFactoryHandle(new GlobalCacheManager.Acquire().TypeId, new Service.ProtocolFactoryHandle()
                {
                    Factory = () => new GlobalCacheManager.Acquire(),
                    // 同步方式调用，不需要设置Handle: Response Timeout 
                });

                Connected = new TaskCompletionSource<AsyncSocket>();
                ClientSocket = Client.NewClientSocket(hostNameOrAddress, port);
                Connected.Task.Wait();
            }
        }

        public void Stop()
        {
            lock (this)
            {
                if (null == Client)
                    return;
                ClientSocket = null;
                Client.Close();
                Client = null;
            }
        }
    }

    public sealed class GlobalClient : Zeze.Net.Service
    {
        GlobalAgent agent;

        public GlobalClient(GlobalAgent agent, Application zeze) : base("GlobalClient", zeze)
        {
            this.agent = agent;
        }

        public override void OnSocketConnected(AsyncSocket so)
        {
            base.OnSocketConnected(so);
            agent.Connected.SetResult(so);
        }

        public override void OnSocketConnectError(AsyncSocket so, Exception e)
        {
            base.OnSocketConnectError(so, e);
            agent.Connected.SetException(e);
        }

        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            // Reduce 很重要。必须得到执行，不能使用默认线程池(Task.Run),防止饥饿。
            if (null != factoryHandle.Handle)
            {
                agent.Zeze.InternalThreadPool.QueueUserWorkItem(() => factoryHandle.Handle(p));
            }
        }

        public override void OnSocketClose(AsyncSocket so, Exception e)
        {
            base.OnSocketClose(so, e);
            if (so == agent.ClientSocket)
            {
                // XXX 被动关闭。和 GlobalCacheManager 失去连接，意味着 Cache 同步无法正常工作。必须停止程序。
                // 确认 Environment.Exit 会触发 AppDomain.ProcessExit
                System.Environment.Exit(5678);
            }
        }
    }
}
