// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class CommitTransaction extends Zeze.Raft.RaftRpc<Zeze.Builtin.Dbh2.BCommitTransactionArgument, Zeze.Builtin.Dbh2.BCommitTransactionResult> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = -1482242027; // 2812725269
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47359122130965

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

    public CommitTransaction() {
        Argument = new Zeze.Builtin.Dbh2.BCommitTransactionArgument();
        Result = new Zeze.Builtin.Dbh2.BCommitTransactionResult();
    }

    public CommitTransaction(Zeze.Builtin.Dbh2.BCommitTransactionArgument arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BCommitTransactionResult();
    }
}
