package Zege;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Util.Task;

public class ClientService extends ClientServiceBase {
    public ClientService(Zeze.Application zeze) throws Throwable {
        super(zeze);
    }
    // 重载需要的方法。
    @Override
    public <P extends Protocol<?>> void DispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
                                                            ProtocolFactoryHandle<?> factoryHandle) throws Throwable {
        Task.runRpcResponse(() -> responseHandle.handle(rpc), rpc);
    }

    @Override
    public final <P extends Protocol<?>> void DispatchProtocol2(Object key, P p, ProtocolFactoryHandle<P> factoryHandle) {
        getZeze().getTaskOneByOneByKey().Execute(key,
                () -> Task.Call(() -> factoryHandle.Handle.handle(p), p, Protocol::trySendResultCode));
    }

    @Override
    public <P extends Protocol<?>> void DispatchProtocol(P p, ProtocolFactoryHandle<P> factoryHandle) throws Throwable {
        ProtocolHandle<P> handle = factoryHandle.Handle;
        Task.run(() -> handle.handle(p), p);
    }

    @Override
    public void OnSocketClose(AsyncSocket so, Throwable e) throws Throwable {
        super.OnSocketClose(so, e);
        if (null != e)
            e.printStackTrace();
        System.out.println("OnSocketClose");
    }
}
