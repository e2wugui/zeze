// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class CommitServiceList extends Zeze.Raft.RaftRpc<Zeze.Builtin.ServiceManagerWithRaft.BServiceListVersion, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 920176378;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47340049712890

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public CommitServiceList() {
        Argument = new Zeze.Builtin.ServiceManagerWithRaft.BServiceListVersion();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public CommitServiceList(Zeze.Builtin.ServiceManagerWithRaft.BServiceListVersion arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
