package Zeze.Services.Handshake;

import java.util.ArrayList;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

public class BSHandshake0Argument implements Serializable {
	public int encryptType; // 推荐的加密算法。旧版是boolean
	public ArrayList<Integer> supportedEncryptList = new ArrayList<>();
	public int compressS2c; // 推荐的压缩算法。
	public int compressC2s; // 推荐的压缩算法。
	public ArrayList<Integer> supportedCompressList = new ArrayList<>();

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(encryptType);
		bb.WriteInt(supportedEncryptList.size());
		for (var e : supportedEncryptList)
			bb.WriteInt(e);
		bb.WriteInt(compressS2c);
		bb.WriteInt(compressC2s);
		bb.WriteInt(supportedCompressList.size());
		for (var e : supportedCompressList)
			bb.WriteInt(e);
	}

	@Override
	public void decode(IByteBuffer bb) {
		encryptType = bb.ReadInt();
		supportedEncryptList.clear();
		supportedCompressList.clear();

		// 兼容旧版
		if (!bb.isEmpty()) {
			for (int count = bb.ReadInt(); count > 0; --count)
				supportedEncryptList.add(bb.ReadInt());
			compressS2c = bb.ReadInt();
			compressC2s = bb.ReadInt();
			for (int count = bb.ReadInt(); count > 0; --count)
				supportedCompressList.add(bb.ReadInt());
		} else {
			compressS2c = encryptType != Constant.eEncryptTypeDisable ? Constant.eCompressTypeMppc : Constant.eCompressTypeDisable;
			compressC2s = compressS2c;
		}
	}

	private static int _PRE_ALLOC_SIZE_ = 32;

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
		return "BSHandshake0Argument{encryptType=" + encryptType
				+ ",supportedEncryptList=" + supportedEncryptList
				+ ",compressS2c/C2s" + compressS2c + '/' + compressC2s
				+ ",supportedCompressList=" + supportedCompressList + '}';
	}
}
