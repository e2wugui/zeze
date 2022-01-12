package Zeze.Services.Handshake;

import Zeze.Serialize.*;

public final class SHandshakeArgument extends Zeze.Transaction.Bean {
	public byte[] dh_data;
	public boolean s2cNeedCompress;
	public boolean c2sNeedCompress;

	@Override
	public void Decode(ByteBuffer bb) {
		dh_data = bb.ReadBytes();
		s2cNeedCompress = bb.ReadBool();
		c2sNeedCompress = bb.ReadBool();
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteBytes(dh_data);
		bb.WriteBool(s2cNeedCompress);
		bb.WriteBool(c2sNeedCompress);
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
	}
}
