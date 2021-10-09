package Zeze.Raft;

import Zeze.Net.*;
import Zeze.Serialize.*;
import Zeze.Transaction.*;
import Zeze.*;
import java.util.*;

public final class RequestVote extends Rpc<RequestVoteArgument, RequestVoteResult> {
	public final static int ProtocolId_ = Bean.Hash16(RequestVote.class.FullName);

	@Override
	public int getModuleId() {
		return 0;
	}
	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}
}