// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class AllocateId extends Zeze.Raft.RaftRpc<Zeze.Builtin.ServiceManagerWithRaft.BAllocateIdArgument, Zeze.Builtin.ServiceManagerWithRaft.BAllocateIdResult> {
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

    public AllocateId() {
        Argument = new Zeze.Builtin.ServiceManagerWithRaft.BAllocateIdArgument();
        Result = new Zeze.Builtin.ServiceManagerWithRaft.BAllocateIdResult();
    }

    public AllocateId(Zeze.Builtin.ServiceManagerWithRaft.BAllocateIdArgument arg) {
        Argument = arg;
        Result = new Zeze.Builtin.ServiceManagerWithRaft.BAllocateIdResult();
    }
}
