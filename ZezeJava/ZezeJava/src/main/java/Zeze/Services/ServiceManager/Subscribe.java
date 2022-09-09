package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class Subscribe extends Rpc<BSubscribeInfo, EmptyBean> {
	public static final int ProtocolId_ = Bean.Hash32(Subscribe.class.getName()); // 1138220698
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 1138220698

	public static final int Success = 0;
	public static final int DuplicateSubscribe = 1;
	public static final int UnknownSubscribeType = 2;

	public Subscribe() {
		Argument = new BSubscribeInfo();
		Result = EmptyBean.instance;
	}

	public Subscribe(BSubscribeInfo arg) {
		Argument = arg;
		Result = EmptyBean.instance;
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}
}
