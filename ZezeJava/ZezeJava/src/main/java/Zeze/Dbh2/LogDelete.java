package Zeze.Dbh2;

import Zeze.Builtin.Dbh2.Delete;
import Zeze.Raft.Log;
import Zeze.Raft.RaftLog;
import Zeze.Raft.StateMachine;
import Zeze.Serialize.ByteBuffer;
import Zeze.Builtin.Dbh2.BDeleteArgumentDaTa;

public class LogDelete extends Log {
	public static final int TypeId_ = Zeze.Transaction.Bean.hash32(LogDelete.class.getName());

	private BDeleteArgumentDaTa argument;

	public LogDelete() {
		this(null);
	}

	public LogDelete(Delete req) {
		super(req);
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
		sm.delete(argument);
	}

	@Override
	public void encode(ByteBuffer bb) {
		super.encode(bb);
		argument.encode(bb);
	}

	@Override
	public void decode(ByteBuffer bb) {
		super.decode(bb);
		argument = new BDeleteArgumentDaTa();
		argument.decode(bb);
	}
}
