// auto-generated @formatter:off
package demo.Module1;

public class ProtocolUseData extends Zeze.Net.Protocol<demo.Module1.BValue.Data> {
    public static final int ModuleId_ = 1;
    public static final int ProtocolId_ = 2084619608;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 6379586904
    static { register(TypeId_, ProtocolUseData.class); }

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

    @Override
    public int getCriticalLevel() {
        return eSheddable;
    }

    public ProtocolUseData() {
        Argument = new demo.Module1.BValue.Data();
    }

    public ProtocolUseData(demo.Module1.BValue.Data arg) {
        Argument = arg;
    }
}
