

using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;

namespace Zege
{
    public sealed partial class ClientService
    {
        private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

        // 网络查询结果
        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (factoryHandle.Mode == DispatchMode.UIThread)
            {
                MainThread.BeginInvokeOnMainThread(
                    async () => await global::Zeze.Util.Mission.CallAsync(factoryHandle.Handle, p, (Protocol p2, long code) => p2.TrySendResultCode(code)));
            }
            else
            {
                _ = global::Zeze.Util.Mission.CallAsync(factoryHandle.Handle, p, (p2, code) => p2.TrySendResultCode(code));
            }
        }

        public override void DispatchRpcResponse(Protocol rpc, Func<Protocol, Task<long>> responseHandle, ProtocolFactoryHandle factoryHandle)
        {
            if (factoryHandle.Mode == DispatchMode.UIThread)
            {
                MainThread.BeginInvokeOnMainThread(
                    async () => await global::Zeze.Util.Mission.CallAsync(responseHandle, rpc));
            }
            else
            {
                base.DispatchRpcResponse(rpc, responseHandle, factoryHandle);
            }
        }

        public override void DispatchProtocol2(object key, Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (factoryHandle.Mode == DispatchMode.UIThread)
            {
                MainThread.BeginInvokeOnMainThread(
                    async () => await global::Zeze.Util.Mission.CallAsync(factoryHandle.Handle, p, (Protocol p2, long code) => p2.TrySendResultCode(code)));
            }
            else
            {
                _ = global::Zeze.Util.Mission.CallAsync(factoryHandle.Handle, p, (p2, code) => p2.TrySendResultCode(code));
            }
        }

        public override void DispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data)
        {
            // 客户端忽略不认识的协议。
            // 仅仅记录一个日志。
            logger.Warn($"DispatchUnknownProtocol Module={moduleId} Protocol={protocolId} Size={data.Size}");
        }
    }
}
