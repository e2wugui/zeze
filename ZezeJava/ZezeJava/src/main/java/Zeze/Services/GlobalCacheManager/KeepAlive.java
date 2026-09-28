package Zeze.Services.GlobalCacheManager;

import Zeze.Net.Rpc;
import Zeze.Transaction.EmptyBean;

/** KeepAlive协议：GCM会话保活心跳，空参数空结果。 */
public final class KeepAlive extends Rpc<EmptyBean, EmptyBean> {
	public static final int ProtocolId_ = Zeze.Transaction.Bean.hash32(KeepAlive.class.getName()); // 560224048
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL; // 560224048

	static {
		register(TypeId_, KeepAlive.class);
	}

	@Override
	public int getModuleId() {
		return 0;
	}

	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}

	public KeepAlive() {
		Argument = EmptyBean.instance;
		Result = EmptyBean.instance;
	}
}
