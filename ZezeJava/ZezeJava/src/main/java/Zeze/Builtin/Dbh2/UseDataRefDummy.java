// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class UseDataRefDummy extends Zeze.Net.Protocol<Zeze.Builtin.Dbh2.BUseDataRefData> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = 798225405;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47357107631101

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

    public UseDataRefDummy() {
        Argument = new Zeze.Builtin.Dbh2.BUseDataRefData();
    }

    public UseDataRefDummy(Zeze.Builtin.Dbh2.BUseDataRefData arg) {
        Argument = arg;
    }
}
