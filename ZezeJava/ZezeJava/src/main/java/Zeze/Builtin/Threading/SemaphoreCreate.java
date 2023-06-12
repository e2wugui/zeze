// auto-generated @formatter:off
package Zeze.Builtin.Threading;

// 这个rpc实际上没有使用参数TimeoutMs
public class SemaphoreCreate extends Zeze.Net.Rpc<Zeze.Builtin.Threading.BSemaphore.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11030;
    public static final int ProtocolId_ = -26296900; // 4268670396
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47377757945276
    static { register(TypeId_, SemaphoreCreate.class); }

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

    public SemaphoreCreate() {
        Argument = new Zeze.Builtin.Threading.BSemaphore.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public SemaphoreCreate(Zeze.Builtin.Threading.BSemaphore.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
