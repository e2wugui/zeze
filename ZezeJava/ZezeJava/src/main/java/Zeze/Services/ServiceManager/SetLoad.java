package Zeze.Services.ServiceManager;

import Zeze.Net.Protocol;
import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class SetLoad extends Protocol<Load> {
	public static final int ProtocolId_ = Bean.Hash32(SetLoad.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public SetLoad() {
		this.Argument = new Load();
	}
}
