// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public class NewSession extends Zeze.Net.Rpc<Zeze.Builtin.LogService.BNewSession.Data, Zeze.Builtin.LogService.BSession.Data> {
    public static final int ModuleId_ = 11035;
    public static final int ProtocolId_ = -549094422; // 3745872874
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47398709984234
    static { register(TypeId_, NewSession.class); }

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

    public NewSession() {
        Argument = new Zeze.Builtin.LogService.BNewSession.Data();
        Result = new Zeze.Builtin.LogService.BSession.Data();
    }

    public NewSession(Zeze.Builtin.LogService.BNewSession.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.LogService.BSession.Data();
    }
}
