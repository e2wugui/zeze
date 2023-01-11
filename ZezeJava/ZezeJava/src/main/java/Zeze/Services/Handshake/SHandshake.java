package Zeze.Services.Handshake;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

public final class SHandshake extends Protocol<BSHandshakeArgument> {
	public static final int ProtocolId_ = Bean.hash32(SHandshake.class.getName()); // -723986006
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 3570981290

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public SHandshake() {
		Argument = new BSHandshakeArgument();
	}
}
