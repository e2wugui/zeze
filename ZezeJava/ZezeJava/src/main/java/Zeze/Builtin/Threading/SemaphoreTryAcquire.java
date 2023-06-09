// auto-generated @formatter:off
package Zeze.Builtin.Threading;

public class SemaphoreTryAcquire extends Zeze.Net.Rpc<Zeze.Builtin.Threading.BSemaphore.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11030;
    public static final int ProtocolId_ = -1376799372; // 2918167924
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47376407442804
    static { register(TypeId_, SemaphoreTryAcquire.class); }

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

    public SemaphoreTryAcquire() {
        Argument = new Zeze.Builtin.Threading.BSemaphore.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public SemaphoreTryAcquire(Zeze.Builtin.Threading.BSemaphore.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
