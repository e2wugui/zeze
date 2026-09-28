package Zeze.Raft;

import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;

/**
 * 这条Rpc由Agent使用，请求Raft节点停止其全部Connector（驱赶Leader时逐个停非建议节点）。
 */
public class StopServerConnector extends RaftRpc<EmptyBean, EmptyBean> {
	public static final int ProtocolId_ = Bean.hash32(StopServerConnector.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	static {
		register(TypeId_, StopServerConnector.class);
	}

	public StopServerConnector() {
		Argument = EmptyBean.instance;
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
