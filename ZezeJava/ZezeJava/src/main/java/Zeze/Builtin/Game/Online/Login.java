// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

public class Login extends Zeze.Net.Rpc<Zeze.Builtin.Game.Online.BLogin, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11013;
    public static final int ProtocolId_ = -789575265; // 3505392031
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47303980222879

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Login() {
        Argument = new Zeze.Builtin.Game.Online.BLogin();
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
