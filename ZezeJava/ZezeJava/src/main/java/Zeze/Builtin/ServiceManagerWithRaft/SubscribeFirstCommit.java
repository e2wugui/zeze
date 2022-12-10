// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class SubscribeFirstCommit extends Zeze.Raft.RaftRpc<Zeze.Builtin.ServiceManagerWithRaft.BServiceInfos, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = -636130961; // 3658836335
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47342788372847

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public SubscribeFirstCommit() {
        Argument = new Zeze.Builtin.ServiceManagerWithRaft.BServiceInfos();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public SubscribeFirstCommit(Zeze.Builtin.ServiceManagerWithRaft.BServiceInfos arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
