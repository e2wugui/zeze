package Zeze.Services.ServiceManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

/**
 * TakeoverDone 协议参数：GS 完成全部 scope 接管后向 SM 上报的回执（fire-and-forget），
 * 供 SM 停止 version 梯队升档。
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
