// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

// Manager向Master报告自己的负载指数
public class ReportLoad extends Zeze.Net.Rpc<Zeze.Builtin.MQ.Master.BLoad.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11040;
    public static final int ProtocolId_ = 153412983;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47416592360823
    static { register(TypeId_, ReportLoad.class); }

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

    public ReportLoad() {
        Argument = new Zeze.Builtin.MQ.Master.BLoad.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public ReportLoad(Zeze.Builtin.MQ.Master.BLoad.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
