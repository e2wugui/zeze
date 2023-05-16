// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class CheckFreeManager extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean.Data, Zeze.Builtin.Dbh2.Master.BBucketCount.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -688748505; // 3606218791
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47364210591783
    static { register(TypeId_, CheckFreeManager.class); }

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

    public CheckFreeManager() {
        Argument = Zeze.Transaction.EmptyBean.Data.instance;
        Result = new Zeze.Builtin.Dbh2.Master.BBucketCount.Data();
    }
}
