// auto-generated @formatter:off
package Zeze.Builtin.World;

public class PutData extends Zeze.Net.Protocol<Zeze.Builtin.World.BPutData.Data> {
    public static final int ModuleId_ = 11031;
    public static final int ProtocolId_ = -768370218; // 3526597078
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47381310839254
    static { register(TypeId_, PutData.class); }

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

    public PutData() {
        Argument = new Zeze.Builtin.World.BPutData.Data();
    }

    public PutData(Zeze.Builtin.World.BPutData.Data arg) {
        Argument = arg;
    }
}
