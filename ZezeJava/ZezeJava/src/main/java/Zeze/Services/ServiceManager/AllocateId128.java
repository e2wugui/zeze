package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;

public final class AllocateId128 extends Rpc<BAllocateId128Argument, BAllocateId128Result> {
	public static final int ProtocolId_ = Bean.hash32(AllocateId128.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	static {
		register(TypeId_, AllocateId128.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public AllocateId128() {
		Argument = new BAllocateId128Argument();
		Result = new BAllocateId128Result();
	}
}
