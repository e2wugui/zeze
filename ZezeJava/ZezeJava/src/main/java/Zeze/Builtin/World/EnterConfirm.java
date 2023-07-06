// auto-generated @formatter:off
package Zeze.Builtin.World;

public class EnterConfirm extends Zeze.Net.Protocol<Zeze.Builtin.World.BEnterConfirm.Data> {
    public static final int ModuleId_ = 11031;
    public static final int ProtocolId_ = 836633097;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47378620875273
    static { register(TypeId_, EnterConfirm.class); }

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

    public EnterConfirm() {
        Argument = new Zeze.Builtin.World.BEnterConfirm.Data();
    }

    public EnterConfirm(Zeze.Builtin.World.BEnterConfirm.Data arg) {
        Argument = arg;
    }
}
