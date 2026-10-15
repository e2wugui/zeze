package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

/**
 * Identify/Suspect 参数：serverId + version（进程数据格式版本，接管梯队依据）。
 * Identify 上报时由 GS 填自己的 config.getAppMainVersion()；Suspect 广播时填死者的 version。
 */
public class BIdentify implements Serializable {
	public int serverId;
	public long version;

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(serverId);
		bb.WriteLong(version);
	}

	@Override
	public void decode(IByteBuffer bb) {
		serverId = bb.ReadInt();
		version = bb.ReadLong();
	}

	private static int _PRE_ALLOC_SIZE_ = 24;

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
		return "BIdentify{serverId=" + serverId + ", version=" + version + '}';
	}
}
