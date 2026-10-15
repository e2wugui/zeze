// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class TakeoverDone extends Zeze.Raft.RaftRpc<Zeze.Services.ServiceManager.BTakeoverDone, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 1876302765;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47341005839277
    static { register(TypeId_, TakeoverDone.class); }

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

    public TakeoverDone() {
        Argument = new Zeze.Services.ServiceManager.BTakeoverDone();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public TakeoverDone(Zeze.Services.ServiceManager.BTakeoverDone arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
