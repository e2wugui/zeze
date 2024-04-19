// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class Subscribe extends Zeze.Raft.RaftRpc<Zeze.Services.ServiceManager.BSubscribeArgument, Zeze.Services.ServiceManager.BSubscribeResult> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 1141948215;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47340271484727
    static { register(TypeId_, Subscribe.class); }

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

    public static final int Success = 0;
    public static final int DuplicateSubscribe = 1;

    public Subscribe() {
        Argument = new Zeze.Services.ServiceManager.BSubscribeArgument();
        Result = new Zeze.Services.ServiceManager.BSubscribeResult();
    }

    public Subscribe(Zeze.Services.ServiceManager.BSubscribeArgument arg) {
        Argument = arg;
        Result = new Zeze.Services.ServiceManager.BSubscribeResult();
    }
}
