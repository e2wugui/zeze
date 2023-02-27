package Zege;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;

public class ClientService extends ClientServiceBase {
    public ClientService(Zeze.Application zeze) {
        super(zeze);
    }
    // 重载需要的方法。

    @Override
    public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) throws Exception {
        var p = decodeProtocol(typeId, bb, factoryHandle, so);
        p.dispatch(this, factoryHandle);
    }

    @Override
    public void OnSocketClose(AsyncSocket so, Throwable e) throws Exception {
        super.OnSocketClose(so, e);
        if (null != e)
            e.printStackTrace();
        System.out.println("OnSocketClose");
    }
}
