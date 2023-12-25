// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

public class CloseFile extends Zeze.Net.Rpc<Zeze.Builtin.HotDistribute.BCloseFile.Data, Zeze.Builtin.HotDistribute.BCloseFileResult.Data> {
    public static final int ModuleId_ = 11033;
    public static final int ProtocolId_ = -224517575; // 4070449721
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47390444626489
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
        Argument = new Zeze.Builtin.HotDistribute.BCloseFile.Data();
        Result = new Zeze.Builtin.HotDistribute.BCloseFileResult.Data();
    }

    public CloseFile(Zeze.Builtin.HotDistribute.BCloseFile.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.HotDistribute.BCloseFileResult.Data();
    }
}
