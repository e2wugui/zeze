package Zeze.Dbh2;

import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogClearEndSplitCleanKey extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogClearEndSplitCleanKey.class.getName());

	public LogClearEndSplitCleanKey() {
		super(null);
	}

	@Override
	public int typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.clearEndSplitCleanKey();
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
	}
}
