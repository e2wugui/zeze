// auto-generated @formatter:off
package Zeze.Builtin.LogService;

public class NewSessionWords extends Zeze.Net.Rpc<Zeze.Builtin.LogService.BWords.Data, Zeze.Builtin.LogService.BSession.Data> {
    public static final int ModuleId_ = 11035;
    public static final int ProtocolId_ = -1530014190; // 2764953106
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47397729064466
    static { register(TypeId_, NewSessionWords.class); }

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

    public NewSessionWords() {
        Argument = new Zeze.Builtin.LogService.BWords.Data();
        Result = new Zeze.Builtin.LogService.BSession.Data();
    }

    public NewSessionWords(Zeze.Builtin.LogService.BWords.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.LogService.BSession.Data();
    }
}
