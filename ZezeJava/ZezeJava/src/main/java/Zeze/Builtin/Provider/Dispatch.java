// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public class Dispatch extends Zeze.Net.Protocol<Zeze.Builtin.Provider.BDispatch> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = 1285307417;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47280285301785

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Dispatch() {
        Argument = new Zeze.Builtin.Provider.BDispatch();
    }

    public Dispatch(Zeze.Builtin.Provider.BDispatch arg) {
        Argument = arg;
    }
}
