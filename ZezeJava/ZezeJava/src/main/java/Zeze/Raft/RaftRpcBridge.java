package Zeze.Raft;

import Zeze.Transaction.Bean;

final class RaftRpcBridge<TArgument extends Bean, TResult extends Bean> extends RaftRpc<TArgument, TResult> {
	private final RaftRpc<TArgument, TResult> Real;

	public RaftRpcBridge(RaftRpc<TArgument, TResult> real) {
		Real = real;
	}

	@Override
	public int getModuleId() {
		return Real.getModuleId();
	}

	@Override
	public int getProtocolId() {
		return Real.getProtocolId();
	}

	@Override
	public String toString() {
		return "RaftRpcBridge(" + Real.toString() + ')';
	}
}
