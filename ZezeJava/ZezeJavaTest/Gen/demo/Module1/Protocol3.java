// auto-generated @formatter:off
package demo.Module1;

// protocol的注释
public class Protocol3 extends Zeze.Net.Protocol<demo.Module2.BValue> {
    public static final int ModuleId_ = 1;
    public static final int ProtocolId_ = -774467372; // 3520499924
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 7815467220
    static { register(TypeId_, Protocol3.class); }

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
        return eCritical;
    }

    public Protocol3() {
        Argument = new demo.Module2.BValue();
    }

    public Protocol3(demo.Module2.BValue arg) {
        Argument = arg;
    }
}
