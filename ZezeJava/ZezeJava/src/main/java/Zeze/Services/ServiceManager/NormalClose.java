package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

public class NormalClose extends Rpc<EmptyBean, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(NormalClose.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	static {
		register(TypeId_, NormalClose.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public NormalClose() {
		Argument = EmptyBean.instance;
		Result = EmptyBean.instance;
	}
}
