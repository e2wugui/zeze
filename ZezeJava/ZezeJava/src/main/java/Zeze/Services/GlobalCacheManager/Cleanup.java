package Zeze.Services.GlobalCacheManager;

import Zeze.Transaction.EmptyBean;

public class Cleanup extends Zeze.Net.Rpc<BAchillesHeel, Zeze.Transaction.EmptyBean> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.Hash32(Cleanup.class.getName()); // -1903349850
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 2391617446

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
