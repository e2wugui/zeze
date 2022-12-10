// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class OfflineRegister extends Zeze.Raft.RaftRpc<Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotify, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 1381638229;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47340511174741

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public OfflineRegister() {
        Argument = new Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotify();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public OfflineRegister(Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotify arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
