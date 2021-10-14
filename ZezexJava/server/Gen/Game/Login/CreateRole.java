// auto-generated
package Game.Login;

public class CreateRole extends Zeze.Net.Rpc<Game.Login.BRole, Zeze.Transaction.EmptyBean> {
    public final static int ModuleId_ = 1;
    public final static int ProtocolId_ = 42558;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public CreateRole() {
        Argument = new Game.Login.BRole();
        Result = new Zeze.Transaction.EmptyBean();
    }

}
