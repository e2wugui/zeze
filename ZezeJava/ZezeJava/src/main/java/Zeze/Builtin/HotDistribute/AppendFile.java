// auto-generated @formatter:off
package Zeze.Builtin.HotDistribute;

public class AppendFile extends Zeze.Net.Rpc<Zeze.Builtin.HotDistribute.BAppendFile.Data, Zeze.Builtin.HotDistribute.BAppendFileResult.Data> {
    public static final int ModuleId_ = 11033;
    public static final int ProtocolId_ = -1359947878; // 2935019418
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47389309196186
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
        Argument = new Zeze.Builtin.HotDistribute.BAppendFile.Data();
        Result = new Zeze.Builtin.HotDistribute.BAppendFileResult.Data();
    }

    public AppendFile(Zeze.Builtin.HotDistribute.BAppendFile.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.HotDistribute.BAppendFileResult.Data();
    }
}
