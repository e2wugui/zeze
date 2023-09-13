package Zeze.Services.Handshake;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class SKeepAlive extends Protocol<EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(SKeepAlive.class.getName()); // 77153202
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 77153202

	public static final SKeepAlive instance = new SKeepAlive();

	static {
		register(TypeId_, SKeepAlive.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public SKeepAlive() {
		Argument = EmptyBean.instance;
	}
}
