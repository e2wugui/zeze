package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BBeginTransactionArgumentData;
import Zeze.Builtin.Dbh2.BLogBeginTransactionData;
import Zeze.Builtin.Dbh2.BeginTransaction;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogBeginTransaction extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogBeginTransaction.class.getName());

	private BLogBeginTransactionData argument;

	public LogBeginTransaction() {
		this(null, null);
	}

	public LogBeginTransaction(BeginTransaction req, BLogBeginTransactionData argument) {
		super(req);
		this.argument = argument;
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
		argument = new BLogBeginTransactionData();
		argument.decode(bb);
	}
}
