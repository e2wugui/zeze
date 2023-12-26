// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class ClearInUse extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BClearInUse.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -1544471837; // 2750495459
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47363354868451
    static { register(TypeId_, ClearInUse.class); }

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

    public ClearInUse() {
        Argument = new Zeze.Builtin.Dbh2.Master.BClearInUse.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public ClearInUse(Zeze.Builtin.Dbh2.Master.BClearInUse.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
