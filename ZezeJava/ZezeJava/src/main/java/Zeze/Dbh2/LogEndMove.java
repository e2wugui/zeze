package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;

public class LogEndMove extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogEndMove.class.getName());

	private BBucketMeta.Data to;

	public LogEndMove() {
		super(null);
	}

	public LogEndMove(BBucketMeta.Data to) {
		super(null);
		this.to = to;
	}

	public BBucketMeta.Data getTo() {
		return to;
	}

	@Override
	public long typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.endMove(to);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		to.encode(bb);
	}

	@Override
	public void decode(IByteBuffer bb) {
		super.decode(bb);
		to = new BBucketMeta.Data();
		to.decode(bb);
	}
}
