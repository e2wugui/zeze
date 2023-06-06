// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class Walk extends Zeze.Raft.RaftRpc<Zeze.Builtin.Dbh2.BWalk.Data, Zeze.Builtin.Dbh2.BWalkResult.Data> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = 557053487;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47356866459183
    static { register(TypeId_, Walk.class); }

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

    public Walk() {
        Argument = new Zeze.Builtin.Dbh2.BWalk.Data();
        Result = new Zeze.Builtin.Dbh2.BWalkResult.Data();
    }

    public Walk(Zeze.Builtin.Dbh2.BWalk.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BWalkResult.Data();
    }
}
