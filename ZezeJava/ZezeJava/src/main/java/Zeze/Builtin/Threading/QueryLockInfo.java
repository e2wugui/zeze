// auto-generated @formatter:off
package Zeze.Builtin.Threading;

public class QueryLockInfo extends Zeze.Net.Rpc<Zeze.Builtin.Threading.BQueryLockInfo.Data, Zeze.Builtin.Threading.BQueryLockInfo.Data> {
    public static final int ModuleId_ = 11030;
    public static final int ProtocolId_ = 118508697;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47373607783577
    static { register(TypeId_, QueryLockInfo.class); }

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

    public QueryLockInfo() {
        Argument = new Zeze.Builtin.Threading.BQueryLockInfo.Data();
        Result = new Zeze.Builtin.Threading.BQueryLockInfo.Data();
    }

    public QueryLockInfo(Zeze.Builtin.Threading.BQueryLockInfo.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Threading.BQueryLockInfo.Data();
    }
}
