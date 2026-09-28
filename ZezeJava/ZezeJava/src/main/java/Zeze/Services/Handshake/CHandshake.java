package Zeze.Services.Handshake;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

/** CHandshake协议：客户端发起握手，携带选定的加密类型/参数与两个方向的压缩意向。 */
public final class CHandshake extends Protocol<BCHandshakeArgument> {
	public static final int ProtocolId_ = Bean.hash32(CHandshake.class.getName()); // -554021601
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 3740945695

	static {
		register(TypeId_, CHandshake.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public CHandshake() {
		Argument = new BCHandshakeArgument();
	}
}
