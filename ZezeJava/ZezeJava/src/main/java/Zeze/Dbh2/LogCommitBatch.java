package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.CommitBatch;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

public class LogCommitBatch extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogCommitBatch.class.getName());

	private long tid;

	public LogCommitBatch() {
		this(null);
	}

	public LogCommitBatch(CommitBatch req) {
		super(req);
		if (null != req)
			tid = req.Argument.getTid();
	}

	@Override
	public long typeId() {
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
		bb.WriteLong(tid);
	}

	@Override
	public void decode(IByteBuffer bb) {
		super.decode(bb);
		tid = bb.ReadLong();
	}
}
