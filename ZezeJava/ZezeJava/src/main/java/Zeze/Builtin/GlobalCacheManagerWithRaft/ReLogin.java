// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

public class ReLogin extends Zeze.Raft.RaftRpc<Zeze.Builtin.GlobalCacheManagerWithRaft.BLoginParam, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11001;
    public static final int ProtocolId_ = -1422572442; // 2872394854
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47251807618150

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ReLogin() {
        Argument = new Zeze.Builtin.GlobalCacheManagerWithRaft.BLoginParam();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
