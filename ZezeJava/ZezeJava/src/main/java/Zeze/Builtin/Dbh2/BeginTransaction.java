// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class BeginTransaction extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.BBeginTransactionArgument, Zeze.Builtin.Dbh2.BBeginTransactionResult> {
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
        Argument = new Zeze.Builtin.Dbh2.BBeginTransactionArgument();
        Result = new Zeze.Builtin.Dbh2.BBeginTransactionResult();
    }

    public BeginTransaction(Zeze.Builtin.Dbh2.BBeginTransactionArgument arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BBeginTransactionResult();
    }
}
