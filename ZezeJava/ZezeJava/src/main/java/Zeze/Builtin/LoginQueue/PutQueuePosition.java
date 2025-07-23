// auto-generated @formatter:off
package Zeze.Builtin.LoginQueue;

/*
			客户端收到这条协议之前，显示排队中，数量显示大于10000
			LoginQueue服务器最多只会广播通知前10000个客户端。
			客户端收到这条协议，就更新显示的排队数量。排队时，每N秒更新一次。
*/
public class PutQueuePosition extends Zeze.Net.Protocol<Zeze.Builtin.LoginQueue.BQueuePosition.Data> {
    public static final int ModuleId_ = 11043;
    public static final int ProtocolId_ = -1003438419; // 3291528877
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47432615378605
    static { register(TypeId_, PutQueuePosition.class); }

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

    public PutQueuePosition() {
        Argument = new Zeze.Builtin.LoginQueue.BQueuePosition.Data();
    }

    public PutQueuePosition(Zeze.Builtin.LoginQueue.BQueuePosition.Data arg) {
        Argument = arg;
    }
}
