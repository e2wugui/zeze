// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public class BrowseRegex extends Zeze.Net.Rpc<Zeze.Builtin.LogService.BSearchRegex.Data, Zeze.Builtin.LogService.BResult.Data> {
    public static final int ModuleId_ = 11035;
    public static final int ProtocolId_ = -1075094639; // 3219872657
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47398183984017
    static { register(TypeId_, BrowseRegex.class); }

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

    public BrowseRegex() {
        Argument = new Zeze.Builtin.LogService.BSearchRegex.Data();
        Result = new Zeze.Builtin.LogService.BResult.Data();
    }

    public BrowseRegex(Zeze.Builtin.LogService.BSearchRegex.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.LogService.BResult.Data();
    }
}
