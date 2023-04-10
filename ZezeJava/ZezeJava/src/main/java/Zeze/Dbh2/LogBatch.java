package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogBatch extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogBatch.class.getName());

	private BBatch.Data argument;

	public LogBatch() {
		this(null);
	}

	public LogBatch(PrepareBatch req) {
		super(null);
		if (null != req)
			this.argument = req.Argument.getBatch();
	}

	@Override
	public int typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.writeBatch(argument);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		argument.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		argument = new BBatch.Data();
		argument.decode(bb);
	}
}
