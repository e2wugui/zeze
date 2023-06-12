// auto-generated @formatter:off
package Zeze.Builtin.Threading;

// 这个rpc实际上没有使用参数TimeoutMs
public class SemaphoreRelease extends Zeze.Net.Rpc<Zeze.Builtin.Threading.BSemaphore.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11030;
    public static final int ProtocolId_ = 885271930;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47374374546810
    static { register(TypeId_, SemaphoreRelease.class); }

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

    public SemaphoreRelease() {
        Argument = new Zeze.Builtin.Threading.BSemaphore.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public SemaphoreRelease(Zeze.Builtin.Threading.BSemaphore.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
