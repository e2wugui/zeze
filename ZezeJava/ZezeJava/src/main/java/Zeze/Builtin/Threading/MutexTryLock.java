// auto-generated @formatter:off
package Zeze.Builtin.Threading;

public class MutexTryLock extends Zeze.Net.Rpc<Zeze.Builtin.Threading.BMutex.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11030;
    public static final int ProtocolId_ = -2142078474; // 2152888822
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47375642163702
    static { register(TypeId_, MutexTryLock.class); }

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

    public MutexTryLock() {
        Argument = new Zeze.Builtin.Threading.BMutex.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public MutexTryLock(Zeze.Builtin.Threading.BMutex.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
