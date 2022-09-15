package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public class OfflineRegister extends Rpc<BOfflineNotify, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(OfflineRegister.class.getName()); // -1713644993
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 2581322303

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public OfflineRegister() {
		Argument = new BOfflineNotify();
		Result = EmptyBean.instance;
	}

	public OfflineRegister(BOfflineNotify argument) {
		Argument = argument;
		Result = EmptyBean.instance;
	}
}
