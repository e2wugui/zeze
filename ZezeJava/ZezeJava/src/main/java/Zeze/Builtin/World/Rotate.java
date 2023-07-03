// auto-generated @formatter:off
package Zeze.Builtin.World;

// 同步原地转身。
public class Rotate extends Zeze.Net.Protocol<Zeze.Builtin.World.BRotate.Data> {
    public static final int ModuleId_ = 11031;
    public static final int ProtocolId_ = 13862247;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47377798104423
    static { register(TypeId_, Rotate.class); }

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

    public Rotate() {
        Argument = new Zeze.Builtin.World.BRotate.Data();
    }

    public Rotate(Zeze.Builtin.World.BRotate.Data arg) {
        Argument = arg;
    }
}
