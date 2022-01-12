package Zeze.Services.Handshake;

import Zeze.Transaction.*;

public final class SHandshake extends Zeze.Net.Protocol1<SHandshakeArgument> {
	public final static int ProtocolId_ = Bean.Hash32(SHandshake.class.getName());

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public SHandshake() {
		Argument = new SHandshakeArgument();
	}

	public SHandshake(byte[] dh_data, boolean s2cNeedCompress, boolean c2sNeedCompress) {
		Argument = new SHandshakeArgument();
		Argument.dh_data = dh_data;
		Argument.s2cNeedCompress = s2cNeedCompress;
		Argument.c2sNeedCompress = c2sNeedCompress;
	}
}
