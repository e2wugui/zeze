// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class ReadyServiceList extends Zeze.Raft.RaftRpc<Zeze.Services.ServiceManager.BServiceListVersion, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 568430381;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47339697966893
    static { register(TypeId_, ReadyServiceList.class); }

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

    public ReadyServiceList() {
        Argument = new Zeze.Services.ServiceManager.BServiceListVersion();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public ReadyServiceList(Zeze.Services.ServiceManager.BServiceListVersion arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
