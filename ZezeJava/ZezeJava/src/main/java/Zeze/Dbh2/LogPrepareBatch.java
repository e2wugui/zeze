package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BBatch;
import Zeze.Builtin.Dbh2.PrepareBatch;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogPrepareBatch extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogPrepareBatch.class.getName());

	private long tid;
	private BBatch.Data argument;

	public LogPrepareBatch() {
		this(0, null);
	}

	public LogPrepareBatch(long tid, PrepareBatch req) {
		super(null);
		this.tid = tid;
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
		sm.prepareBatch(tid, argument);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		bb.WriteLong(tid);
		argument.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		tid = bb.ReadLong();
		argument = new BBatch.Data();
		argument.decode(bb);
	}
}
