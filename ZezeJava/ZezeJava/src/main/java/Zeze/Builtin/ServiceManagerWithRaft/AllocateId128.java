// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class AllocateId128 extends Zeze.Raft.RaftRpc<Zeze.Services.ServiceManager.BAllocateId128Argument, Zeze.Services.ServiceManager.BAllocateId128Result> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = -1847248875; // 2447718421
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47341577254933
    static { register(TypeId_, AllocateId128.class); }

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

    public AllocateId128() {
        Argument = new Zeze.Services.ServiceManager.BAllocateId128Argument();
        Result = new Zeze.Services.ServiceManager.BAllocateId128Result();
    }

    public AllocateId128(Zeze.Services.ServiceManager.BAllocateId128Argument arg) {
        Argument = arg;
        Result = new Zeze.Services.ServiceManager.BAllocateId128Result();
    }
}
