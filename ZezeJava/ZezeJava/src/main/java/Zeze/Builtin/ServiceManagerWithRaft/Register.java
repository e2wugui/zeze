// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class Register extends Zeze.Raft.RaftRpc<Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 1511238554;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47340640775066

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public static final int Success = 0;
    public static final int DuplicateRegister = 1;

    public Register() {
        Argument = new Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Register(Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
