// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class GetBuckets extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BGetBucketsDaTa, Zeze.Dbh2.Master.MasterTableDaTa> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -1781126263; // 2513841033
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47363118214025

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
        Argument = new Zeze.Builtin.Dbh2.Master.BGetBucketsDaTa();
        Result = new Zeze.Dbh2.Master.MasterTableDaTa();
    }

    public GetBuckets(Zeze.Builtin.Dbh2.Master.BGetBucketsDaTa arg) {
        Argument = arg;
        Result = new Zeze.Dbh2.Master.MasterTableDaTa();
    }
}
