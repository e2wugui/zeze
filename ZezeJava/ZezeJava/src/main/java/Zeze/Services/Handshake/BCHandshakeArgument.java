package Zeze.Services.Handshake;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class BCHandshakeArgument extends Bean {
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
		throw new UnsupportedOperationException();
	}

	@Override
	protected void ResetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	private static int _PRE_ALLOC_SIZE_ = 16;

	@Override
	public int preAllocSize() {
		return _PRE_ALLOC_SIZE_;
	}

	@Override
	public void preAllocSize(int size) {
		_PRE_ALLOC_SIZE_ = size;
	}

	@Override
	public String toString() {
		return "BCHandshakeArgument{dh_group=" + dh_group + ", dh_data=["
				+ (dh_data != null ? dh_data.length : -1) + "]}";
	}
}
