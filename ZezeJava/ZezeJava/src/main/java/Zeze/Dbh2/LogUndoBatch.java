package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.UndoBatch;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import org.jetbrains.annotations.NotNull;

public class LogUndoBatch extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogUndoBatch.class.getName());

	private long tid;

	public LogUndoBatch() {
		this(0L);
	}

	public LogUndoBatch(UndoBatch req) {
		super(req);
		if (null != req)
			this.tid = req.Argument.getTid();
	}

	public LogUndoBatch(long tid) {
		super(null);
		this.tid = tid;
	}

	@Override
	public long typeId() {
		return TypeId_;
	}

	@Override
	public void apply(RaftLog holder, StateMachine stateMachine) throws Exception {
		var sm = (Dbh2StateMachine)stateMachine;
		sm.undoBatch(tid);
	}

	@Override
	public void encode(@NotNull ByteBuffer bb) {
		super.encode(bb);
		bb.WriteLong(tid);
	}

	@Override
	public void decode(@NotNull IByteBuffer bb) {
		super.decode(bb);
		tid = bb.ReadLong();
	}
}
