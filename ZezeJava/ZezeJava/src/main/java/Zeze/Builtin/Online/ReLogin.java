// auto-generated @formatter:off
package Zeze.Builtin.Online;

// 网络断开重新登录（数据无法同步时会失败，此时客户端应该重新走完整的登录流程-装载数据。）
public class ReLogin extends Zeze.Net.Rpc<Zeze.Builtin.Online.BReLogin, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11100;
    public static final int ProtocolId_ = 927898915;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47675064884515
    static { register(TypeId_, ReLogin.class); }

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

    public ReLogin() {
        Argument = new Zeze.Builtin.Online.BReLogin();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public ReLogin(Zeze.Builtin.Online.BReLogin arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
