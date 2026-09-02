package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

/**
 * Identify/Suspect 参数：serverId。
 */
public class BIdentify implements Serializable {
	public int serverId;

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(serverId);
	}

	@Override
	public void decode(IByteBuffer bb) {
		serverId = bb.ReadInt();
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
		return "BIdentify{serverId=" + serverId + '}';
	}
}
