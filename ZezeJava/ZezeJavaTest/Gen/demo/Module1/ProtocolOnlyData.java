// auto-generated @formatter:off
package demo.Module1;

public class ProtocolOnlyData extends Zeze.Net.Protocol<demo.Module1.BOnlyData> {
    public static final int ModuleId_ = 1;
    public static final int ProtocolId_ = -1918026873; // 2376940423
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 6671907719
    static { register(TypeId_, ProtocolOnlyData.class); }

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

    public ProtocolOnlyData() {
        Argument = new demo.Module1.BOnlyData();
    }

    public ProtocolOnlyData(demo.Module1.BOnlyData arg) {
        Argument = arg;
    }
}
