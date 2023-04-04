package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.RollbackTransaction;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Builtin.Dbh2.BRollbackTransactionArgument;

public class LogRollbackTransaction extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogRollbackTransaction.class.getName());

	private BRollbackTransactionArgument.Data argument;

	public LogRollbackTransaction() {
		this(null);
	}

	public LogRollbackTransaction(RollbackTransaction req) {
		super(null);
		if (null != req)
			argument = req.Argument;
	}

	@Override
	public int typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.rollbackTransaction(argument);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		argument.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		argument = new BRollbackTransactionArgument.Data();
		argument.decode(bb);
	}
}
