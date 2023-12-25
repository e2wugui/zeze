// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

public class Commit extends Zeze.Net.Rpc<Zeze.Builtin.HotDistribute.BCommit.Data, Zeze.Builtin.HotDistribute.BCommitResult.Data> {
    public static final int ModuleId_ = 11033;
    public static final int ProtocolId_ = -32813597; // 4262153699
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47390636330467
    static { register(TypeId_, Commit.class); }

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

    public Commit() {
        Argument = new Zeze.Builtin.HotDistribute.BCommit.Data();
        Result = new Zeze.Builtin.HotDistribute.BCommitResult.Data();
    }

    public Commit(Zeze.Builtin.HotDistribute.BCommit.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.HotDistribute.BCommitResult.Data();
    }
}
