// auto-generated @formatter:off
package Game.Login;

public class CreateRole extends Zeze.Net.Rpc<Game.Login.BCreateRole, Game.Login.BRole> {
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
        Argument = new Game.Login.BCreateRole();
        Result = new Game.Login.BRole();
    }

    public CreateRole(Game.Login.BCreateRole arg) {
        Argument = arg;
        Result = new Game.Login.BRole();
    }
}
