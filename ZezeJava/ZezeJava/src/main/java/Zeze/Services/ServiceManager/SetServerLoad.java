package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

public final class SetServerLoad extends Protocol<BServerLoad> {
	public static final int ProtocolId_ = Bean.Hash32(SetServerLoad.class.getName()); // -790028280
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 3504939016

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public SetServerLoad() {
		this.Argument = new BServerLoad();
	}
}
