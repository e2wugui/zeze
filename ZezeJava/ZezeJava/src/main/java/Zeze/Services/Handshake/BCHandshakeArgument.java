package Zeze.Services.Handshake;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public final class BCHandshakeArgument extends Bean {
	public int encryptType;
	public byte[] encryptParam;

	public int compressS2c = Constant.eCompressTypeDisable; // 默认的时候由服务器决定是否压缩。
	public int compressC2s = Constant.eCompressTypeDisable; // 默认的时候由服务器决定是否压缩。

	@Override
	public void decode(ByteBuffer bb) {
		encryptType = bb.ReadInt();
		encryptParam = bb.ReadBytes();

		// 兼容旧版客户端
		if (bb.WriteIndex > bb.ReadIndex) {
			compressS2c = bb.ReadInt();
			compressC2s = bb.ReadInt();
		}
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(encryptType);
		bb.WriteBytes(encryptParam);
		bb.WriteInt(compressS2c);
		bb.WriteInt(compressC2s);
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
		return "BCHandshakeArgument { EncryptType=" + encryptType
				+ ", EncryptParam=[" + (encryptParam != null ? encryptParam.length : -1)
				+ "] compress S2c/C2s=" + compressS2c + "/"+ compressC2s + "}";
	}
}
