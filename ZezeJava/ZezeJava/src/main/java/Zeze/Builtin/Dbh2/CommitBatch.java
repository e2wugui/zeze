// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class CommitBatch extends Zeze.Raft.RaftRpc<Zeze.Builtin.Dbh2.BBatchTid.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = 740306824;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47357049712520
    static { register(TypeId_, CommitBatch.class); }

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

    public CommitBatch() {
        Argument = new Zeze.Builtin.Dbh2.BBatchTid.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public CommitBatch(Zeze.Builtin.Dbh2.BBatchTid.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
