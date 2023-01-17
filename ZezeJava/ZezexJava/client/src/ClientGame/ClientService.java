package ClientGame;

import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Util.Task;

public class ClientService extends ClientServiceBase {
    public ClientService(Zeze.Application zeze) throws Exception {
        super(zeze);
    }
    // 重载需要的方法。
    @Override
    public <P extends Protocol<?>> void DispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
                                                            ProtocolFactoryHandle<?> factoryHandle) throws Exception {
        Task.runRpcResponse(() -> responseHandle.handle(rpc), rpc, factoryHandle.Mode);
    }

    @Override
    public final <P extends Protocol<?>> void dispatchProtocol2(Object key, P p, ProtocolFactoryHandle<P> factoryHandle) {
        getZeze().getTaskOneByOneByKey().Execute(key,
                () -> Task.call(() -> factoryHandle.Handle.handle(p), p, Protocol::trySendResultCode),
                factoryHandle.Mode);
    }

    @Override
    public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) throws Exception {
        ProtocolHandle<P> handle = factoryHandle.Handle;
        Task.run(() -> handle.handle(p), p, null, null, factoryHandle.Mode);
    }
}
