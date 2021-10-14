// auto-generated
package Game.Login;

public class Login extends Zeze.Net.Rpc<Game.Login.BLogin, Zeze.Transaction.EmptyBean> {
    public final static int ModuleId_ = 1;
    public final static int ProtocolId_ = 17788;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Login() {
        Argument = new Game.Login.BLogin();
        Result = new Zeze.Transaction.EmptyBean();
    }

}
