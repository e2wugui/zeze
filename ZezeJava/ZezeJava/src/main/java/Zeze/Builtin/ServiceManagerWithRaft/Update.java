// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class Update extends Zeze.Raft.RaftRpc<Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 1810779937;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47340940316449

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public static final int Success = 0;
    public static final int ServiceNotRegister = 1;
    public static final int ServerStateError = 2;
    public static final int ServiceIdentityNotExist = 3;
    public static final int ServiceNotSubscribe = 4;

    public Update() {
        Argument = new Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Update(Zeze.Builtin.ServiceManagerWithRaft.BServiceInfo arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
