package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.CommitBatch;
import Zeze.Net.Binary;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogCommitBatch extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogCommitBatch.class.getName());

	private Binary tid;

	public LogCommitBatch() {
		this(null);
	}

	public LogCommitBatch(CommitBatch req) {
		super(null);
		if (null != req)
			tid = req.Argument.getTid();
	}

	@Override
	public int typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.commitBatch(tid);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		bb.WriteBinary(tid);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		tid = bb.ReadBinary();
	}
}
