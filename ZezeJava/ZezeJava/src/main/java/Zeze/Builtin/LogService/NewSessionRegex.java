// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public class NewSessionRegex extends Zeze.Net.Rpc<Zeze.Builtin.LogService.BRegex.Data, Zeze.Builtin.LogService.BSession.Data> {
    public static final int ModuleId_ = 11035;
    public static final int ProtocolId_ = 912521228;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47395876632588
    static { register(TypeId_, NewSessionRegex.class); }

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

    public NewSessionRegex() {
        Argument = new Zeze.Builtin.LogService.BRegex.Data();
        Result = new Zeze.Builtin.LogService.BSession.Data();
    }

    public NewSessionRegex(Zeze.Builtin.LogService.BRegex.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.LogService.BSession.Data();
    }
}
