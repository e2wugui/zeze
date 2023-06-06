// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class WalkKey extends Zeze.Raft.RaftRpc<Zeze.Builtin.Dbh2.BWalk.Data, Zeze.Builtin.Dbh2.BWalkKeyResult.Data> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = 1484217124;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47357793622820
    static { register(TypeId_, WalkKey.class); }

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

    public WalkKey() {
        Argument = new Zeze.Builtin.Dbh2.BWalk.Data();
        Result = new Zeze.Builtin.Dbh2.BWalkKeyResult.Data();
    }

    public WalkKey(Zeze.Builtin.Dbh2.BWalk.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BWalkKeyResult.Data();
    }
}
