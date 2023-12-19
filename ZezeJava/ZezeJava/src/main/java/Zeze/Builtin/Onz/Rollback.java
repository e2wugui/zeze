// auto-generated @formatter:off
package Zeze.Builtin.Onz;

public class Rollback extends Zeze.Net.Rpc<Zeze.Builtin.Onz.BCommit.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11038;
    public static final int ProtocolId_ = -2031131886; // 2263835410
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47410112848658
    static { register(TypeId_, Rollback.class); }

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

    public Rollback() {
        Argument = new Zeze.Builtin.Onz.BCommit.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Rollback(Zeze.Builtin.Onz.BCommit.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
