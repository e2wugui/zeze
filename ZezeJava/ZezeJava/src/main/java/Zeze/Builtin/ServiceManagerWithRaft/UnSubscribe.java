// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class UnSubscribe extends Zeze.Raft.RaftRpc<Zeze.Services.ServiceManager.BUnSubscribeArgument, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 622739852;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47339752276364
    static { register(TypeId_, UnSubscribe.class); }

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

    public static final int Success = 0;
    public static final int NotExist = 1;

    public UnSubscribe() {
        Argument = new Zeze.Services.ServiceManager.BUnSubscribeArgument();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public UnSubscribe(Zeze.Services.ServiceManager.BUnSubscribeArgument arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
