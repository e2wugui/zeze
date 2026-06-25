// auto-generated @formatter:off
package metagame.builtin.World.Static;

public class SwitchWorld extends Zeze.Net.Rpc<metagame.builtin.World.Static.BSwitchWorld.Data, metagame.builtin.World.Static.BSwitchWorldResult.Data> {
    public static final int ModuleId_ = 10003;
    public static final int ProtocolId_ = -1789474540; // 2505492756
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 42965063354644
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
        Argument = new metagame.builtin.World.Static.BSwitchWorld.Data();
        Result = new metagame.builtin.World.Static.BSwitchWorldResult.Data();
    }

    public SwitchWorld(metagame.builtin.World.Static.BSwitchWorld.Data arg) {
        Argument = arg;
        Result = new metagame.builtin.World.Static.BSwitchWorldResult.Data();
    }
}
