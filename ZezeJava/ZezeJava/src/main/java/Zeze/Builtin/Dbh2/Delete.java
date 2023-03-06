// auto-generated @formatter:off
package Zeze.Builtin.Dbh2;

public class Delete extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.BDeleteArgument, Zeze.Builtin.Dbh2.BDeleteResult> {
    public static final int ModuleId_ = 11026;
    public static final int ProtocolId_ = -367775506; // 3927191790
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47360236597486

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

    public Delete() {
        Argument = new Zeze.Builtin.Dbh2.BDeleteArgument();
        Result = new Zeze.Builtin.Dbh2.BDeleteResult();
    }

    public Delete(Zeze.Builtin.Dbh2.BDeleteArgument arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.BDeleteResult();
    }
}
