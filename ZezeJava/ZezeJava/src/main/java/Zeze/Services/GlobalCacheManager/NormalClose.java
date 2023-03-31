package Zeze.Services.GlobalCacheManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.EmptyBean;

public class NormalClose extends Rpc<EmptyBean, EmptyBean> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.hash32(NormalClose.class.getName()); // -532299976
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 3762667320

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
