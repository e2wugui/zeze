// auto-generated @formatter:off
package demo.Module1;

public class ProtocolOnlyUseData extends Zeze.Net.Protocol<demo.Module1.BOnlyData> {
    public static final int ModuleId_ = 1;
    public static final int ProtocolId_ = -1027279294; // 3267688002
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 7562655298
    static { register(TypeId_, ProtocolOnlyUseData.class); }

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

    public ProtocolOnlyUseData() {
        Argument = new demo.Module1.BOnlyData();
    }

    public ProtocolOnlyUseData(demo.Module1.BOnlyData arg) {
        Argument = arg;
    }
}
