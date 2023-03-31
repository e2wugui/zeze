// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class GetBuckets extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BGetBuckets.Data, Zeze.Dbh2.Master.MasterTable.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -1781126263; // 2513841033
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47363118214025
    static { register(TypeId_, GetBuckets.class); }

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

    public GetBuckets() {
        Argument = new Zeze.Builtin.Dbh2.Master.BGetBuckets.Data();
        Result = new Zeze.Dbh2.Master.MasterTable.Data();
    }

    public GetBuckets(Zeze.Builtin.Dbh2.Master.BGetBuckets.Data arg) {
        Argument = arg;
        Result = new Zeze.Dbh2.Master.MasterTable.Data();
    }
}
