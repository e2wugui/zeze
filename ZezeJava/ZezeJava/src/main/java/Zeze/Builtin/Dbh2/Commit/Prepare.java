// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

public class Prepare extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Commit.BCommit.Data, Zeze.Builtin.Dbh2.BBatchTid.Data> {
    public static final int ModuleId_ = 11028;
    public static final int ProtocolId_ = -1839863382; // 2455103914
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47367354444202
    static { register(TypeId_, Prepare.class); }

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

    public Prepare() {
        Argument = new Zeze.Builtin.Dbh2.Commit.BCommit.Data();
        Result = new Zeze.Builtin.Dbh2.BBatchTid.Data();
    }

    public Prepare(Zeze.Builtin.Dbh2.Commit.BCommit.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BBatchTid.Data();
    }
}
