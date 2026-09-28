package Zeze.Services.Handshake;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

/** CHandshakeDone协议：客户端进入加密后通知服务端握手完成，服务端据此触发OnHandshakeDone。 */
public final class CHandshakeDone extends Protocol<EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(CHandshakeDone.class.getName()); // 1896283174
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 1896283174

	public static final CHandshakeDone instance = new CHandshakeDone();

	static {
		register(TypeId_, CHandshakeDone.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public CHandshakeDone() {
		Argument = EmptyBean.instance;
	}
}
