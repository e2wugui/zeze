package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;

public final class Subscribe extends Rpc<BSubscribeArgument, BSubscribeResult> {
	public static final int ProtocolId_ = Bean.hash32(Subscribe.class.getName()); // 1138220698
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 1138220698

	static {
		register(TypeId_, Subscribe.class);
	}

	public static final int Success = 0;
	public static final int DuplicateSubscribe = 1;

	public Subscribe() {
		Argument = new BSubscribeArgument();
		Result = new BSubscribeResult();
	}

	public Subscribe(BSubscribeArgument arg) {
		Argument = arg;
		Result = new BSubscribeResult();
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
