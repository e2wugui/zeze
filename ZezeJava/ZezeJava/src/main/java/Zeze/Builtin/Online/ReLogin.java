// auto-generated @formatter:off
package Zeze.Builtin.Online;

public class ReLogin extends Zeze.Net.Rpc<Zeze.Builtin.Online.BReLogin, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11100;
    public static final int ProtocolId_ = 927898915;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47675064884515

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
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
