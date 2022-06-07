// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

public class Reduce extends Zeze.Raft.RaftRpc<Zeze.Builtin.GlobalCacheManagerWithRaft.ReduceParam, Zeze.Builtin.GlobalCacheManagerWithRaft.ReduceParam> {
    public static final int ModuleId_ = 11001;
    public static final int ProtocolId_ = 1451302739;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47250386526035

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Reduce() {
        Argument = new Zeze.Builtin.GlobalCacheManagerWithRaft.ReduceParam();
        Result = new Zeze.Builtin.GlobalCacheManagerWithRaft.ReduceParam();
    }
}
