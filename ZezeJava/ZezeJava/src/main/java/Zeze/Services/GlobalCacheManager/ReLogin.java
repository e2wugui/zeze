package Zeze.Services.GlobalCacheManager;

public class ReLogin extends Zeze.Net.Rpc<BLoginParam, Zeze.Transaction.EmptyBean> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.Hash32(ReLogin.class.getName()); // 1197409195
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 1197409195

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public ReLogin() {
		Argument = new BLoginParam();
		Result = new Zeze.Transaction.EmptyBean();
	}
}
