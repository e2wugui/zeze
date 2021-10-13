package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol1;
import Zeze.Transaction.Bean;

public final class CommitServiceList extends Protocol1<ServiceInfos> {
	public final static int ProtocolId_ = Bean.Hash16(CommitServiceList.class.getName());

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
