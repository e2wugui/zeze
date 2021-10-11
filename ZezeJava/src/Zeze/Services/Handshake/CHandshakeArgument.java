package Zeze.Services.Handshake;

import Zeze.Serialize.*;

public final class CHandshakeArgument extends Zeze.Transaction.Bean {
	public byte dh_group;
	public byte[] dh_data;

	@Override
	public void Decode(ByteBuffer bb) {
		dh_group = bb.ReadByte();
		dh_data = bb.ReadBytes();
	}

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteByte(dh_group);
		bb.WriteBytes(dh_data);
	}

	@Override
	protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
	}
}