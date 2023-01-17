// auto-generated @formatter:off
package Zeze.Builtin.ServiceManagerWithRaft;

public class Login extends Zeze.Raft.RaftRpc<Zeze.Builtin.ServiceManagerWithRaft.BLogin, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11022;
    public static final int ProtocolId_ = 618354316;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47339747890828

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

    public Login() {
        Argument = new Zeze.Builtin.ServiceManagerWithRaft.BLogin();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Login(Zeze.Builtin.ServiceManagerWithRaft.BLogin arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
