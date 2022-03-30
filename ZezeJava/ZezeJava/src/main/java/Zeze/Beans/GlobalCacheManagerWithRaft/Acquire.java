// auto-generated @formatter:off
package Zeze.Beans.GlobalCacheManagerWithRaft;

public class Acquire extends Zeze.Raft.RaftRpc<Zeze.Beans.GlobalCacheManagerWithRaft.AcquireParam, Zeze.Beans.GlobalCacheManagerWithRaft.ReduceParam> {
    public static final int ModuleId_ = 11001;
    public static final int ProtocolId_ = -1471313076;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Acquire() {
        Argument = new Zeze.Beans.GlobalCacheManagerWithRaft.AcquireParam();
        Result = new Zeze.Beans.GlobalCacheManagerWithRaft.ReduceParam();
    }
}
