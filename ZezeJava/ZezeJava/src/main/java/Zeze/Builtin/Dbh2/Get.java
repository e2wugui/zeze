// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class Get extends Zeze.Raft.RaftRpc<Zeze.Builtin.Dbh2.BGetArgument, Zeze.Builtin.Dbh2.BGetResult> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = 529792484;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47356839198180

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

    public Get() {
        Argument = new Zeze.Builtin.Dbh2.BGetArgument();
        Result = new Zeze.Builtin.Dbh2.BGetResult();
    }

    public Get(Zeze.Builtin.Dbh2.BGetArgument arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BGetResult();
    }
}
