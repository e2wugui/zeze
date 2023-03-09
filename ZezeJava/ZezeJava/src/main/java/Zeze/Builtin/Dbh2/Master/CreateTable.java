// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class CreateTable extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BCreateTableDaTa, Zeze.Dbh2.Master.MasterTableDaTa> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -1554675613; // 2740291683
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47363344664675

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

    public CreateTable() {
        Argument = new Zeze.Builtin.Dbh2.Master.BCreateTableDaTa();
        Result = new Zeze.Dbh2.Master.MasterTableDaTa();
    }

    public CreateTable(Zeze.Builtin.Dbh2.Master.BCreateTableDaTa arg) {
        Argument = arg;
        Result = new Zeze.Dbh2.Master.MasterTableDaTa();
    }
}
