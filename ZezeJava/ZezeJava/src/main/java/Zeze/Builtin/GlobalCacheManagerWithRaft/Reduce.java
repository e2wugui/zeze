// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

public class Reduce extends Zeze.Raft.RaftRpc<Zeze.Builtin.GlobalCacheManagerWithRaft.BReduceParam, Zeze.Builtin.GlobalCacheManagerWithRaft.BReduceParam> {
    public static final int ModuleId_ = 11001;
    public static final int ProtocolId_ = 1451302739;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47250386526035
    static { register(TypeId_, Reduce.class); }

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

    public Reduce() {
        Argument = new Zeze.Builtin.GlobalCacheManagerWithRaft.BReduceParam();
        Result = new Zeze.Builtin.GlobalCacheManagerWithRaft.BReduceParam();
    }

    public Reduce(Zeze.Builtin.GlobalCacheManagerWithRaft.BReduceParam arg) {
        Argument = arg;
        Result = new Zeze.Builtin.GlobalCacheManagerWithRaft.BReduceParam();
    }
}
