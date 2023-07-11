// auto-generated @formatter:off
package Zeze.Builtin.World.Static;

public class SwitchWorld extends Zeze.Net.Rpc<Zeze.Builtin.World.Static.BSwitchWorld.Data, Zeze.Builtin.World.Static.BSwitchWorldResult.Data> {
    public static final int ModuleId_ = 11032;
    public static final int ProtocolId_ = -553593857; // 3741373439
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47385820582911
    static { register(TypeId_, SwitchWorld.class); }

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

    public SwitchWorld() {
        Argument = new Zeze.Builtin.World.Static.BSwitchWorld.Data();
        Result = new Zeze.Builtin.World.Static.BSwitchWorldResult.Data();
    }

    public SwitchWorld(Zeze.Builtin.World.Static.BSwitchWorld.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.World.Static.BSwitchWorldResult.Data();
    }
}
