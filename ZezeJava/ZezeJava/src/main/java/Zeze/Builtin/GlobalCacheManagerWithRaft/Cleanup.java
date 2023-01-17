// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

public class Cleanup extends Zeze.Raft.RaftRpc<Zeze.Builtin.GlobalCacheManagerWithRaft.BAchillesHeel, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11001;
    public static final int ProtocolId_ = -73964423; // 4221002873
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47253156226169

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

    public Cleanup() {
        Argument = new Zeze.Builtin.GlobalCacheManagerWithRaft.BAchillesHeel();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Cleanup(Zeze.Builtin.GlobalCacheManagerWithRaft.BAchillesHeel arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
