package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol1;
import Zeze.Transaction.Bean;

public final class ReadyServiceList extends Protocol1<ServiceInfos> {
	public final static int ProtocolId_ = Bean.Hash16(ReadyServiceList.class.getName());

	public ReadyServiceList() {
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
