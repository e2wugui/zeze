// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

public class Cleanup extends Zeze.Raft.RaftRpc<Zeze.Builtin.GlobalCacheManagerWithRaft.AchillesHeel, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11001;
    public static final int ProtocolId_ = -73964423; // 4221002873
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47253156226169

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Cleanup() {
        Argument = new Zeze.Builtin.GlobalCacheManagerWithRaft.AchillesHeel();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
