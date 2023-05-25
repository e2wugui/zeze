package Zege;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public class ClientService extends ClientServiceBase {
    public ClientService(Zeze.Application zeze) {
        super(zeze);
    }
    // 重载需要的方法。

    @Override
    public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) throws Exception {
        // 不支持事务
        var p = decodeProtocol(typeId, bb, factoryHandle, so);
        p.dispatch(this, factoryHandle);
    }

    @Override
    public void dispatchProtocol(@NotNull Protocol<?> p, @NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
        // 不支持事务
        Task.runUnsafe(() -> p.handle(this, factoryHandle),
                p, Protocol::trySendResultCode, null, factoryHandle.Mode);
    }

    @Override
    public <P extends Protocol<?>> void dispatchRpcResponse(@NotNull P rpc, @NotNull ProtocolHandle<P> responseHandle,
                                                            @NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
        // 不支持事务
        Task.runRpcResponseUnsafe(() -> responseHandle.handle(rpc), rpc, factoryHandle.Mode);
    }

    @Override
    public void OnSocketClose(AsyncSocket so, Throwable e) throws Exception {
        super.OnSocketClose(so, e);
        if (null != e)
            e.printStackTrace();
        System.out.println("OnSocketClose");
    }
}
