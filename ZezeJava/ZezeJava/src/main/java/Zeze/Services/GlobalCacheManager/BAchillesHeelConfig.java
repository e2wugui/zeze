package Zeze.Services.GlobalCacheManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;

public class BAchillesHeelConfig implements Serializable {
	public int maxNetPing;
	public int serverProcessTime;
	public int serverReleaseTimeout;

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(maxNetPing);
		bb.WriteInt(serverProcessTime);
		bb.WriteInt(serverReleaseTimeout);
	}

	@Override
	public void decode(IByteBuffer bb) {
		maxNetPing = bb.ReadInt();
		serverProcessTime = bb.ReadInt();
		serverReleaseTimeout = bb.ReadInt();
	}

	@Override
	public int preAllocSize() {
		return 5 + 5 + 5;
	}

	@Override
	public String toString() {
		return "BAchillesHeelConfig{" + "maxNetPing=" + maxNetPing + ",serverProcessTime=" + serverProcessTime +
				",serverReleaseTimeout=" + serverReleaseTimeout + '}';
	}
}
