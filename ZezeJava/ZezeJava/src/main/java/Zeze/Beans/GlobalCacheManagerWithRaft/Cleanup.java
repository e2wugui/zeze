// auto-generated @formatter:off
package Zeze.Beans.GlobalCacheManagerWithRaft;

public class Cleanup extends Zeze.Raft.RaftRpc<Zeze.Beans.GlobalCacheManagerWithRaft.AchillesHeel, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11001;
    public static final int ProtocolId_ = 754579307;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Cleanup() {
        Argument = new Zeze.Beans.GlobalCacheManagerWithRaft.AchillesHeel();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
