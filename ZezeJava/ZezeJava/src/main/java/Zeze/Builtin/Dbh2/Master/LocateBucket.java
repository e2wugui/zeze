// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class LocateBucket extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BLocateBucketDaTa, Zeze.Builtin.Dbh2.BBucketMetaDaTa> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -1189628841; // 3105338455
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47363709711447

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

    public LocateBucket() {
        Argument = new Zeze.Builtin.Dbh2.Master.BLocateBucketDaTa();
        Result = new Zeze.Builtin.Dbh2.BBucketMetaDaTa();
    }

    public LocateBucket(Zeze.Builtin.Dbh2.Master.BLocateBucketDaTa arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BBucketMetaDaTa();
    }
}
