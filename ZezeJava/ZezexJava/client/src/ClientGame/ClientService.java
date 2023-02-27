package ClientGame;

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
    public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) {
        var p = decodeProtocol(typeId, bb, factoryHandle, so);
        Task.run(() -> p.handle(this, factoryHandle), p, null, null, factoryHandle.Mode);
    }
}
