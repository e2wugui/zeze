package Zeze.Services.Handshake;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class BSHandshakeArgument extends Bean {
	public byte[] encryptParam;
	public int compressS2c;
	public int compressC2s;
	public int encryptType;

	@Override
	public void decode(ByteBuffer bb) {
		encryptParam = bb.ReadBytes();
		compressS2c = bb.ReadInt();
		compressC2s = bb.ReadInt();
		encryptType = bb.ReadInt();
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBytes(encryptParam);
		bb.WriteInt(compressS2c);
		bb.WriteInt(compressC2s);
		bb.WriteInt(encryptType);
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}

	private static int _PRE_ALLOC_SIZE_ = 128;

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
		return "BSHandshakeArgument {encryptParam=[" + (encryptParam != null ? encryptParam.length : -1) +
				"], compressS2c=" + compressS2c + ", compressC2s=" + compressC2s + ", encryptType=" + encryptType + '}';
	}
}
