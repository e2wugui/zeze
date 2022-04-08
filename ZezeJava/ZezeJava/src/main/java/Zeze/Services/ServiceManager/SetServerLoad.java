package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol;
import Zeze.Transaction.Bean;

public final class SetServerLoad extends Protocol<ServerLoad> {
	public static final int ProtocolId_ = Bean.Hash32(SetServerLoad.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public SetServerLoad() {
		this.Argument = new ServerLoad();
	}
}
