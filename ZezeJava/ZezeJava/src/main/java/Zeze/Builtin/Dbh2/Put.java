// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class Put extends Zeze.Raft.RaftRpc<Zeze.Builtin.Dbh2.BPutArgument, Zeze.Builtin.Dbh2.BPutResult> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = -915697573; // 3379269723
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47359688675419

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

    public Put() {
        Argument = new Zeze.Builtin.Dbh2.BPutArgument();
        Result = new Zeze.Builtin.Dbh2.BPutResult();
    }

    public Put(Zeze.Builtin.Dbh2.BPutArgument arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BPutResult();
    }
}
