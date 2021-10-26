package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;

public final class AllocateId extends Rpc<AllocateIdArgument, AllocateIdResult> {
	public final static int ProtocolId_ = Bean.Hash16(AllocateId.class.getName());

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
