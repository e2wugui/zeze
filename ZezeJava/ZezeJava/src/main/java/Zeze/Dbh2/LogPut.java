package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.Put;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogPut extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogPut.class.getName());

	public LogPut() {
		this(null);
	}

	public LogPut(Put req) {
		super(req);
	}

	@Override
	public int typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {

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
