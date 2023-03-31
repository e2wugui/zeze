// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

// 登录角色
public class Login extends Zeze.Net.Rpc<Zeze.Builtin.Game.Online.BLogin, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11013;
    public static final int ProtocolId_ = -789575265; // 3505392031
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47303980222879
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
        Argument = new Zeze.Builtin.Game.Online.BLogin();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Login(Zeze.Builtin.Game.Online.BLogin arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
