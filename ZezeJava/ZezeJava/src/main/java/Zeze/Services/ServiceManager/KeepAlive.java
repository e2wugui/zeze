package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public final class KeepAlive extends Rpc<EmptyBean, EmptyBean> {
	public final static int ProtocolId_ = Bean.Hash32(KeepAlive.class.getName());

	public static final int Success = 0;

	public KeepAlive() {
		Argument = new EmptyBean();
		Result = new EmptyBean();
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
