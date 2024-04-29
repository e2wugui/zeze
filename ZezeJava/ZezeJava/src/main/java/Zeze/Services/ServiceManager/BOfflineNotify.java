package Zeze.Services.ServiceManager;

import Zeze.Net.Binary;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

public class BOfflineNotify implements Serializable {
	public int serverId;
	public String notifyId;
	public long notifySerialId; // context 如果够用就直接用这个，
	public Binary notifyContext = Binary.Empty; // context 扩展context。

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(serverId);
		bb.WriteString(notifyId);
		bb.WriteLong(notifySerialId);
		bb.WriteBinary(notifyContext);
	}

	@Override
	public void decode(IByteBuffer bb) {
		serverId = bb.ReadInt();
		notifyId = bb.ReadString();
		notifySerialId = bb.ReadLong();
		notifyContext = bb.ReadBinary();
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
		return "BOfflineNotify{serverId=" + serverId + ",notifyId='" + notifyId +
				"',notifySerialId=" + notifySerialId + ",notifyContext=" + notifyContext + '}';
	}
}
