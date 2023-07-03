// auto-generated @formatter:off
package Zeze.Builtin.World;

public class Stop extends Zeze.Net.Protocol<Zeze.Builtin.World.BMove.Data> {
    public static final int ModuleId_ = 11031;
    public static final int ProtocolId_ = -1398851858; // 2896115438
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47380680357614
    static { register(TypeId_, Stop.class); }

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

    public Stop() {
        Argument = new Zeze.Builtin.World.BMove.Data();
    }

    public Stop(Zeze.Builtin.World.BMove.Data arg) {
        Argument = arg;
    }
}
