package Zeze.Raft;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;

/**
 * Raft 预投票（PreVote）Rpc：候选者在递增 term 前先向多数派探测，避免分区后 term 膨胀的节点回归时打断健康 Leader。
 * 接收者处理预投票时不修改 term/voteFor，也不重置选举计时。
 */
final class PreVote extends Rpc<BRequestVoteArgument, BRequestVoteResult> {
	public static final int ProtocolId_ = Bean.hash32(PreVote.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	static {
		register(TypeId_, PreVote.class);
	}

	public PreVote() {
		Argument = new BRequestVoteArgument();
		Result = new BRequestVoteResult();
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
