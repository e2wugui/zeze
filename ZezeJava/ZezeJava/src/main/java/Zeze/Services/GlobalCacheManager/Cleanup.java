package Zeze.Services.GlobalCacheManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.EmptyBean;

public class Cleanup extends Rpc<BAchillesHeel, EmptyBean> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.hash32(Cleanup.class.getName()); // -1903349850
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 2391617446

	static {
		register(TypeId_, Cleanup.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Cleanup() {
		Argument = new BAchillesHeel();
		Result = EmptyBean.instance;
	}
}
