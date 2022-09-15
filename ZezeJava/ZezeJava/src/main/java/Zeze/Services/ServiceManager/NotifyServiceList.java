package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

public final class NotifyServiceList extends Protocol<BServiceInfos> {
	public static final int ProtocolId_ = Bean.hash32(NotifyServiceList.class.getName()); // -1758680910
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 2536286386

	public NotifyServiceList() {
		Argument = new BServiceInfos();
	}

	public NotifyServiceList(BServiceInfos arg) {
		Argument = arg;
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
