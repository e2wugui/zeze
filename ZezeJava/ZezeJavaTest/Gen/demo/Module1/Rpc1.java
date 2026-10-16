// auto-generated @formatter:off
package demo.Module1;

// rpc的注释
public class Rpc1 extends Zeze.Net.Rpc<demo.Module1.BValue, demo.Module1.BValue> {
    public static final int ModuleId_ = 1;
    public static final int ProtocolId_ = 1340115327;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 5635082623
    static { register(TypeId_, Rpc1.class); }

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

    public Rpc1() {
        Argument = new demo.Module1.BValue();
        Result = new demo.Module1.BValue();
    }

    public Rpc1(demo.Module1.BValue arg) {
        Argument = arg;
        Result = new demo.Module1.BValue();
    }
}
