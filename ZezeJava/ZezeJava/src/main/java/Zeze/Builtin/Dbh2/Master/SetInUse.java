// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class SetInUse extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BSetInUse.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = 252006537;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47360856379529
    static { register(TypeId_, SetInUse.class); }

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

    public SetInUse() {
        Argument = new Zeze.Builtin.Dbh2.Master.BSetInUse.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public SetInUse(Zeze.Builtin.Dbh2.Master.BSetInUse.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
