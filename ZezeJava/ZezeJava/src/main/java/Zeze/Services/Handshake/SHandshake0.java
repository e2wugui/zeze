package Zeze.Services.Handshake;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

/** SHandshake0协议：服务端接受连接后主动下发的握手推荐（加密/压缩类型与支持列表）。 */
public class SHandshake0 extends Protocol<BSHandshake0Argument> {
	public static final int ProtocolId_ = Bean.hash32(SHandshake0.class.getName()); // -2018202792
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 2276764504

	static {
		register(TypeId_, SHandshake0.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public SHandshake0() {
		Argument = new BSHandshake0Argument();
	}
}
