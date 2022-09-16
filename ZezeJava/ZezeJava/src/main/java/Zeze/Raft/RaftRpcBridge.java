package Zeze.Raft;

import Zeze.Transaction.Bean;

final class RaftRpcBridge<TArgument extends Bean, TResult extends Bean> extends RaftRpc<TArgument, TResult> {
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
