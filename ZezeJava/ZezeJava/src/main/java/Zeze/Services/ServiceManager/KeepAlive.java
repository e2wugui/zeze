package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class KeepAlive extends Rpc<EmptyBean, EmptyBean> {
	public static final int ProtocolId_ = Bean.Hash32(KeepAlive.class.getName()); // 1337189598
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 1337189598

	public static final int Success = 0;

	public KeepAlive() {
		Argument = EmptyBean.instance;
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
