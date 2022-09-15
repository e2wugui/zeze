package Zeze.Services.Handshake;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class BSHandshakeArgument extends Bean {
	public byte[] dh_data;
	public boolean s2cNeedCompress;
	public boolean c2sNeedCompress;

	@Override
	public void decode(ByteBuffer bb) {
		dh_data = bb.ReadBytes();
		s2cNeedCompress = bb.ReadBool();
		c2sNeedCompress = bb.ReadBool();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBytes(dh_data);
		bb.WriteBool(s2cNeedCompress);
		bb.WriteBool(c2sNeedCompress);
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
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
		return "BSHandshakeArgument{" + "dh_data=[" + (dh_data != null ? dh_data.length : -1) +
				"], s2cNeedCompress=" + s2cNeedCompress + ", c2sNeedCompress=" + c2sNeedCompress + '}';
	}
}
