package Zeze.Raft;

import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;

final class RaftRpcBridge<TArgument extends Serializable, TResult extends Serializable> extends RaftRpc<TArgument, TResult> {
	private final RaftRpc<TArgument, TResult> real;

	public RaftRpcBridge(RaftRpc<TArgument, TResult> real) {
		this.real = real;
	}

	@Override
	public int getModuleId() {
		return real.getModuleId();
	}

	@Override
	public int getProtocolId() {
		return real.getProtocolId();
	}

	@Override
	public String toString() {
		return "RaftRpcBridge(" + real.toString() + ')';
	}
}
