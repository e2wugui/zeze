// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public class SearchWords extends Zeze.Net.Rpc<Zeze.Builtin.LogService.BSearchWords.Data, Zeze.Builtin.LogService.BResult.Data> {
    public static final int ModuleId_ = 11035;
    public static final int ProtocolId_ = 443347516;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47395407458876
    static { register(TypeId_, SearchWords.class); }

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

    public SearchWords() {
        Argument = new Zeze.Builtin.LogService.BSearchWords.Data();
        Result = new Zeze.Builtin.LogService.BResult.Data();
    }

    public SearchWords(Zeze.Builtin.LogService.BSearchWords.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.LogService.BResult.Data();
    }
}
