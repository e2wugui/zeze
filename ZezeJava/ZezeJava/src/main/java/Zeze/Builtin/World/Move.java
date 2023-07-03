// auto-generated @formatter:off
package Zeze.Builtin.World;

// 最简单的移动协议，发给第三方也是用相同的协议，参数可能会被服务器调整。
public class Move extends Zeze.Net.Protocol<Zeze.Builtin.World.BMove.Data> {
    public static final int ModuleId_ = 11031;
    public static final int ProtocolId_ = 1967237043;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47379751479219
    static { register(TypeId_, Move.class); }

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

    public Move() {
        Argument = new Zeze.Builtin.World.BMove.Data();
    }

    public Move(Zeze.Builtin.World.BMove.Data arg) {
        Argument = arg;
    }
}
