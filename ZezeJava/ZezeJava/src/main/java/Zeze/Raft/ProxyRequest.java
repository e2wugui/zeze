package Zeze.Raft;

import Zeze.Transaction.Bean;
import Zeze.Net.Rpc;

/**
 * 代理协议。
 */
public class ProxyRequest extends Rpc<ProxyArgument, ProxyResult> {
	public static final int ProtocolId_ = Bean.hash32(ProxyRequest.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	public ProxyRequest() {
		Argument = new ProxyArgument();
		Result = new ProxyResult();
	}

	public ProxyRequest(ProxyArgument proxyArgument) {
		Argument = proxyArgument;
		Result = new ProxyResult();
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
