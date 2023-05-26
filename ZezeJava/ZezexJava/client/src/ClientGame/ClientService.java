package ClientGame;

import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

public class ClientService extends ClientServiceBase {
    public ClientService(Zeze.Application zeze) {
        super(zeze);
        setNoProcedure(true);
    }
    // 重载需要的方法。
}
