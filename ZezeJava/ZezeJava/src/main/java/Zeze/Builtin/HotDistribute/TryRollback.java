// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

public class TryRollback extends Zeze.Net.Rpc<Zeze.Builtin.HotDistribute.BDistributeId.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11033;
    public static final int ProtocolId_ = 81474205;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47386455650973
    static { register(TypeId_, TryRollback.class); }

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

    public TryRollback() {
        Argument = new Zeze.Builtin.HotDistribute.BDistributeId.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public TryRollback(Zeze.Builtin.HotDistribute.BDistributeId.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
