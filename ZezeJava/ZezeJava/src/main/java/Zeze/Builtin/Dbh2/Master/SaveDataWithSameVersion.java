// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class SaveDataWithSameVersion extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion.Data, Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersionResult.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -1497736380; // 2797230916
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47363401603908
    static { register(TypeId_, SaveDataWithSameVersion.class); }

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

    public SaveDataWithSameVersion() {
        Argument = new Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion.Data();
        Result = new Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersionResult.Data();
    }

    public SaveDataWithSameVersion(Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersion.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.Master.BSaveDataWithSameVersionResult.Data();
    }
}
