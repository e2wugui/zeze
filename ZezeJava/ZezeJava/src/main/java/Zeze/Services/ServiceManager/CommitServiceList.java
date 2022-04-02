package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

public final class CommitServiceList extends Protocol<ServiceInfos> {
	public static final int ProtocolId_ = Bean.Hash32(CommitServiceList.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	public CommitServiceList() {
		Argument = new ServiceInfos();
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
