// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

// Takeover租约接管（对齐非raft版）：Identify上报serverId，断线广播Suspect提示；裁决在应用库tTakeoverLease
public class Identify extends Zeze.Raft.RaftRpc<Zeze.Services.ServiceManager.BIdentify, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 1940701173;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47341070237685
    static { register(TypeId_, Identify.class); }

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

    public Identify() {
        Argument = new Zeze.Services.ServiceManager.BIdentify();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Identify(Zeze.Services.ServiceManager.BIdentify arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
