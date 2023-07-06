// auto-generated @formatter:off
package Zeze.Builtin.World;

public class Command extends Zeze.Net.Protocol<Zeze.Builtin.World.BCommand.Data> {
    public static final int ModuleId_ = 11031;
    public static final int ProtocolId_ = 497549917;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47378281792093
    static { register(TypeId_, Command.class); }

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

    public Command() {
        Argument = new Zeze.Builtin.World.BCommand.Data();
    }

    public Command(Zeze.Builtin.World.BCommand.Data arg) {
        Argument = arg;
    }
}
