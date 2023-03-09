// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class Delete extends Zeze.Raft.RaftRpc<Zeze.Builtin.Dbh2.BDeleteArgumentDaTa, Zeze.Builtin.Dbh2.BDeleteResultDaTa> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = -367775506; // 3927191790
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47360236597486

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

    public Delete() {
        Argument = new Zeze.Builtin.Dbh2.BDeleteArgumentDaTa();
        Result = new Zeze.Builtin.Dbh2.BDeleteResultDaTa();
    }

    public Delete(Zeze.Builtin.Dbh2.BDeleteArgumentDaTa arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BDeleteResultDaTa();
    }
}
