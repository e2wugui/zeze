package Zeze.Services.GlobalCacheManager;

public class Login extends Zeze.Net.Rpc<LoginParam, AchillesHeelConfig> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.Hash32(Login.class.getName()); // -1420506365
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 2874460931

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
		Result = new AchillesHeelConfig();
	}

	public Login(int id) {
		Argument = new LoginParam();
		Result = new AchillesHeelConfig();

		Argument.ServerId = id;
	}
}
