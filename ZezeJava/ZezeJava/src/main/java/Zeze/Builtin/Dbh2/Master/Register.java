// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class Register extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BRegisterData, Zeze.Transaction.EmptyBeanData> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -552030131; // 3742937165
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47364347310157

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

    public Register() {
        Argument = new Zeze.Builtin.Dbh2.Master.BRegisterData();
        Result = Zeze.Transaction.EmptyBeanData.instance;
    }

    public Register(Zeze.Builtin.Dbh2.Master.BRegisterData arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBeanData.instance;
    }
}
