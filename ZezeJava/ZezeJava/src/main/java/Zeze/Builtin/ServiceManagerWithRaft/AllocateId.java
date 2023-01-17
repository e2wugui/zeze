// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class AllocateId extends Zeze.Raft.RaftRpc<Zeze.Services.ServiceManager.BAllocateIdArgument, Zeze.Services.ServiceManager.BAllocateIdResult> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = -776297405; // 3518669891
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47342648206403

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

    public AllocateId() {
        Argument = new Zeze.Services.ServiceManager.BAllocateIdArgument();
        Result = new Zeze.Services.ServiceManager.BAllocateIdResult();
    }

    public AllocateId(Zeze.Services.ServiceManager.BAllocateIdArgument arg) {
        Argument = arg;
        Result = new Zeze.Services.ServiceManager.BAllocateIdResult();
    }
}
