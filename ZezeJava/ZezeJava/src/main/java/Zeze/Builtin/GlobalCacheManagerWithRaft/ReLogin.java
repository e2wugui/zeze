// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

public class ReLogin extends Zeze.Raft.RaftRpc<Zeze.Builtin.GlobalCacheManagerWithRaft.LoginParam, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11001;
    public static final int ProtocolId_ = -1422572442;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ReLogin() {
        Argument = new Zeze.Builtin.GlobalCacheManagerWithRaft.LoginParam();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
