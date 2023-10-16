package Zeze.Raft;

import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

/**
 * LeaderIs 的发送时机
 * 0. Agent 刚连上来时，如果Node是当前Leader，它马上这个Rpc给Agent。
 * 1. Node 收到应用请求时，发现自己不是Leader，发送重定向。此时Node不处理请求（也不返回结果）。
 * 2. 选举结束时也给Agent广播选举结果。
 */
public final class LeaderIs extends ProxyableRpc<BLeaderIsArgument, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(LeaderIs.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	static {
		register(TypeId_, LeaderIs.class);
	}

	public LeaderIs() {
		Argument = new BLeaderIsArgument();
		Result = EmptyBean.instance;
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
