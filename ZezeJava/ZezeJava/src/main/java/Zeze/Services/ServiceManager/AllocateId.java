package Zeze.Services.ServiceManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;

/** AllocateId协议：客户端向SM申请全局唯一long号段（name+count），返回startId+count。 */
public final class AllocateId extends Rpc<BAllocateIdArgument, BAllocateIdResult> {
	public static final int ProtocolId_ = Bean.hash32(AllocateId.class.getName()); // -282549003
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 4012418293

	static {
		register(TypeId_, AllocateId.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public AllocateId() {
		Argument = new BAllocateIdArgument();
		Result = new BAllocateIdResult();
	}
}
