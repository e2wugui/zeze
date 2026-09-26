// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

// Manager周期上报本地分区清单，Master与mqTable对账（孤儿超宽限期回收）
public class ReportPartitions extends Zeze.Net.Rpc<Zeze.Builtin.MQ.Master.BReportPartitions.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11040;
    public static final int ProtocolId_ = -2146185701; // 2148781595
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47418587729435
    static { register(TypeId_, ReportPartitions.class); }

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

    public ReportPartitions() {
        Argument = new Zeze.Builtin.MQ.Master.BReportPartitions.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public ReportPartitions(Zeze.Builtin.MQ.Master.BReportPartitions.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
