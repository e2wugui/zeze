// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

public class ReLogin extends Zeze.Raft.RaftRpc<Zeze.Builtin.GlobalCacheManagerWithRaft.BLoginParam, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11001;
    public static final int ProtocolId_ = -1422572442; // 2872394854
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47251807618150
    static { register(TypeId_, ReLogin.class); }

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

    public ReLogin() {
        Argument = new Zeze.Builtin.GlobalCacheManagerWithRaft.BLoginParam();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public ReLogin(Zeze.Builtin.GlobalCacheManagerWithRaft.BLoginParam arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
