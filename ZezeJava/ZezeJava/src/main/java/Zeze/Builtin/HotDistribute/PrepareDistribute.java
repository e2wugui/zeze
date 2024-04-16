// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

public class PrepareDistribute extends Zeze.Net.Rpc<Zeze.Builtin.HotDistribute.BDistributeId.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11033;
    public static final int ProtocolId_ = -1410145287; // 2884822009
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47389258998777
    static { register(TypeId_, PrepareDistribute.class); }

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

    public PrepareDistribute() {
        Argument = new Zeze.Builtin.HotDistribute.BDistributeId.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public PrepareDistribute(Zeze.Builtin.HotDistribute.BDistributeId.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
