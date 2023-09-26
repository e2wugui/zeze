// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public class Search extends Zeze.Net.Rpc<Zeze.Builtin.LogService.BSearch.Data, Zeze.Builtin.LogService.BResult.Data> {
    public static final int ModuleId_ = 11035;
    public static final int ProtocolId_ = 90756530;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47395054867890
    static { register(TypeId_, Search.class); }

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

    public Search() {
        Argument = new Zeze.Builtin.LogService.BSearch.Data();
        Result = new Zeze.Builtin.LogService.BResult.Data();
    }

    public Search(Zeze.Builtin.LogService.BSearch.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.LogService.BResult.Data();
    }
}
