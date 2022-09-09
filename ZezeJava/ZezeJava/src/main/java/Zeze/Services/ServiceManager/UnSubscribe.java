package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class UnSubscribe extends Rpc<BSubscribeInfo, EmptyBean> {
	public static final int ProtocolId_ = Bean.Hash32(UnSubscribe.class.getName()); // -1962944542
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 2332022754

	public static final int Success = 0;
	public static final int NotExist = 1;

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public UnSubscribe() {
		Argument = new BSubscribeInfo();
		Result = EmptyBean.instance;
	}
}
