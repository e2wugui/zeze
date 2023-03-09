// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class CommitTransaction extends Zeze.Raft.RaftRpc<Zeze.Builtin.Dbh2.BCommitTransactionArgumentDaTa, Zeze.Builtin.Dbh2.BCommitTransactionResultDaTa> {
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
        Argument = new Zeze.Builtin.Dbh2.BCommitTransactionArgumentDaTa();
        Result = new Zeze.Builtin.Dbh2.BCommitTransactionResultDaTa();
    }

    public CommitTransaction(Zeze.Builtin.Dbh2.BCommitTransactionArgumentDaTa arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BCommitTransactionResultDaTa();
    }
}
