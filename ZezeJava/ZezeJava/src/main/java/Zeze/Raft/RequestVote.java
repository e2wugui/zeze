package Zeze.Raft;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;

final class RequestVote extends Rpc<RequestVoteArgument, RequestVoteResult> {
	public static final int ProtocolId_ = Bean.Hash32(RequestVote.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	public RequestVote() {
		Argument = new RequestVoteArgument();
		Result = new RequestVoteResult();
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
