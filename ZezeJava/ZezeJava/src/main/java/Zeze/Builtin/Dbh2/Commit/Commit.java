// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

public class Commit extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Commit.BPrepareBatches.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11028;
    public static final int ProtocolId_ = 671558423;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47365570898711
    static { register(TypeId_, Commit.class); }

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

    public Commit() {
        Argument = new Zeze.Builtin.Dbh2.Commit.BPrepareBatches.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Commit(Zeze.Builtin.Dbh2.Commit.BPrepareBatches.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
