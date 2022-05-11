package Zeze.Services.GlobalCacheManager;

import Zeze.Net.Binary;

public class Reduce extends Zeze.Net.Rpc<Param2, Param2> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.Hash32(Reduce.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public Reduce() {
		Argument = new Param2();
		Result = new Param2();
	}

	public Reduce(Binary gkey, int state, long globalSerialId) {
		Argument = new Param2();
		Result = new Param2();
		Argument.GlobalKey = gkey;
		Argument.State = state;
		Argument.GlobalSerialId = globalSerialId;
	}
}
