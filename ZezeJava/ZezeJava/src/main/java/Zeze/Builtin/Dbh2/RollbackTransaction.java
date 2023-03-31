// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class RollbackTransaction extends Zeze.Raft.RaftRpc<Zeze.Builtin.Dbh2.BRollbackTransactionArgument.Data, Zeze.Builtin.Dbh2.BRollbackTransactionResult.Data> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = -323506902; // 3971460394
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47360280866090
    static { register(TypeId_, RollbackTransaction.class); }

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

    public RollbackTransaction() {
        Argument = new Zeze.Builtin.Dbh2.BRollbackTransactionArgument.Data();
        Result = new Zeze.Builtin.Dbh2.BRollbackTransactionResult.Data();
    }

    public RollbackTransaction(Zeze.Builtin.Dbh2.BRollbackTransactionArgument.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BRollbackTransactionResult.Data();
    }
}
