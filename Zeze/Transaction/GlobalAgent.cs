using System;
using System.Collections.Generic;
using System.Text;
using Zeze.Net;
using Zeze.Services;

namespace Zeze.Transaction
{
    public class GlobalAgent
    {
        public static GlobalAgent Instance { get; } = new GlobalAgent();
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

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
    }

    public class GlobalClient : Zeze.Net.Service
    {

    }
}
