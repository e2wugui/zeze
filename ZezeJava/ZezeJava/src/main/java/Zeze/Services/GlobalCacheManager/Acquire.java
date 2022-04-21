package Zeze.Services.GlobalCacheManager;

import Zeze.Builtin.GlobalCacheManagerWithRaft.GlobalTableKey;

public class Acquire extends Zeze.Net.Rpc<Param, Param2> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.Hash32(Acquire.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Acquire() {
		Argument = new Param();
		Result = new Param2();
	}

	public Acquire(GlobalTableKey gkey, int state) {
		Argument = new Param();
		Result = new Param2();
		Argument.GlobalTableKey = gkey;
		Argument.State = state;
	}
}
