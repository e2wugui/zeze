package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BBeginTransactionArgument;
import Zeze.Builtin.Dbh2.BeginTransaction;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogBeginTransaction extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogBeginTransaction.class.getName());

	private BBeginTransactionArgument.Data argument;

	public LogBeginTransaction() {
		this(null);
	}

	public LogBeginTransaction(BeginTransaction req) {
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
		sm.beginTransaction(argument);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		argument.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		argument = new BBeginTransactionArgument.Data();
		argument.decode(bb);
	}
}
