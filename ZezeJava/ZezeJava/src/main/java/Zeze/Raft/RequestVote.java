package Zeze.Raft;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;

final class RequestVote extends Rpc<BRequestVoteArgument, BRequestVoteResult> {
	public static final int ProtocolId_ = Bean.Hash32(RequestVote.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	public RequestVote() {
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
