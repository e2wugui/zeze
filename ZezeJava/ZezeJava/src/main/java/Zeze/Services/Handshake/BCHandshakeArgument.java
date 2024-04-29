package Zeze.Services.Handshake;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

public final class BCHandshakeArgument implements Serializable {
	public int encryptType;
	public byte[] encryptParam = ByteBuffer.Empty;
	public int compressS2c = Constant.eCompressTypeDisable; // 默认的时候由服务器决定是否压缩。
	public int compressC2s = Constant.eCompressTypeDisable; // 默认的时候由服务器决定是否压缩。

	@Override
	public void decode(IByteBuffer bb) {
		encryptType = bb.ReadInt();
		encryptParam = bb.ReadBytes();

		// 兼容旧版
		if (!bb.isEmpty()) {
			compressS2c = bb.ReadInt();
			compressC2s = bb.ReadInt();
		} else {
			compressS2c = encryptType != Constant.eEncryptTypeDisable ? Constant.eCompressTypeMppc : Constant.eCompressTypeDisable;
			compressC2s = compressS2c;
		}
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(encryptType);
		bb.WriteBytes(encryptParam);
		bb.WriteInt(compressS2c);
		bb.WriteInt(compressC2s);
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
		return "BCHandshakeArgument{encryptType=" + encryptType
				+ ",encryptParam=[" + (encryptParam != null ? encryptParam.length : -1)
				+ "],compressS2c/C2s=" + compressS2c + '/' + compressC2s + '}';
	}
}
