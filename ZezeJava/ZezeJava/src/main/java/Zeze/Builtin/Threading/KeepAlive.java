// auto-generated @formatter:off
package Zeze.Builtin.Threading;

// 由Client定时发送报告，Server发现AppSerialId发生变化或者超时，释放该ServerId所有的锁。
public class KeepAlive extends Zeze.Net.Protocol<Zeze.Builtin.Threading.BKeepAlive.Data> {
    public static final int ModuleId_ = 11030;
    public static final int ProtocolId_ = 1149398977;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47374638673857
    static { register(TypeId_, KeepAlive.class); }

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

    public KeepAlive() {
        Argument = new Zeze.Builtin.Threading.BKeepAlive.Data();
    }

    public KeepAlive(Zeze.Builtin.Threading.BKeepAlive.Data arg) {
        Argument = arg;
    }
}
