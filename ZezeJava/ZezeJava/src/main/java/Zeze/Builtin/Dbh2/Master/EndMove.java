// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class EndMove extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BEndMove.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = 2056102490;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47362660475482
    static { register(TypeId_, EndMove.class); }

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

    public EndMove() {
        Argument = new Zeze.Builtin.Dbh2.Master.BEndMove.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public EndMove(Zeze.Builtin.Dbh2.Master.BEndMove.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
