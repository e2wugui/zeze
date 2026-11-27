package Zeze.Raft;

import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

/**
 * Raft 节点向 Agent 通告当前 Leader 的 Rpc：
 * 连接建立且自身为 Leader、非 Leader 收到请求需重定向、或选举结束广播时发送。
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
