package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;

public final class AllocateId extends Rpc<AllocateIdArgument, AllocateIdResult> {
	public static final int ProtocolId_ = Bean.Hash32(AllocateId.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public AllocateId() {
		Argument = new AllocateIdArgument();
		Result = new AllocateIdResult();
	}
}
