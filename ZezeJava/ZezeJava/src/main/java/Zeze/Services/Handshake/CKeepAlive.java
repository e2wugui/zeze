package Zeze.Services.Handshake;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class CKeepAlive extends Protocol<EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(CKeepAlive.class.getName()); // -1636259715
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 2658707581

	public static final CKeepAlive instance = new CKeepAlive();

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
