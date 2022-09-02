

using Zeze.Net;

namespace Zege
{
    public sealed partial class ClientService
    {
        // 网络查询结果
        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            MainThread.BeginInvokeOnMainThread(() => base.DispatchProtocol(p, factoryHandle));
        }

        public override void DispatchRpcResponse(Protocol rpc, Func<Protocol, Task<long>> responseHandle, ProtocolFactoryHandle factoryHandle)
        {
            MainThread.BeginInvokeOnMainThread(() => base.DispatchRpcResponse(rpc, responseHandle, factoryHandle));
        }

        public override void DispatchProtocol2(object key, Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            MainThread.BeginInvokeOnMainThread(() => base.DispatchProtocol2(key, p, factoryHandle));
        }
    }
}
