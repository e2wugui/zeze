// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

public class Acquire extends Zeze.Raft.RaftRpc<Zeze.Builtin.GlobalCacheManagerWithRaft.BAcquireParam, Zeze.Builtin.GlobalCacheManagerWithRaft.BReduceParam> {
    public static final int ModuleId_ = 11001;
    public static final int ProtocolId_ = -1825434690; // 2469532606
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47251404755902
    static { register(TypeId_, Acquire.class); }

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

    public Acquire() {
        Argument = new Zeze.Builtin.GlobalCacheManagerWithRaft.BAcquireParam();
        Result = new Zeze.Builtin.GlobalCacheManagerWithRaft.BReduceParam();
    }

    public Acquire(Zeze.Builtin.GlobalCacheManagerWithRaft.BAcquireParam arg) {
        Argument = arg;
        Result = new Zeze.Builtin.GlobalCacheManagerWithRaft.BReduceParam();
    }
}
