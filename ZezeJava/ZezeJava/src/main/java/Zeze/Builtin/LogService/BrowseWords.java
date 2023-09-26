// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public class BrowseWords extends Zeze.Net.Rpc<Zeze.Builtin.LogService.BSearchWords.Data, Zeze.Builtin.LogService.BResult.Data> {
    public static final int ModuleId_ = 11035;
    public static final int ProtocolId_ = 1061340255;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47396025451615
    static { register(TypeId_, BrowseWords.class); }

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

    public BrowseWords() {
        Argument = new Zeze.Builtin.LogService.BSearchWords.Data();
        Result = new Zeze.Builtin.LogService.BResult.Data();
    }

    public BrowseWords(Zeze.Builtin.LogService.BSearchWords.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.LogService.BResult.Data();
    }
}
