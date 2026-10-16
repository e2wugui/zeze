// auto-generated @formatter:off
package Game.Login;

public class GetRoleList extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Game.Login.BRoles> {
    public static final int ModuleId_ = 1;
    public static final int ProtocolId_ = 256665687;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 4551632983
    static { register(TypeId_, GetRoleList.class); }

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

    public GetRoleList() {
        Argument = Zeze.Transaction.EmptyBean.instance;
        Result = new Game.Login.BRoles();
    }
}
