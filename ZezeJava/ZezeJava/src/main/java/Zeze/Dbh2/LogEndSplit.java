package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.BBucketMeta;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogEndSplit extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogEndSplit.class.getName());

	private BBucketMeta.Data from;
	private BBucketMeta.Data to;

	public LogEndSplit() {
		super(null);
	}

	public LogEndSplit(BBucketMeta.Data from, BBucketMeta.Data to) {
		super(null);
		this.from = from;
		this.to = to;
	}

	public BBucketMeta.Data getFrom() {
		return from;
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
		sm.endSplit(from, to);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		super.encode(bb);
		from.encode(bb);
		to.encode(bb);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		super.decode(bb);
		from = new BBucketMeta.Data();
		from.decode(bb);
		to = new BBucketMeta.Data();
		to.decode(bb);
	}
}
