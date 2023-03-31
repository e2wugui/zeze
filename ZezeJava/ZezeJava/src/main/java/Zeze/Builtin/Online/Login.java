// auto-generated @formatter:off
package Zeze.Builtin.Online;

// 登录角色
public class Login extends Zeze.Net.Rpc<Zeze.Builtin.Online.BLogin, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11100;
    public static final int ProtocolId_ = -1498951762; // 2796015534
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47676933001134
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
        Argument = new Zeze.Builtin.Online.BLogin();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Login(Zeze.Builtin.Online.BLogin arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
