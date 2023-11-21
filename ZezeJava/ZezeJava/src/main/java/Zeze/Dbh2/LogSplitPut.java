package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BSplitPut;
import Zeze.Builtin.Dbh2.SplitPut;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

public class LogSplitPut extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogSplitPut.class.getName());

	private BSplitPut.Data puts;

	public LogSplitPut() {
		this(null);
	}

	public LogSplitPut(SplitPut req) {
		super(null);
		if (null != req)
			puts = req.Argument;
	}

	@Override
	public long typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.applySplitPut(puts);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		puts.encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		super.decode(bb);
		puts = new BSplitPut.Data();
		puts.decode(bb);
	}
}
