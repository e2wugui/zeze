package Zeze.Services.Handshake;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class CHandshakeArgument extends Bean {
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
	protected void InitChildrenRootInfo(Record.RootInfo root) {
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int getPreAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void setPreAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}
}
