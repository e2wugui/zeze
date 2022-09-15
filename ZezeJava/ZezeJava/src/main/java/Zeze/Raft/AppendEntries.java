package Zeze.Raft;

import Zeze.Net.Rpc;
import Zeze.Transaction.Bean;

final class AppendEntries extends Rpc<BAppendEntriesArgument, BAppendEntriesResult> {
	public static final int ProtocolId_ = Bean.hash32(AppendEntries.class.getName());
	public static final long TypeId_ = ProtocolId_ & 0xffff_ffffL;

	public AppendEntries() {
		Argument = new BAppendEntriesArgument();
		Result = new BAppendEntriesResult();
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
