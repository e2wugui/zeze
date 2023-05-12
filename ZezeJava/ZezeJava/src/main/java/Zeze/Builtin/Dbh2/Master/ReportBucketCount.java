// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class ReportBucketCount extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BBucketCount.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = 80492696;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47360684865688
    static { register(TypeId_, ReportBucketCount.class); }

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

    public ReportBucketCount() {
        Argument = new Zeze.Builtin.Dbh2.Master.BBucketCount.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public ReportBucketCount(Zeze.Builtin.Dbh2.Master.BBucketCount.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
