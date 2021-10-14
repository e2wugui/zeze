// auto-generated
package Game.Login;

public class Logout extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Zeze.Transaction.EmptyBean> {
    public final static int ModuleId_ = 1;
    public final static int ProtocolId_ = 48433;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Logout() {
        Argument = new Zeze.Transaction.EmptyBean();
        Result = new Zeze.Transaction.EmptyBean();
    }

}
