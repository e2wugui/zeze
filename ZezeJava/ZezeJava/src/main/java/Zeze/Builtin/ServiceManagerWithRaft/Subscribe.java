// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class Subscribe extends Zeze.Raft.RaftRpc<Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfo, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 1141948215;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47340271484727

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Subscribe() {
        Argument = new Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfo();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Subscribe(Zeze.Builtin.ServiceManagerWithRaft.BSubscribeInfo arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
