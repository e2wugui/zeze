package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.UndoBatch;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;

public class LogUndoBatch extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogUndoBatch.class.getName());

	private long tid;

	public LogUndoBatch() {
		this(null);
	}

	public LogUndoBatch(UndoBatch req) {
		super(null);
		if (null != req)
			this.tid = req.Argument.getTid();
	}

	@Override
	public int typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.undoBatch(tid);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		bb.WriteLong(tid);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		tid = bb.ReadLong();
	}
}
