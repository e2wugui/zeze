﻿using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Net;
using Zeze.Services;
using System.Threading.Tasks;

namespace Zeze.Transaction
{
    public class GlobalAgent
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
        private GlobalClient Client { get; set; }
        private AsyncSocket ClientSocket;
        internal TaskCompletionSource<AsyncSocket> Connected;

        internal int Acquire(GlobalTableKey gkey, int state)
        {
            AsyncSocket clientSocket = GetClientSocket();
            if (null != clientSocket)
            {
                // 请求处理错误抛出异常（比如网络或者GlobalCacheManager已经不存在了）。
                Acquire rpc = new Acquire(gkey, state);
                rpc.SendForWait(clientSocket).Task.Wait();
                if (rpc.ResultCode != 0)
                {
                    logger.Warn("Acquire ResultCode={0} {1}", rpc.ResultCode, rpc.Result);
                }
                return rpc.Result.State;
            }
            return state;
        }

        private AsyncSocket GetClientSocket()
        {
            return null;
        }

        int ProcessReduceRequest(Reduce rpc)
        {
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
                Client = new GlobalClient(this);
                Client.AddFactory(new Reduce().TypeId, () => new Reduce());
                Client.AddHandle(new Reduce().TypeRpcRequestId, Service.MakeHandle<Reduce>(this, GetType().GetMethod(nameof(ProcessReduceRequest))));
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
                Client.Close();
                Client = null;
            }
        }
    }

    public class GlobalClient : Zeze.Net.Service
    {
        GlobalAgent agent;
        public GlobalClient(GlobalAgent agent)
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

        public override void OnSocketClose(AsyncSocket so, Exception e)
        {
            base.OnSocketClose(so, e);
            // XXX 和 GlobalCacheManager 失去连接，意味着 Cache 同步无法正常工作。必须停止程序。
            System.Environment.Exit(5678);
        }

        public override void DispatchProtocol(Protocol p)
        {
            if (Handles.TryGetValue(p.TypeId, out var handle))
            {
                Task.Run(() => handle(p));
            }
            else
            {
                throw new Exception("Protocol Handle Not Found. " + p);
            }
        }
    }
}
