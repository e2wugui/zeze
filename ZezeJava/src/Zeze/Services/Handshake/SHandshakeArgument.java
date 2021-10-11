package Zeze.Services.Handshake;

import Zeze.Serialize.*;

public final class SHandshakeArgument extends Zeze.Transaction.Bean {
	public byte[] dh_data;
	public boolean s2cneedcompress;
	public boolean c2sneedcompress;

	@Override
	public void Decode(ByteBuffer bb) {
		dh_data = bb.ReadBytes();
		s2cneedcompress = bb.ReadBool();
		c2sneedcompress = bb.ReadBool();
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteBytes(dh_data);
		bb.WriteBool(s2cneedcompress);
		bb.WriteBool(c2sneedcompress);
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
	}
}