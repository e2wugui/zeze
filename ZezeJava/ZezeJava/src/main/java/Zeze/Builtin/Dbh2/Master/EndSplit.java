// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class EndSplit extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BEndSplit.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -994882332; // 3300084964
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47363904457956
    static { register(TypeId_, EndSplit.class); }

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

    public EndSplit() {
        Argument = new Zeze.Builtin.Dbh2.Master.BEndSplit.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public EndSplit(Zeze.Builtin.Dbh2.Master.BEndSplit.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
