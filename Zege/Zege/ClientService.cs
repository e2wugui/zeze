

using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;

namespace Zege
{
    public sealed partial class ClientService
    {
        // 网络查询结果
        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (factoryHandle.Mode == DispatchMode.UIThread)
            {
                MainThread.BeginInvokeOnMainThread(
                    async () => await Mission.CallAsync(factoryHandle.Handle, p, (p2, code) => p2.TrySendResultCode(code)));
            }
            else
            {
                base.DispatchProtocol(p, factoryHandle);
            }
        }

        public override void DispatchRpcResponse(Protocol rpc, Func<Protocol, Task<long>> responseHandle, ProtocolFactoryHandle factoryHandle)
        {
            if (factoryHandle.Mode == DispatchMode.UIThread)
            {
                MainThread.BeginInvokeOnMainThread(
                    async () => await Mission.CallAsync(responseHandle, rpc));
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
                    async () => await Mission.CallAsync(factoryHandle.Handle, p, (p2, code) => p2.TrySendResultCode(code)));
            }
            else
            {
                base.DispatchProtocol2(key, p, factoryHandle);
            }
        }

        public override void DispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data)
        {
            // 客户端忽略不认识的协议。
            // TODO 记一下LOG吧。
        }
    }
}
