package Zeze.Services.Handshake;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class CKeepAlive extends Protocol<EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(CKeepAlive.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	static {
		register(TypeId_, CKeepAlive.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public CKeepAlive() {
		Argument = EmptyBean.instance;
	}
}
