// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class UnRegister extends Zeze.Raft.RaftRpc<Zeze.Services.ServiceManager.BServiceInfo, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 1881863600;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47341011400112
    static { register(TypeId_, UnRegister.class); }

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

    public UnRegister() {
        Argument = new Zeze.Services.ServiceManager.BServiceInfo();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public UnRegister(Zeze.Services.ServiceManager.BServiceInfo arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
