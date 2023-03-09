// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class BeginTransaction extends Zeze.Raft.RaftRpc<Zeze.Builtin.Dbh2.BBeginTransactionArgumentDaTa, Zeze.Builtin.Dbh2.BBeginTransactionResultDaTa> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = -480216509; // 3814750787
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47360124156483

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

    public BeginTransaction() {
        Argument = new Zeze.Builtin.Dbh2.BBeginTransactionArgumentDaTa();
        Result = new Zeze.Builtin.Dbh2.BBeginTransactionResultDaTa();
    }

    public BeginTransaction(Zeze.Builtin.Dbh2.BBeginTransactionArgumentDaTa arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BBeginTransactionResultDaTa();
    }
}
