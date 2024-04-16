// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

public class Commit2 extends Zeze.Net.Rpc<Zeze.Builtin.HotDistribute.BDistributeId.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11033;
    public static final int ProtocolId_ = 891403198;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47387265579966
    static { register(TypeId_, Commit2.class); }

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

    public Commit2() {
        Argument = new Zeze.Builtin.HotDistribute.BDistributeId.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Commit2(Zeze.Builtin.HotDistribute.BDistributeId.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
