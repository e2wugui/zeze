// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

public class Query extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.BBatchTid.Data, Zeze.Builtin.Dbh2.Commit.BTransactionState.Data> {
    public static final int ModuleId_ = 11028;
    public static final int ProtocolId_ = 287502951;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47365186843239
    static { register(TypeId_, Query.class); }

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

    public Query() {
        Argument = new Zeze.Builtin.Dbh2.BBatchTid.Data();
        Result = new Zeze.Builtin.Dbh2.Commit.BTransactionState.Data();
    }

    public Query(Zeze.Builtin.Dbh2.BBatchTid.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.Commit.BTransactionState.Data();
    }
}
