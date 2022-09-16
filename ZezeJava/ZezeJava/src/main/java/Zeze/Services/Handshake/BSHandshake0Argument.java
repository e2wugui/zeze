package Zeze.Services.Handshake;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public class BSHandshake0Argument extends Bean {
	public boolean enableEncrypt;

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBool(enableEncrypt);
	}

	@Override
	public void decode(ByteBuffer bb) {
		enableEncrypt = bb.ReadBool();
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
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "BSHandshake0Argument{" + "EnableEncrypt=" + enableEncrypt + '}';
	}
}
