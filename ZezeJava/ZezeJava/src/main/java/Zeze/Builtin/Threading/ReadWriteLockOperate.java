// auto-generated @formatter:off
package Zeze.Builtin.Threading;

public class ReadWriteLockOperate extends Zeze.Net.Rpc<Zeze.Builtin.Threading.BReadWriteLock.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11030;
    public static final int ProtocolId_ = -923258741; // 3371708555
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47376860983435
    static { register(TypeId_, ReadWriteLockOperate.class); }

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

    public ReadWriteLockOperate() {
        Argument = new Zeze.Builtin.Threading.BReadWriteLock.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public ReadWriteLockOperate(Zeze.Builtin.Threading.BReadWriteLock.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
