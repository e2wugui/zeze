// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class Suspect extends Zeze.Raft.RaftRpc<Zeze.Services.ServiceManager.BIdentify, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 1784335559;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47340913872071
    static { register(TypeId_, Suspect.class); }

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

    public Suspect() {
        Argument = new Zeze.Services.ServiceManager.BIdentify();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Suspect(Zeze.Services.ServiceManager.BIdentify arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
