

using Zeze.Util;
using Zeze.Net;
using System.Threading.Tasks;
using System;

namespace ClientGame
{
    public sealed partial class ClientService
    {
        // 重载需要的方法。
        public override void DispatchRpcResponse(Protocol rpc,
            Func<Protocol, Task<long>> responseHandle,
            ProtocolFactoryHandle factoryHandle)
        {
            _ = Mission.CallAsync(responseHandle, rpc, null);
        }

        public override void DispatchProtocol2(object key, Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            Zeze.TaskOneByOneByKey.Execute(key, factoryHandle.Handle, p, (p, code) => p.SendResultCode(code));
        }

        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            _ = Mission.CallAsync(factoryHandle.Handle, p, null);
        }
    }
}
