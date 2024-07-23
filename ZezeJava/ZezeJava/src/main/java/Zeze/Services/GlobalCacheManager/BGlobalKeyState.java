package Zeze.Services.GlobalCacheManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Util.Id128;
import org.jetbrains.annotations.Nullable;

public class BGlobalKeyState implements Serializable {
	public Binary globalKey; // 没有初始化，使用时注意
	public int state;
	public @Nullable Id128 reducedTid; // 被降级方的事务Id

	@Override
	public void decode(IByteBuffer bb) {
		globalKey = bb.ReadBinary();
		state = bb.ReadUInt();
		reducedTid = new Id128();
		reducedTid.decodeRaw(bb);
	}

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteBinary(globalKey);
		bb.WriteUInt(state);
		var tid = reducedTid;
		(tid != null ? tid : Id128.Zero).encodeRaw(bb);
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
		return globalKey + ":" + state;
	}
}
