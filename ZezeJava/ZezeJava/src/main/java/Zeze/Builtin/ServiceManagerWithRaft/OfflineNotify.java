// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class OfflineNotify extends Zeze.Raft.RaftRpc<Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotify, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 1429001328;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47340558537840

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public OfflineNotify() {
        Argument = new Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotify();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public OfflineNotify(Zeze.Builtin.ServiceManagerWithRaft.BOfflineNotify arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
