package Zeze.Services.Handshake;

import Zeze.Net.*;
import Zeze.Transaction.*;

public final class CHandshake extends Protocol1<CHandshakeArgument> {
	public final static int ProtocolId_ = Bean.Hash16(CHandshake.class.getName());

	@Override
	public int getModuleId() {
		return 0;
	}
	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public CHandshake() {
		Argument = new CHandshakeArgument();
	}

	public CHandshake(byte dh_group, byte[] dh_data) {
		Argument = new CHandshakeArgument();
		Argument.dh_group = dh_group;
		Argument.dh_data = dh_data;
	}
}