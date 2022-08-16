package Zeze.Services.Handshake;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public class SHandshake0Argument extends Bean {
	public boolean EnableEncrypt;

	@Override
	public void Encode(ByteBuffer bb) {
		bb.WriteBool(EnableEncrypt);
	}

	@Override
	public void Decode(ByteBuffer bb) {
		EnableEncrypt = bb.ReadBool();
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
	protected void InitChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void ResetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return "SHandshake0Argument{" + "EnableEncrypt=" + EnableEncrypt + '}';
	}
}
