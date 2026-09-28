package Zeze.Services.Handshake;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

/** KeepAlive协议：握手连接的保活RPC，空参数空结果。 */
public final class KeepAlive extends Rpc<EmptyBean, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(KeepAlive.class.getName()); // -183352608
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 4111614688

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
