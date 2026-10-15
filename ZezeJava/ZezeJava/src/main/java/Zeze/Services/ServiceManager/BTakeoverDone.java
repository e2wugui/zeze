package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

/**
 * TakeoverDone 参数：接管完成回执（GS→SM，fire-and-forget）。
 * GS 的 tryTransfer 全部 scope 搬运完成（立墓碑）后上报，SM 据此停止 version 梯队升档。
 * 回执丢失由梯队超时升档的重复 Suspect 兜底：已接管者 tryTransfer 幂等命中墓碑空转。
 */
public class BTakeoverDone implements Serializable {
	public int deadServerId;
	public int takerServerId;
	public long takerVersion;

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(deadServerId);
		bb.WriteInt(takerServerId);
		bb.WriteLong(takerVersion);
	}

	@Override
	public void decode(IByteBuffer bb) {
		deadServerId = bb.ReadInt();
		takerServerId = bb.ReadInt();
		takerVersion = bb.ReadLong();
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
		return "BTakeoverDone{deadServerId=" + deadServerId + ", takerServerId=" + takerServerId
				+ ", takerVersion=" + takerVersion + '}';
	}
}
