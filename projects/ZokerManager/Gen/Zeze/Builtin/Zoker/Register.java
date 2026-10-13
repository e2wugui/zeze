// auto-generated @formatter:off
package Zeze.Builtin.Zoker;

public class Register extends Zeze.Net.Rpc<Zeze.Builtin.Zoker.BRegister.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11037;
    public static final int ProtocolId_ = -507135573; // 3787831723
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47407341877675
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
        Argument = new Zeze.Builtin.Zoker.BRegister.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Register(Zeze.Builtin.Zoker.BRegister.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
