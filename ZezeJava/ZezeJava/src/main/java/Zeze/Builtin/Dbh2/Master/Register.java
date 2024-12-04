// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

public class Register extends Zeze.Net.Rpc<Zeze.Builtin.Dbh2.Master.BRegister.Data, Zeze.Builtin.Dbh2.Master.BRegisterResult.Data> {
    public static final int ModuleId_ = 11027;
    public static final int ProtocolId_ = -552030131; // 3742937165
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47364347310157
    static { register(TypeId_, Register.class); }

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
        Argument = new Zeze.Builtin.Dbh2.Master.BRegister.Data();
        Result = new Zeze.Builtin.Dbh2.Master.BRegisterResult.Data();
    }

    public Register(Zeze.Builtin.Dbh2.Master.BRegister.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Dbh2.Master.BRegisterResult.Data();
    }
}
