package Zeze.Services.GlobalCacheManager;

public class Login extends Zeze.Net.Rpc<LoginParam, Zeze.Transaction.EmptyBean> {
    public final static int ProtocolId_ = Zeze.Transaction.Bean.Hash16(Login.class.getName());

    @Override
    public int getModuleId() {
        return 0;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Login() {
        Argument = new LoginParam();
        Result = new Zeze.Transaction.EmptyBean();
    }

    public Login(int id) {
        Argument = new LoginParam();
        Result = new Zeze.Transaction.EmptyBean();

        Argument.ServerId = id;
    }
}
