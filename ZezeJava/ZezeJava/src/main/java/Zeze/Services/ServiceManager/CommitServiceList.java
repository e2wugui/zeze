package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

public final class CommitServiceList extends Protocol<BServiceListVersion> {
	public static final int ProtocolId_ = Bean.hash32(CommitServiceList.class.getName()); // -1436469291
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 2858498005

	public CommitServiceList() {
		Argument = new BServiceListVersion();
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}
}
