package Zeze.Services.Handshake;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class KeepAlive extends Rpc<EmptyBean, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(KeepAlive.class.getName()); //
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; //

	public static final KeepAlive instance = new KeepAlive();

	static {
		register(TypeId_, KeepAlive.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public KeepAlive() {
		Argument = EmptyBean.instance;
		Result = EmptyBean.instance;
	}
}
