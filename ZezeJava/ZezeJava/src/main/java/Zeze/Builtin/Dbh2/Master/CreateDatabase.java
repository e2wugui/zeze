// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class CreateDatabase extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BCreateDatabase.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = 1368681472;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47361973054464
    static { register(TypeId_, CreateDatabase.class); }

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

    public CreateDatabase() {
        Argument = new Zeze.Builtin.Dbh2.Master.BCreateDatabase.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public CreateDatabase(Zeze.Builtin.Dbh2.Master.BCreateDatabase.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
