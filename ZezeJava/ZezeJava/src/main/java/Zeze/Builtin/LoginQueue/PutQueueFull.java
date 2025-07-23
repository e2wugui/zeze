// auto-generated @formatter:off
package Zeze.Builtin.LoginQueue;

public class PutQueueFull extends Zeze.Net.Protocol<Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11043;
    public static final int ProtocolId_ = -1331617396; // 2963349900
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47432287199628
    static { register(TypeId_, PutQueueFull.class); }

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

    public PutQueueFull() {
        Argument = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
