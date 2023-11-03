// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

public class CloseFile extends Zeze.Net.Rpc<Zeze.Builtin.Zoker.BCloseFile.Data, Zeze.Builtin.Zoker.BCloseFileResult.Data> {
    public static final int ModuleId_ = 11037;
    public static final int ProtocolId_ = -1119176907; // 3175790389
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47406729836341
    static { register(TypeId_, CloseFile.class); }

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

    public CloseFile() {
        Argument = new Zeze.Builtin.Zoker.BCloseFile.Data();
        Result = new Zeze.Builtin.Zoker.BCloseFileResult.Data();
    }

    public CloseFile(Zeze.Builtin.Zoker.BCloseFile.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Zoker.BCloseFileResult.Data();
    }
}
