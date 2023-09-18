

using Zeze.Net;
using Zeze.Serialize;
using Zeze.Transaction;
using Zeze.Util;

namespace Zege
{
    public sealed partial class ClientService
    {
        private static readonly ILogger logger = LogManager.GetLogger();

        // 网络查询结果
        public override void DispatchProtocol(Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (factoryHandle.Mode == DispatchMode.UIThread)
            {
                MainThread.BeginInvokeOnMainThread(
                    async () => await Mission.CallAsync(() => factoryHandle.Handle(p), (code) => p.TrySendResultCode(code)));
            }
            else
            {
                _ = Mission.CallAsync(() => factoryHandle.Handle(p));
            }
        }

        public override void DispatchRpcResponse(Protocol rpc, Func<Protocol, Task<long>> responseHandle, ProtocolFactoryHandle factoryHandle)
        {
            if (factoryHandle.Mode == DispatchMode.UIThread)
            {
                MainThread.BeginInvokeOnMainThread(
                    async () => await Mission.CallAsync(() => responseHandle(rpc)));
            }
            else
            {
                _ = Mission.CallAsync(() => responseHandle(rpc));
            }
        }

        public override void DispatchProtocol2(object key, Protocol p, ProtocolFactoryHandle factoryHandle)
        {
            if (factoryHandle.Mode == DispatchMode.UIThread)
            {
                MainThread.BeginInvokeOnMainThread(
                    async () => await Mission.CallAsync(() => factoryHandle.Handle(p), (code) => p.TrySendResultCode(code)));
            }
            else
            {
                _ = Mission.CallAsync(() => factoryHandle.Handle(p));
            }
        }

        public override void DispatchUnknownProtocol(AsyncSocket so, int moduleId, int protocolId, ByteBuffer data)
        {
            // 客户端忽略不认识的协议。
            // 仅仅记录一个日志。
            logger.Warn($"DispatchUnknownProtocol Module={moduleId} Protocol={protocolId} Size={data.Size}");
        }

        protected override void OnSendKeepAlive(AsyncSocket socket)
        {
            global::Zeze.Services.Handshake.KeepAlive.Instance.Send(socket); // skip result.
        }
    }
}
