package Zeze.Services.Handshake;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

public final class CHandshake extends Protocol<BCHandshakeArgument> {
	public static final int ProtocolId_ = Bean.hash32(CHandshake.class.getName()); // -554021601
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 3740945695

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

	public CHandshake(byte dh_group, byte[] dh_data) {
		Argument = new BCHandshakeArgument();
		Argument.dh_group = dh_group;
		Argument.dh_data = dh_data;
	}
}
