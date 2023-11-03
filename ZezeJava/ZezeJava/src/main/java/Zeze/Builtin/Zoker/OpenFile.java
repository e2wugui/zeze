// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

public class OpenFile extends Zeze.Net.Rpc<Zeze.Builtin.Zoker.BOpenFile.Data, Zeze.Builtin.Zoker.BOpenFileResult.Data> {
    public static final int ModuleId_ = 11037;
    public static final int ProtocolId_ = 2088462255;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47405642508207
    static { register(TypeId_, OpenFile.class); }

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

    public OpenFile() {
        Argument = new Zeze.Builtin.Zoker.BOpenFile.Data();
        Result = new Zeze.Builtin.Zoker.BOpenFileResult.Data();
    }

    public OpenFile(Zeze.Builtin.Zoker.BOpenFile.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Zoker.BOpenFileResult.Data();
    }
}
