// auto-generated @formatter:off
package Zeze.Builtin.MQ.Master;

// Manager向Master注册
public class Register extends Zeze.Net.Rpc<Zeze.Builtin.MQ.Master.BMQServer.Data, Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11040;
    public static final int ProtocolId_ = 1280150188;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47417719098028
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
        Argument = new Zeze.Builtin.MQ.Master.BMQServer.Data();
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }

    public Register(Zeze.Builtin.MQ.Master.BMQServer.Data arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
