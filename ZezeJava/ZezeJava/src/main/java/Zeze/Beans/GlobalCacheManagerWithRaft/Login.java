// auto-generated @formatter:off
package Zeze.Beans.GlobalCacheManagerWithRaft;

public class Login extends Zeze.Raft.RaftRpc<Zeze.Beans.GlobalCacheManagerWithRaft.LoginParam, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11001;
    public static final int ProtocolId_ = -1624612360;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Login() {
        Argument = new Zeze.Beans.GlobalCacheManagerWithRaft.LoginParam();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
