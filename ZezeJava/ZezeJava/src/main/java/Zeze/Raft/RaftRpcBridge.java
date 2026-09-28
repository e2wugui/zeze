package Zeze.Raft;

import Zeze.Serialize.Serializable;
import org.jetbrains.annotations.NotNull;

/**
 * RaftRpc 发送桥接：为每次发送提供新的 sessionId，实际承载原始 rpc 的参数、唯一标识与回调。
 */
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
	public @NotNull String toString() {
		return "RaftRpcBridge(" + real.toString() + ')';
	}
}
