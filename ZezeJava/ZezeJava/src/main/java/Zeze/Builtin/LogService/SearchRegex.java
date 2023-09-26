// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public class SearchRegex extends Zeze.Net.Rpc<Zeze.Builtin.LogService.BSearchRegex.Data, Zeze.Builtin.LogService.BResult.Data> {
    public static final int ModuleId_ = 11035;
    public static final int ProtocolId_ = -1677801411; // 2617165885
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47397581277245
    static { register(TypeId_, SearchRegex.class); }

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

    public SearchRegex() {
        Argument = new Zeze.Builtin.LogService.BSearchRegex.Data();
        Result = new Zeze.Builtin.LogService.BResult.Data();
    }

    public SearchRegex(Zeze.Builtin.LogService.BSearchRegex.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.LogService.BResult.Data();
    }
}
