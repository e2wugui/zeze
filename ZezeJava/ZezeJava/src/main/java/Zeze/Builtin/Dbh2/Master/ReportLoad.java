// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class ReportLoad extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BLoad.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -1187744480; // 3107222816
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47363711595808
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
        Argument = new Zeze.Builtin.Dbh2.Master.BLoad.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public ReportLoad(Zeze.Builtin.Dbh2.Master.BLoad.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
