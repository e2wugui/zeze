// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class KeepAlive extends Zeze.Raft.RaftRpc<Zeze.Transaction.EmptyBean, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 2096518282;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47341226054794
    static { register(TypeId_, KeepAlive.class); }

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

    public KeepAlive() {
        Argument = Zeze.Transaction.EmptyBean.instance;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
