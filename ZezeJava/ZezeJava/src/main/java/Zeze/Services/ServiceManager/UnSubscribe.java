package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class UnSubscribe extends Rpc<SubscribeInfo, EmptyBean> {
	public static final int ProtocolId_ = Bean.Hash32(UnSubscribe.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

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
		Argument = new SubscribeInfo();
		Result = new EmptyBean();
	}
}
