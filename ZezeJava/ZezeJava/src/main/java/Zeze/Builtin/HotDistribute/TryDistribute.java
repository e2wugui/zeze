// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

public class TryDistribute extends Zeze.Net.Rpc<Zeze.Builtin.HotDistribute.BTryDistribute.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11033;
    public static final int ProtocolId_ = 2075503473;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47388449680241
    static { register(TypeId_, TryDistribute.class); }

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

    public TryDistribute() {
        Argument = new Zeze.Builtin.HotDistribute.BTryDistribute.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public TryDistribute(Zeze.Builtin.HotDistribute.BTryDistribute.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
