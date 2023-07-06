// auto-generated @formatter:off
package Zeze.Builtin.World;

public class EnterWorld extends Zeze.Net.Protocol<Zeze.Builtin.World.BEnterWorld.Data> {
    public static final int ModuleId_ = 11031;
    public static final int ProtocolId_ = 1235871388;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47379020113564
    static { register(TypeId_, EnterWorld.class); }

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

    public EnterWorld() {
        Argument = new Zeze.Builtin.World.BEnterWorld.Data();
    }

    public EnterWorld(Zeze.Builtin.World.BEnterWorld.Data arg) {
        Argument = arg;
    }
}
