package Zeze.Raft;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;

/**
 * 预投票（PreVote，raft 博士论文 §4.2.3 / §9.6）。
 * 候选者在真正增加 term 发起选举前，先以"下一个term"询问多数派：
 * 只有预投票成功才真正发起 RequestVote。
 * 这样被网络分区、term 已经膨胀的节点重新加入时，
 * 不会因为它的 RequestVote 携带巨大 term 而把健康 Leader 拉下台
 * （其它节点近期还听得到 Leader，会拒绝预投票）。
 * 预投票不修改接收者的 term/voteFor，也不重置任何选举计时。
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
