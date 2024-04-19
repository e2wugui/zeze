package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class UnSubscribe extends Rpc<BUnSubscribeArgument, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(UnSubscribe.class.getName()); // -1962944542
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 2332022754

	static {
		register(TypeId_, UnSubscribe.class);
	}

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
		Argument = new BUnSubscribeArgument();
		Result = EmptyBean.instance;
	}

	public UnSubscribe(BUnSubscribeArgument arg) {
		Argument = arg;
		Result = EmptyBean.instance;
	}
}
