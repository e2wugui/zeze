package Zeze.Services.GlobalCacheManager;

public class ReLogin extends Zeze.Net.Rpc<LoginParam, Zeze.Transaction.EmptyBean> {
    public final static int ProtocolId_ = Zeze.Transaction.Bean.Hash32(ReLogin.class.getName());

    @Override
    public int getModuleId() {
        return 0;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public ReLogin() {
        Argument = new LoginParam();
        Result = new Zeze.Transaction.EmptyBean();
    }

    public ReLogin(int id) {
        Argument = new LoginParam();
        Result = new Zeze.Transaction.EmptyBean();
        Argument.ServerId = id;
    }
}
