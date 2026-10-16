// auto-generated @formatter:off
package demo.Module1;

// protocol的注释
public class Protocol4 extends Zeze.Net.Protocol<demo.Module1.BValue> {
    public static final int ModuleId_ = 1;
    public static final int ProtocolId_ = 927897233;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 5222864529
    static { register(TypeId_, Protocol4.class); }

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

    public Protocol4() {
        Argument = new demo.Module1.BValue();
    }

    public Protocol4(demo.Module1.BValue arg) {
        Argument = arg;
    }
}
