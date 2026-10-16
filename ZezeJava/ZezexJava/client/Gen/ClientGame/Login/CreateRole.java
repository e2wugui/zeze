// auto-generated @formatter:off
package ClientGame.Login;

public class CreateRole extends Zeze.Net.Rpc<ClientGame.Login.BCreateRole, ClientGame.Login.BRole> {
    public static final int ModuleId_ = 1;
    public static final int ProtocolId_ = -270579231; // 4024388065
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 8319355361
    static { register(TypeId_, CreateRole.class); }

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

    public CreateRole() {
        Argument = new ClientGame.Login.BCreateRole();
        Result = new ClientGame.Login.BRole();
    }

    public CreateRole(ClientGame.Login.BCreateRole arg) {
        Argument = arg;
        Result = new ClientGame.Login.BRole();
    }
}
