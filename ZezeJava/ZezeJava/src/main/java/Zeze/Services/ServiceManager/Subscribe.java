package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class Subscribe extends Rpc<SubscribeInfo, EmptyBean> {
	public static final int ProtocolId_ = Bean.Hash32(Subscribe.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	public static final int Success = 0;
	public static final int DuplicateSubscribe = 1;
	public static final int UnknownSubscribeType = 2;

	public Subscribe() {
		Argument = new SubscribeInfo();
		Result = new EmptyBean();
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
