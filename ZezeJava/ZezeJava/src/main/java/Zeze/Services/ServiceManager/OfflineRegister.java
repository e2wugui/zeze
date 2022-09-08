package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.EmptyBean;

public class OfflineRegister extends Rpc<BOfflineRegister, EmptyBean> {
	public static final int ProtocolId_ = Bean.Hash32(Register.class.getName()); // -1080792024
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 3214175272

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public OfflineRegister() {
		Argument = new BOfflineRegister();
		Result = new EmptyBean();
	}
}
