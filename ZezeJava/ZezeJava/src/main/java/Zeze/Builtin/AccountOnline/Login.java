// auto-generated @formatter:off
package Zeze.Builtin.AccountOnline;

// auth过后，登录，判断唯一。
public class Login extends Zeze.Net.Rpc<Zeze.Builtin.AccountOnline.BLogin.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11041;
    public static final int ProtocolId_ = 992740300;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47421726655436
    static { register(TypeId_, Login.class); }

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
        Argument = new Zeze.Builtin.AccountOnline.BLogin.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Login(Zeze.Builtin.AccountOnline.BLogin.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
