// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class GetDataWithVersion extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BGetDataWithVersion.Data, Zeze.Builtin.Dbh2.Master.BGetDataWithVersionResult.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = 589401115;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47361193774107
    static { register(TypeId_, GetDataWithVersion.class); }

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

    public GetDataWithVersion() {
        Argument = new Zeze.Builtin.Dbh2.Master.BGetDataWithVersion.Data();
        Result = new Zeze.Builtin.Dbh2.Master.BGetDataWithVersionResult.Data();
    }

    public GetDataWithVersion(Zeze.Builtin.Dbh2.Master.BGetDataWithVersion.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.Master.BGetDataWithVersionResult.Data();
    }
}
