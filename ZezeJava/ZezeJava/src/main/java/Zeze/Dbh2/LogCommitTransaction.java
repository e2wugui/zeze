package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.CommitTransaction;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogCommitTransaction extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogCommitTransaction.class.getName());

	public LogCommitTransaction() {
		this(null);
	}

	public LogCommitTransaction(CommitTransaction req) {
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
