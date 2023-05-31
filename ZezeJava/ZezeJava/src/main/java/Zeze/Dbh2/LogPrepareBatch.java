package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BPrepareBatch;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogPrepareBatch extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogPrepareBatch.class.getName());

	private BPrepareBatch.Data argument;

	public LogPrepareBatch() {
		this(null);
	}

	public LogPrepareBatch(PrepareBatch req) {
		super(null);
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
		sm.prepareBatch(argument);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		argument.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		argument = new BPrepareBatch.Data();
		argument.decode(bb);
	}
}
