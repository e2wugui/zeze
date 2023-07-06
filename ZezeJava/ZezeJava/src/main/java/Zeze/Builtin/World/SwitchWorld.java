// auto-generated @formatter:off
package Zeze.Builtin.World;

// mapId==-1，进入地图由服务器控制，此时仅仅表示客户端准备好进入地图了。
public class SwitchWorld extends Zeze.Net.Protocol<Zeze.Builtin.World.BSwitchWorld.Data> {
    public static final int ModuleId_ = 11031;
    public static final int ProtocolId_ = 1558632955;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47379342875131
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
        Argument = new Zeze.Builtin.World.BSwitchWorld.Data();
    }

    public SwitchWorld(Zeze.Builtin.World.BSwitchWorld.Data arg) {
        Argument = arg;
    }
}
