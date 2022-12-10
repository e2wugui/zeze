// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class SetServerLoad extends Zeze.Raft.RaftRpc<Zeze.Builtin.ServiceManagerWithRaft.BServerLoad, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = -894675129; // 3400292167
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47342529828679

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public SetServerLoad() {
        Argument = new Zeze.Builtin.ServiceManagerWithRaft.BServerLoad();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public SetServerLoad(Zeze.Builtin.ServiceManagerWithRaft.BServerLoad arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
