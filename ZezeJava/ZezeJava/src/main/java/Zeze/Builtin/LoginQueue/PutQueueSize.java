// auto-generated @formatter:off
package Zeze.Builtin.LoginQueue;

/*
			客户端收到这条协议之前，显示排队中，数量显示大于10000
			LoginQueue服务器最多只会广播通知前10000个客户端。
			客户端收到这条协议，就更新显示的排队数量。排队时，每N秒更新一次。
*/
public class PutQueueSize extends Zeze.Net.Protocol<Zeze.Builtin.LoginQueue.BQueueSize.Data> {
    public static final int ModuleId_ = 11043;
    public static final int ProtocolId_ = -1521970333; // 2772996963
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47432096846691
    static { register(TypeId_, PutQueueSize.class); }

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    @Override
    public long getTypeId() {
        return TypeId_;
    }

    public PutQueueSize() {
        Argument = new Zeze.Builtin.LoginQueue.BQueueSize.Data();
    }

    public PutQueueSize(Zeze.Builtin.LoginQueue.BQueueSize.Data arg) {
        Argument = arg;
    }
}
