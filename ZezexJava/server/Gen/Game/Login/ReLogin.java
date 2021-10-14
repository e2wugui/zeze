// auto-generated
package Game.Login;

public class ReLogin extends Zeze.Net.Rpc<Game.Login.BReLogin, Zeze.Transaction.EmptyBean> {
    public final static int ModuleId_ = 1;
    public final static int ProtocolId_ = 43107;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ReLogin() {
        Argument = new Game.Login.BReLogin();
        Result = new Zeze.Transaction.EmptyBean();
    }

}
