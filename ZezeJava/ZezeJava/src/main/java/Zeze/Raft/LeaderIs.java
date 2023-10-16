package Zeze.Raft;

import Zeze.Net.Binary;
import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import org.jetbrains.annotations.Nullable;

/**
 * LeaderIs 的发送时机
 * 0. Agent 刚连上来时，如果Node是当前Leader，它马上这个Rpc给Agent。
 * 1. Node 收到应用请求时，发现自己不是Leader，发送重定向。此时Node不处理请求（也不返回结果）。
 * 2. 选举结束时也给Agent广播选举结果。
 */
public final class LeaderIs extends Rpc<BLeaderIsArgument, EmptyBean> {
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

	private ProxyRequest proxyRequest;

	public void setProxyRequest(ProxyRequest proxyRequest) {
		this.proxyRequest = proxyRequest;
	}

	@Override
	public void SendResult(@Nullable Binary result) {
		if (proxyRequest == null) {
			// 原始raft连接方式。
			super.SendResult(result);
			return;
		}

		// proxy 方式，基本逻辑拷贝自 Rpc.SendResult(Binary result)。
		if (sendResultDone) {
			logger.error("Rpc.SendResult Already Done: {} {}", getSender(), this, new Exception());
			return;
		}
		sendResultDone = true;
		resultEncoded = result;
		setRequest(false);

		// 填写proxyRequest.Result并发送。
		proxyRequest.Result.setData(new Binary(this.encode()));
		proxyRequest.SendResult();
	}
}
