package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

public final class SubscribeFirstCommit extends Protocol<BServiceInfos> {
	public static final int ProtocolId_ = Bean.hash32(SubscribeFirstCommit.class.getName()); // -441242282
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 3853725014

	public SubscribeFirstCommit() {
		Argument = new BServiceInfos();
	}

	public SubscribeFirstCommit(BServiceInfos infos) {
		Argument = infos;
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
