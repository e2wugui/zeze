// auto-generated @formatter:off
package demo.Module1;

// rpc的注释
public class Rpc2 extends Zeze.Net.Rpc<demo.Module1.BValue, demo.Module1.BValue> {
    public static final int ModuleId_ = 1;
    public static final int ProtocolId_ = -735856552; // 3559110744
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 7854078040
    static { register(TypeId_, Rpc2.class); }

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
        return eNormal;
    }

    public Rpc2() {
        Argument = new demo.Module1.BValue();
        Result = new demo.Module1.BValue();
    }

    public Rpc2(demo.Module1.BValue arg) {
        Argument = arg;
        Result = new demo.Module1.BValue();
    }
}
