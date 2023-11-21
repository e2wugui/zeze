package Zeze.Dbh2;

import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

public class LogAllocateTid extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogAllocateTid.class.getName());

	private long range;

	public LogAllocateTid() {
		super(null);
	}

	public LogAllocateTid(long range) {
		super(null);
		this.range = range;
	}

	@Override
	public long typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.allocateTid(range);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		bb.WriteLong(range);
	}

	@Override
	public void decode(IByteBuffer bb) {
		super.decode(bb);
		range = bb.ReadLong();
	}
}
