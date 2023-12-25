// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

public class OpenFile extends Zeze.Net.Rpc<Zeze.Builtin.HotDistribute.BOpenFile.Data, Zeze.Builtin.HotDistribute.BOpenFileResult.Data> {
    public static final int ModuleId_ = 11033;
    public static final int ProtocolId_ = -467348589; // 3827618707
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47390201795475
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
        Argument = new Zeze.Builtin.HotDistribute.BOpenFile.Data();
        Result = new Zeze.Builtin.HotDistribute.BOpenFileResult.Data();
    }

    public OpenFile(Zeze.Builtin.HotDistribute.BOpenFile.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.HotDistribute.BOpenFileResult.Data();
    }
}
