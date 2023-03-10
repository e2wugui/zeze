package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BCommitTransactionArgument;
import Zeze.Builtin.Dbh2.CommitTransaction;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogCommitTransaction extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogCommitTransaction.class.getName());

	private BCommitTransactionArgument.Data argument;

	public LogCommitTransaction() {
		this(null);
	}

	public LogCommitTransaction(CommitTransaction req) {
		super(req);
		if (null != req)
			this.argument = req.Argument;
	}

	@Override
	public int typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.commitTransaction(argument);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		argument.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		argument = new BCommitTransactionArgument.Data();
		argument.decode(bb);
	}
}
