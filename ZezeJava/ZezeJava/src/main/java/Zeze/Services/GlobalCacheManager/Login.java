package Zeze.Services.GlobalCacheManager;

public class Login extends Zeze.Net.Rpc<LoginParam, Zeze.Transaction.EmptyBean> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.Hash32(Login.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

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
