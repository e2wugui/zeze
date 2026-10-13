// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

public class CommitService extends Zeze.Net.Rpc<Zeze.Builtin.Zoker.BCommitService.Data, Zeze.Builtin.Zoker.BCommitServiceResult.Data> {
    public static final int ModuleId_ = 11037;
    public static final int ProtocolId_ = -1267193119; // 3027774177
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47406581820129
    static { register(TypeId_, CommitService.class); }

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

    public CommitService() {
        Argument = new Zeze.Builtin.Zoker.BCommitService.Data();
        Result = new Zeze.Builtin.Zoker.BCommitServiceResult.Data();
    }

    public CommitService(Zeze.Builtin.Zoker.BCommitService.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Zoker.BCommitServiceResult.Data();
    }
}
