// auto-generated @formatter:off
package metagame.builtin.World;

public class Command extends Zeze.Net.Protocol<metagame.builtin.World.BCommand.Data> {
    public static final int ModuleId_ = 10002;
    public static final int ProtocolId_ = -2140454993; // 2154512303
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 42960417406895
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
        Argument = new metagame.builtin.World.BCommand.Data();
    }

    public Command(metagame.builtin.World.BCommand.Data arg) {
        Argument = arg;
    }
}
