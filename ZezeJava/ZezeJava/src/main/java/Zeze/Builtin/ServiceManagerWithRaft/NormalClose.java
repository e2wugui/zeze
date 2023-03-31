// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class NormalClose extends Zeze.Raft.RaftRpc<Zeze.Transaction.EmptyBean, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = -776632619; // 3518334677
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47342647871189
    static { register(TypeId_, NormalClose.class); }

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

    public NormalClose() {
        Argument = Zeze.Transaction.EmptyBean.instance;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
