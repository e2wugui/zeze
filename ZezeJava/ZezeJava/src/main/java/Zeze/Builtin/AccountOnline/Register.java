// auto-generated @formatter:off
package Zeze.Builtin.AccountOnline;

// 连上服务器马上发送，登记自己的名字。
public class Register extends Zeze.Net.Rpc<Zeze.Builtin.AccountOnline.BRegister.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11041;
    public static final int ProtocolId_ = -1979811585; // 2315155711
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47423049070847
    static { register(TypeId_, Register.class); }

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

    public Register() {
        Argument = new Zeze.Builtin.AccountOnline.BRegister.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Register(Zeze.Builtin.AccountOnline.BRegister.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
