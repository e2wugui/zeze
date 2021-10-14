// auto-generated
package Game.Login;

public class GetRoleList extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Game.Login.BRoles> {
    public final static int ModuleId_ = 1;
    public final static int ProtocolId_ = 26395;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public GetRoleList() {
        Argument = new Zeze.Transaction.EmptyBean();
        Result = new Game.Login.BRoles();
    }

}
