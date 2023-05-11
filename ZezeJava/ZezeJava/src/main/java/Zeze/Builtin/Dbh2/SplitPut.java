// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class SplitPut extends Zeze.Raft.RaftRpc<Zeze.Builtin.Dbh2.BSplitPut.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = -1456158957; // 2838808339
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47359148214035
    static { register(TypeId_, SplitPut.class); }

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

    public SplitPut() {
        Argument = new Zeze.Builtin.Dbh2.BSplitPut.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public SplitPut(Zeze.Builtin.Dbh2.BSplitPut.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
