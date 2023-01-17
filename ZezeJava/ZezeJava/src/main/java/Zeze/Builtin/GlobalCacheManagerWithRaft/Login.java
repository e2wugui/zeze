// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

public class Login extends Zeze.Raft.RaftRpc<Zeze.Builtin.GlobalCacheManagerWithRaft.BLoginParam, Zeze.Builtin.GlobalCacheManagerWithRaft.BAchillesHeelConfig> {
    public static final int ModuleId_ = 11001;
    public static final int ProtocolId_ = -1968616174; // 2326351122
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47251261574418

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

    public Login() {
        Argument = new Zeze.Builtin.GlobalCacheManagerWithRaft.BLoginParam();
        Result = new Zeze.Builtin.GlobalCacheManagerWithRaft.BAchillesHeelConfig();
    }

    public Login(Zeze.Builtin.GlobalCacheManagerWithRaft.BLoginParam arg) {
        Argument = arg;
        Result = new Zeze.Builtin.GlobalCacheManagerWithRaft.BAchillesHeelConfig();
    }
}
