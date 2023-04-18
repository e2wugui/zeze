// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Commit;

public class DummyImportBean extends Zeze.Net.Protocol<Zeze.Builtin.Dbh2.Commit.BTransactionState.Data> {
    public static final int ModuleId_ = 11028;
    public static final int ProtocolId_ = -444779155; // 3850188141
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47368749528429
    static { register(TypeId_, DummyImportBean.class); }

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

    public DummyImportBean() {
        Argument = new Zeze.Builtin.Dbh2.Commit.BTransactionState.Data();
    }

    public DummyImportBean(Zeze.Builtin.Dbh2.Commit.BTransactionState.Data arg) {
        Argument = arg;
    }
}
