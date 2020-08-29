using System;
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

        internal void Acquire(GlobalTableKey gkey, int state)
        {
            AsyncSocket client = GetClientSocket();
            if (null != client)
            {
                Acquire rpc = new Acquire(gkey, state);
                rpc.SendForWait(client).Task.Wait();
                if (rpc.ResultCode != 0)
                {
                    logger.Warn("Acquire ResultCode={0} {1}", rpc.ResultCode, rpc.Result);
                }
            }
        }

        private AsyncSocket GetClientSocket()
        {
            return null;
        }

        int ProcessReduceRequest(Reduce rpc)
        {
            return 0;
        }

        public void Start(string hostNameOrAddress, int port)
        {
            lock (this)
            {
                if (null != Client)
                    return;
                Client = new GlobalClient();
                Client.AddFactory(new Reduce().TypeId, () => new Reduce());
                Client.AddHandle(new Reduce().TypeRpcRequestId, Service.MakeHandle<Reduce>(this, GetType().GetMethod(nameof(ProcessReduceRequest))));
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
        public override void OnSocketConnected(AsyncSocket so)
        {
            base.OnSocketConnected(so);
        }

        public override void OnSocketConnectError(AsyncSocket so, Exception e)
        {
            base.OnSocketConnectError(so, e);
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
