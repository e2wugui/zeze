package Zeze.Services.Handshake;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

public final class BSHandshakeArgument implements Serializable {
	public byte[] encryptParam = ByteBuffer.Empty;
	public int compressS2c;
	public int compressC2s;
	public int encryptType;

	@Override
	public void decode(IByteBuffer bb) {
		encryptParam = bb.ReadBytes();
		compressS2c = bb.ReadInt();
		compressC2s = bb.ReadInt();

		// 兼容旧版
		if (!bb.isEmpty())
			encryptType = bb.ReadInt();
		else
			encryptType = encryptParam.length != 0 ? Constant.eEncryptTypeAes : Constant.eEncryptTypeDisable;
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBytes(encryptParam);
		bb.WriteInt(compressS2c);
		bb.WriteInt(compressC2s);
		bb.WriteInt(encryptType);
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
		return "BSHandshakeArgument{encryptParam=[" + (encryptParam != null ? encryptParam.length : -1) +
				"],compressS2c/C2s=" + compressS2c + '/' + compressC2s + ",encryptType=" + encryptType + '}';
	}
}
