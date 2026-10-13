// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

public class AppendFile extends Zeze.Net.Rpc<Zeze.Builtin.Zoker.BAppendFile.Data, Zeze.Builtin.Zoker.BAppendFileResult.Data> {
    public static final int ModuleId_ = 11037;
    public static final int ProtocolId_ = -1813302165; // 2481665131
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47406035711083
    static { register(TypeId_, AppendFile.class); }

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

    public AppendFile() {
        Argument = new Zeze.Builtin.Zoker.BAppendFile.Data();
        Result = new Zeze.Builtin.Zoker.BAppendFileResult.Data();
    }

    public AppendFile(Zeze.Builtin.Zoker.BAppendFile.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Zoker.BAppendFileResult.Data();
    }
}
