// auto-generated @formatter:off
package Zeze.Builtin.Threading;

public class MutexUnlock extends Zeze.Net.Rpc<Zeze.Builtin.Threading.BLockName, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11030;
    public static final int ProtocolId_ = 769968098;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47374259242978
    static { register(TypeId_, MutexUnlock.class); }

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

    public MutexUnlock() {
        Argument = new Zeze.Builtin.Threading.BLockName();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public MutexUnlock(Zeze.Builtin.Threading.BLockName arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
