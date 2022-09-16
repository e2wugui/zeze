package Zeze.Services.GlobalCacheManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;

public class BAchillesHeelConfig extends Bean {
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
	public void decode(ByteBuffer bb) {
		maxNetPing = bb.ReadInt();
		serverProcessTime = bb.ReadInt();
		serverReleaseTimeout = bb.ReadInt();
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
