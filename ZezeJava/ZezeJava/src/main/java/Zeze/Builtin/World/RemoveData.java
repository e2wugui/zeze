// auto-generated @formatter:off
package Zeze.Builtin.World;

public class RemoveData extends Zeze.Net.Protocol<Zeze.Builtin.World.BRemoveData.Data> {
    public static final int ModuleId_ = 11031;
    public static final int ProtocolId_ = 1598013393;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47379382255569
    static { register(TypeId_, RemoveData.class); }

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

    public RemoveData() {
        Argument = new Zeze.Builtin.World.BRemoveData.Data();
    }

    public RemoveData(Zeze.Builtin.World.BRemoveData.Data arg) {
        Argument = arg;
    }
}
