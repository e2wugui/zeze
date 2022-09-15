package Zeze.Services.GlobalCacheManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public class BAchillesHeelConfig extends Bean {
	public int MaxNetPing;
	public int ServerProcessTime;
	public int ServerReleaseTimeout;

	@Override
	public void encode(ByteBuffer bb) {
		bb.WriteInt(MaxNetPing);
		bb.WriteInt(ServerProcessTime);
		bb.WriteInt(ServerReleaseTimeout);
	}

	@Override
	public void decode(ByteBuffer bb) {
		MaxNetPing = bb.ReadInt();
		ServerProcessTime = bb.ReadInt();
		ServerReleaseTimeout = bb.ReadInt();
	}

	@Override
	protected void initChildrenRootInfo(Record.RootInfo root) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetChildrenRootInfo() {
		throw new UnsupportedOperationException();
	}
}
